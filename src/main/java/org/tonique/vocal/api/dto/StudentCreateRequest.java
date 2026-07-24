package org.tonique.vocal.api.dto;

import jakarta.validation.constraints.NotBlank;
import org.tonique.vocal.student.Tariff;

public record StudentCreateRequest(@NotBlank String fullName, Tariff tariff) {
}
