package org.tonique.vocal.payment;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tonique.vocal.student.Student;
import org.tonique.vocal.student.StudentService;
import org.tonique.vocal.student.Tariff;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final StudentService studentService;

    public PaymentService(PaymentRepository paymentRepository, StudentService studentService) {
        this.paymentRepository = paymentRepository;
        this.studentService = studentService;
    }

    @Transactional(readOnly = true)
    public List<Payment> listUnmatched() {
        return paymentRepository.findByMatchStatusOrderByPaymentDateDesc(PaymentMatchStatus.NEEDS_REVIEW);
    }

    public Payment addCash(Long studentId, long amountKopiykas, LocalDate paymentDate,
                            int periodYear, int periodMonth, String comment) {
        Student student = studentService.getOrThrow(studentId);
        Payment payment = Payment.cash(student, amountKopiykas, paymentDate, periodYear, periodMonth, comment);
        Tariff.byAmountKopiykas(amountKopiykas).ifPresent(tariff -> studentService.assignTariffIfAbsent(student, tariff));
        return paymentRepository.save(payment);
    }

    public Payment resolve(Long paymentId, Long studentId, int periodYear, int periodMonth) {
        Payment payment = getOrThrow(paymentId);
        Student student = studentService.getOrThrow(studentId);
        payment.setStudent(student);
        payment.setPeriodYear(periodYear);
        payment.setPeriodMonth(periodMonth);
        payment.setMatchStatus(PaymentMatchStatus.MATCHED);
        Optional<Tariff> tariff = Tariff.byAmountKopiykas(payment.getAmountKopiykas());
        tariff.ifPresent(t -> studentService.assignTariffIfAbsent(student, t));
        return paymentRepository.save(payment);
    }

    public Payment ignore(Long paymentId) {
        Payment payment = getOrThrow(paymentId);
        payment.setMatchStatus(PaymentMatchStatus.IGNORED);
        return paymentRepository.save(payment);
    }

    public Payment patch(Long paymentId, Long studentId, Integer periodYear, Integer periodMonth) {
        Payment payment = getOrThrow(paymentId);
        if (studentId != null) {
            payment.setStudent(studentService.getOrThrow(studentId));
        }
        if (periodYear != null) {
            payment.setPeriodYear(periodYear);
        }
        if (periodMonth != null) {
            payment.setPeriodMonth(periodMonth);
        }
        return paymentRepository.save(payment);
    }

    private Payment getOrThrow(Long id) {
        return paymentRepository.findById(id).orElseThrow(() -> new PaymentNotFoundException(id));
    }
}
