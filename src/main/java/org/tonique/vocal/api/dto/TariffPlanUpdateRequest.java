package org.tonique.vocal.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TariffPlanUpdateRequest(@NotBlank String label, @NotNull Boolean active) {
}
