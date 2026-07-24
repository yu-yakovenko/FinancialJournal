package org.tonique.vocal.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.tonique.vocal.student.Tariff;

public record StudentUpdateRequest(@NotBlank String fullName, Tariff tariff, @NotNull Boolean active) {
}
