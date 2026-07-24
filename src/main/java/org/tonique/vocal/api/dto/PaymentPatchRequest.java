package org.tonique.vocal.api.dto;

public record PaymentPatchRequest(Long studentId, Long tariffPlanId, Integer periodYear, Integer periodMonth) {
}
