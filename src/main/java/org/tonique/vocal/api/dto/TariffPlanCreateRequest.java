package org.tonique.vocal.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.tonique.vocal.tariff.ServiceType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TariffPlanCreateRequest(
        @NotNull ServiceType serviceType,
        @NotBlank String label,
        @NotNull @Positive BigDecimal initialAmountUah,
        LocalDate effectiveFrom
) {
}
