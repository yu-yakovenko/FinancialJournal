package org.tonique.vocal.api.dto;

import jakarta.validation.constraints.NotNull;

public record PaymentResolveRequest(
        @NotNull Long studentId,
        @NotNull Long tariffPlanId,
        @NotNull Integer periodYear,
        @NotNull Integer periodMonth
) {
}
