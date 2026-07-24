package org.tonique.vocal.api.dto;

import java.time.LocalDate;

public record TariffRateResponse(Long id, long amountKopiykas, LocalDate effectiveFrom) {
}
