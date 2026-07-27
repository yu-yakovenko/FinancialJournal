package org.tonique.vocal.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TariffRateUpdateRequest(@NotNull @Positive BigDecimal amountUah, @NotNull LocalDate effectiveFrom) {
}
