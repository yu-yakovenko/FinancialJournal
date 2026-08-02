package org.tonique.vocal.api.dto;

import org.tonique.vocal.payment.PaymentMatchStatus;

public record PaymentPatchRequest(
        Long studentId, Long tariffPlanId, Integer periodYear, Integer periodMonth, PaymentMatchStatus matchStatus) {
}
