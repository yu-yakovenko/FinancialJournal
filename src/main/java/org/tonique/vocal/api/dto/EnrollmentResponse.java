package org.tonique.vocal.api.dto;

import java.time.LocalDate;

public record EnrollmentResponse(
        Long id,
        Long studentId,
        Long tariffPlanId,
        String tariffLabel,
        LocalDate validFrom,
        LocalDate validTo,
        boolean active
) {
}
