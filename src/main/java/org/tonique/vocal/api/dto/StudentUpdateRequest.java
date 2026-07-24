package org.tonique.vocal.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record StudentUpdateRequest(@NotBlank String fullName, @NotNull Boolean active) {
}
