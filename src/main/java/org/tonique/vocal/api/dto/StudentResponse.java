package org.tonique.vocal.api.dto;

import java.time.Instant;
import java.util.List;

public record StudentResponse(
        Long id,
        String fullName,
        List<String> activeTariffLabels,
        boolean active,
        Instant createdAt
) {
}
