package org.tonique.vocal.api.dto;

import java.time.LocalDate;
import java.util.List;

public record DuplicateGroupResponse(List<DuplicateGroupResponse.Student> students) {

    public record Student(
            Long id,
            String fullName,
            boolean recommendedTarget,
            LocalDate lastPaymentDate,
            Long lastPaymentAmountKopiykas,
            String lastPaymentTariffLabel
    ) {
    }
}
