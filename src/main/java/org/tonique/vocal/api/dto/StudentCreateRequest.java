package org.tonique.vocal.api.dto;

import jakarta.validation.constraints.NotBlank;

public record StudentCreateRequest(@NotBlank String fullName) {
}
