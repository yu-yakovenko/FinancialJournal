package org.tonique.vocal.api.dto;

import org.tonique.vocal.payment.Payment;
import org.tonique.vocal.payment.PaymentMatchStatus;
import org.tonique.vocal.payment.PaymentSource;

import java.time.LocalDate;

public record PaymentResponse(
        Long id,
        Long studentId,
        Long tariffPlanId,
        String tariffLabel,
        PaymentSource source,
        PaymentMatchStatus matchStatus,
        long amountKopiykas,
        LocalDate paymentDate,
        Integer periodYear,
        Integer periodMonth,
        String rawComment,
        String parsedPayerName
) {

    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getStudent() != null ? payment.getStudent().getId() : null,
                payment.getTariffPlan() != null ? payment.getTariffPlan().getId() : null,
                payment.getTariffPlan() != null ? payment.getTariffPlan().getLabel() : null,
                payment.getSource(),
                payment.getMatchStatus(),
                payment.getAmountKopiykas(),
                payment.getPaymentDate(),
                payment.getPeriodYear(),
                payment.getPeriodMonth(),
                payment.getRawComment(),
                payment.getParsedPayerName()
        );
    }
}
