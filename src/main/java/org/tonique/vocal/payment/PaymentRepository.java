package org.tonique.vocal.payment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    boolean existsByMonobankTransactionId(String monobankTransactionId);

    List<Payment> findByMatchStatusOrderByPaymentDateDesc(PaymentMatchStatus matchStatus);

    List<Payment> findByMatchStatusAndPeriodYear(PaymentMatchStatus matchStatus, int periodYear);

    List<Payment> findByStudentIdAndTariffPlanIdAndMatchStatusAndPeriodYearAndPeriodMonth(
            Long studentId, Long tariffPlanId, PaymentMatchStatus matchStatus, int periodYear, int periodMonth);

    List<Payment> findByStudentId(Long studentId);

    Optional<Payment> findFirstByStudentIdOrderByPaymentDateDesc(Long studentId);
}
