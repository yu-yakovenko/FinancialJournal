package org.tonique.vocal.api.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record EnrollmentCreateRequest(@NotNull Long tariffPlanId, LocalDate validFrom) {
}
