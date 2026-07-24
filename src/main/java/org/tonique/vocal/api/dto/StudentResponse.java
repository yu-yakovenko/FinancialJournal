package org.tonique.vocal.api.dto;

import org.tonique.vocal.student.Student;
import org.tonique.vocal.student.Tariff;

import java.time.Instant;

public record StudentResponse(
        Long id,
        String fullName,
        Tariff tariff,
        Long tariffAmountKopiykas,
        boolean active,
        Instant createdAt
) {

    public static StudentResponse from(Student student) {
        Tariff tariff = student.getTariff();
        return new StudentResponse(
                student.getId(),
                student.getFullName(),
                tariff,
                tariff != null ? tariff.amountKopiykas() : null,
                student.isActive(),
                student.getCreatedAt()
        );
    }
}
