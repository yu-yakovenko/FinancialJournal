package org.tonique.vocal.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TariffRateCreateRequest(@NotNull @Positive BigDecimal amountUah, LocalDate effectiveFrom) {
}
