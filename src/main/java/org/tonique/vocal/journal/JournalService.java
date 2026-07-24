package org.tonique.vocal.journal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tonique.vocal.payment.Payment;
import org.tonique.vocal.payment.PaymentMatchStatus;
import org.tonique.vocal.payment.PaymentRepository;
import org.tonique.vocal.student.Student;
import org.tonique.vocal.student.StudentService;
import org.tonique.vocal.student.Tariff;

import java.time.ZoneId;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class JournalService {

    private static final ZoneId KYIV_ZONE = ZoneId.of("Europe/Kyiv");

    private final StudentService studentService;
    private final PaymentRepository paymentRepository;

    public JournalService(StudentService studentService, PaymentRepository paymentRepository) {
        this.studentService = studentService;
        this.paymentRepository = paymentRepository;
    }

    public JournalGrid buildGrid(int year) {
        List<Student> students = studentService.findActive();
        List<Payment> yearPayments = paymentRepository.findByMatchStatusAndPeriodYear(PaymentMatchStatus.MATCHED, year);

        Map<Long, Map<Integer, Long>> sumsByStudentAndMonth = new HashMap<>();
        for (Payment payment : yearPayments) {
            if (payment.getStudent() == null) {
                continue;
            }
            sumsByStudentAndMonth
                    .computeIfAbsent(payment.getStudent().getId(), id -> new HashMap<>())
                    .merge(payment.getPeriodMonth(), payment.getAmountKopiykas(), Long::sum);
        }

        List<JournalRow> rows = new ArrayList<>();
        for (Student student : students) {
            rows.add(buildRow(student, year, sumsByStudentAndMonth.getOrDefault(student.getId(), Map.of())));
        }
        return new JournalGrid(year, rows);
    }

    private JournalRow buildRow(Student student, int year, Map<Integer, Long> monthSums) {
        YearMonth joinedYearMonth = YearMonth.from(student.getCreatedAt().atZone(KYIV_ZONE));

        List<JournalCell> cells = new ArrayList<>(12);
        for (int month = 1; month <= 12; month++) {
            YearMonth cellYearMonth = YearMonth.of(year, month);
            if (cellYearMonth.isBefore(joinedYearMonth)) {
                cells.add(null);
                continue;
            }
            long sum = monthSums.getOrDefault(month, 0L);
            cells.add(new JournalCell(sum, statusFor(sum, student.getTariff())));
        }

        Tariff tariff = student.getTariff();
        return new JournalRow(
                student.getId(),
                student.getFullName(),
                tariff != null ? tariff.name() : null,
                tariff != null ? tariff.amountKopiykas() : null,
                cells
        );
    }

    private JournalCell.CellStatus statusFor(long sumKopiykas, Tariff tariff) {
        if (sumKopiykas <= 0) {
            return JournalCell.CellStatus.RED;
        }
        if (tariff == null) {
            // paid something, but we have no confirmed target amount to call it "full"
            return JournalCell.CellStatus.YELLOW;
        }
        return sumKopiykas >= tariff.amountKopiykas() ? JournalCell.CellStatus.GREEN : JournalCell.CellStatus.YELLOW;
    }

    public List<PaymentDetail> studentMonthDetail(Long studentId, int year, int month) {
        studentService.getOrThrow(studentId);
        return paymentRepository
                .findByStudentIdAndMatchStatusAndPeriodYearAndPeriodMonth(studentId, PaymentMatchStatus.MATCHED, year, month)
                .stream()
                .sorted(Comparator.comparing(Payment::getPaymentDate))
                .map(payment -> new PaymentDetail(
                        payment.getId(),
                        payment.getSource(),
                        payment.getAmountKopiykas(),
                        payment.getPaymentDate(),
                        payment.getRawComment()
                ))
                .toList();
    }
}
