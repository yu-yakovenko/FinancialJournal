package org.tonique.vocal.payment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    boolean existsByMonobankTransactionId(String monobankTransactionId);

    List<Payment> findByMatchStatusOrderByPaymentDateDesc(PaymentMatchStatus matchStatus);

    List<Payment> findByMatchStatusAndPeriodYear(PaymentMatchStatus matchStatus, int periodYear);

    List<Payment> findByStudentIdAndMatchStatusAndPeriodYearAndPeriodMonth(
            Long studentId, PaymentMatchStatus matchStatus, int periodYear, int periodMonth);
}
