package org.tonique.vocal.journal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tonique.vocal.enrollment.EnrollmentService;
import org.tonique.vocal.enrollment.TariffEnrollment;
import org.tonique.vocal.payment.Payment;
import org.tonique.vocal.payment.PaymentMatchStatus;
import org.tonique.vocal.payment.PaymentRepository;
import org.tonique.vocal.student.Student;
import org.tonique.vocal.student.StudentService;
import org.tonique.vocal.tariff.TariffPlan;
import org.tonique.vocal.tariff.TariffPricingService;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the journal grid with one row per (student, tariff enrollment) that
 * overlaps the requested year — a student on both choir and individual lessons
 * gets two independent rows, each colored against only that tariff's payments.
 */
@Service
@Transactional(readOnly = true)
public class JournalService {

    private final StudentService studentService;
    private final PaymentRepository paymentRepository;
    private final TariffPricingService tariffPricingService;
    private final EnrollmentService enrollmentService;

    public JournalService(
            StudentService studentService,
            PaymentRepository paymentRepository,
            TariffPricingService tariffPricingService,
            EnrollmentService enrollmentService
    ) {
        this.studentService = studentService;
        this.paymentRepository = paymentRepository;
        this.tariffPricingService = tariffPricingService;
        this.enrollmentService = enrollmentService;
    }

    public JournalGrid buildGrid(int year) {
        List<Student> students = studentService.findActive();
        List<Long> studentIds = students.stream().map(Student::getId).toList();

        List<TariffEnrollment> enrollments = enrollmentService.findForStudents(studentIds).stream()
                .filter(enrollment -> enrollment.overlaps(year))
                .sorted(Comparator.<TariffEnrollment, String>comparing(e -> e.getStudent().getFullName())
                        .thenComparing(e -> e.getTariffPlan().getLabel())
                        .thenComparing(TariffEnrollment::getValidFrom))
                .toList();

        Map<StudentTariffKey, Map<Integer, Long>> sumsByStudentTariffAndMonth = new HashMap<>();
        for (Payment payment : paymentRepository.findByMatchStatusAndPeriodYear(PaymentMatchStatus.MATCHED, year)) {
            if (payment.getStudent() == null || payment.getTariffPlan() == null) {
                continue;
            }
            StudentTariffKey key = new StudentTariffKey(payment.getStudent().getId(), payment.getTariffPlan().getId());
            sumsByStudentTariffAndMonth
                    .computeIfAbsent(key, k -> new HashMap<>())
                    .merge(payment.getPeriodMonth(), payment.getAmountKopiykas(), Long::sum);
        }

        List<JournalRow> rows = new ArrayList<>();
        for (TariffEnrollment enrollment : enrollments) {
            StudentTariffKey key = new StudentTariffKey(enrollment.getStudent().getId(), enrollment.getTariffPlan().getId());
            rows.add(buildRow(enrollment, year, sumsByStudentTariffAndMonth.getOrDefault(key, Map.of())));
        }
        return new JournalGrid(year, rows);
    }

    private JournalRow buildRow(TariffEnrollment enrollment, int year, Map<Integer, Long> monthSums) {
        YearMonth validFromMonth = YearMonth.from(enrollment.getValidFrom());
        YearMonth validToMonth = enrollment.getValidTo() != null ? YearMonth.from(enrollment.getValidTo()) : null;
        TariffPlan tariffPlan = enrollment.getTariffPlan();

        List<JournalCell> cells = new ArrayList<>(12);
        for (int month = 1; month <= 12; month++) {
            YearMonth cellYearMonth = YearMonth.of(year, month);
            boolean withinRange = !cellYearMonth.isBefore(validFromMonth)
                    && (validToMonth == null || !cellYearMonth.isAfter(validToMonth));
            if (!withinRange) {
                cells.add(null);
                continue;
            }
            long sum = monthSums.getOrDefault(month, 0L);
            Long expectedAmount = tariffPricingService.amountForPlanAt(tariffPlan, cellYearMonth.atDay(1)).orElse(null);
            cells.add(new JournalCell(sum, expectedAmount, statusFor(sum, expectedAmount)));
        }

        return new JournalRow(
                enrollment.getStudent().getId(),
                enrollment.getStudent().getFullName(),
                tariffPlan.getId(),
                tariffPlan.getLabel(),
                cells
        );
    }

    private JournalCell.CellStatus statusFor(long sumKopiykas, Long expectedAmountKopiykas) {
        if (sumKopiykas <= 0) {
            return JournalCell.CellStatus.RED;
        }
        if (expectedAmountKopiykas == null) {
            // paid something, but we have no confirmed target amount to call it "full"
            return JournalCell.CellStatus.YELLOW;
        }
        return sumKopiykas >= expectedAmountKopiykas ? JournalCell.CellStatus.GREEN : JournalCell.CellStatus.YELLOW;
    }

    public List<PaymentDetail> studentMonthDetail(Long studentId, Long tariffPlanId, int year, int month) {
        studentService.getOrThrow(studentId);
        return paymentRepository
                .findByStudentIdAndTariffPlanIdAndMatchStatusAndPeriodYearAndPeriodMonth(
                        studentId, tariffPlanId, PaymentMatchStatus.MATCHED, year, month)
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

    private record StudentTariffKey(Long studentId, Long tariffPlanId) {
    }
}
