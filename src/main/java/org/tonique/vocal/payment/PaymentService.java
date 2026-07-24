package org.tonique.vocal.payment;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tonique.vocal.enrollment.EnrollmentService;
import org.tonique.vocal.student.Student;
import org.tonique.vocal.student.StudentService;
import org.tonique.vocal.tariff.TariffPlan;
import org.tonique.vocal.tariff.TariffPricingService;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final StudentService studentService;
    private final TariffPricingService tariffPricingService;
    private final EnrollmentService enrollmentService;

    public PaymentService(
            PaymentRepository paymentRepository,
            StudentService studentService,
            TariffPricingService tariffPricingService,
            EnrollmentService enrollmentService
    ) {
        this.paymentRepository = paymentRepository;
        this.studentService = studentService;
        this.tariffPricingService = tariffPricingService;
        this.enrollmentService = enrollmentService;
    }

    @Transactional(readOnly = true)
    public List<Payment> listUnmatched() {
        return paymentRepository.findByMatchStatusOrderByPaymentDateDesc(PaymentMatchStatus.NEEDS_REVIEW);
    }

    public Payment addCash(Long studentId, Long tariffPlanId, long amountKopiykas, LocalDate paymentDate,
                            int periodYear, int periodMonth, String comment) {
        Student student = studentService.getOrThrow(studentId);
        TariffPlan tariffPlan = tariffPricingService.getOrThrow(tariffPlanId);
        ensureEnrollment(student, tariffPlan, periodYear, periodMonth);
        Payment payment = Payment.cash(student, tariffPlan, amountKopiykas, paymentDate, periodYear, periodMonth, comment);
        return paymentRepository.save(payment);
    }

    public Payment resolve(Long paymentId, Long studentId, Long tariffPlanId, int periodYear, int periodMonth) {
        Payment payment = getOrThrow(paymentId);
        Student student = studentService.getOrThrow(studentId);
        TariffPlan tariffPlan = tariffPricingService.getOrThrow(tariffPlanId);
        ensureEnrollment(student, tariffPlan, periodYear, periodMonth);
        payment.setStudent(student);
        payment.setTariffPlan(tariffPlan);
        payment.setPeriodYear(periodYear);
        payment.setPeriodMonth(periodMonth);
        payment.setMatchStatus(PaymentMatchStatus.MATCHED);
        return paymentRepository.save(payment);
    }

    public Payment ignore(Long paymentId) {
        Payment payment = getOrThrow(paymentId);
        payment.setMatchStatus(PaymentMatchStatus.IGNORED);
        return paymentRepository.save(payment);
    }

    public Payment patch(Long paymentId, Long studentId, Long tariffPlanId, Integer periodYear, Integer periodMonth) {
        Payment payment = getOrThrow(paymentId);
        if (studentId != null) {
            payment.setStudent(studentService.getOrThrow(studentId));
        }
        if (tariffPlanId != null) {
            payment.setTariffPlan(tariffPricingService.getOrThrow(tariffPlanId));
        }
        if (periodYear != null) {
            payment.setPeriodYear(periodYear);
        }
        if (periodMonth != null) {
            payment.setPeriodMonth(periodMonth);
        }
        return paymentRepository.save(payment);
    }

    private void ensureEnrollment(Student student, TariffPlan tariffPlan, int periodYear, int periodMonth) {
        enrollmentService.ensureActive(student, tariffPlan, YearMonth.of(periodYear, periodMonth).atDay(1));
    }

    private Payment getOrThrow(Long id) {
        return paymentRepository.findById(id).orElseThrow(() -> new PaymentNotFoundException(id));
    }
}
