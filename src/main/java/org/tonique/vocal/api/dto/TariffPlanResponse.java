package org.tonique.vocal.api.dto;

import org.tonique.vocal.tariff.ServiceType;

public record TariffPlanResponse(
        Long id,
        ServiceType serviceType,
        String label,
        boolean active,
        Long currentAmountKopiykas
) {
}
