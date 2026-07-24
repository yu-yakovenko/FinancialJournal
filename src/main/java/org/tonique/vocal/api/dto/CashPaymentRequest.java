package org.tonique.vocal.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CashPaymentRequest(
        @NotNull Long studentId,
        @NotNull Long tariffPlanId,
        @NotNull @Positive BigDecimal amountUah,
        @NotNull LocalDate paymentDate,
        @NotNull Integer periodYear,
        @NotNull Integer periodMonth,
        String comment
) {
}
