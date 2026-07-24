package org.tonique.vocal.enrollment;

import org.junit.jupiter.api.Test;
import org.tonique.vocal.student.Student;
import org.tonique.vocal.tariff.ServiceType;
import org.tonique.vocal.tariff.TariffPlan;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class TariffEnrollmentTest {

    private final Student student = new Student("Дворецька Софія Романівна");
    private final TariffPlan plan = new TariffPlan(ServiceType.CHOIR, "Хор, стандартний");

    @Test
    void stillActiveEnrollmentOverlapsItsStartYearAndEveryYearAfter() {
        TariffEnrollment enrollment = new TariffEnrollment(student, plan, LocalDate.of(2026, 3, 1));

        assertThat(enrollment.overlaps(2026)).isTrue();
        assertThat(enrollment.overlaps(2027)).isTrue();
        assertThat(enrollment.overlaps(2025)).isFalse();
    }

    @Test
    void endedEnrollmentOnlyOverlapsYearsItWasActiveIn() {
        TariffEnrollment enrollment = new TariffEnrollment(student, plan, LocalDate.of(2025, 1, 1));
        enrollment.setValidTo(LocalDate.of(2025, 6, 30));

        assertThat(enrollment.overlaps(2025)).isTrue();
        assertThat(enrollment.overlaps(2026)).isFalse();
        assertThat(enrollment.overlaps(2024)).isFalse();
    }

    @Test
    void endingExactlyOnJanuaryFirstStillCountsAsOverlappingThatYear() {
        TariffEnrollment enrollment = new TariffEnrollment(student, plan, LocalDate.of(2025, 1, 1));
        enrollment.setValidTo(LocalDate.of(2026, 1, 1));

        assertThat(enrollment.overlaps(2026)).isTrue();
    }

    @Test
    void isActiveReflectsWhetherValidToIsSet() {
        TariffEnrollment enrollment = new TariffEnrollment(student, plan, LocalDate.of(2026, 1, 1));
        assertThat(enrollment.isActive()).isTrue();

        enrollment.setValidTo(LocalDate.of(2026, 6, 1));
        assertThat(enrollment.isActive()).isFalse();

        enrollment.setValidTo(null);
        assertThat(enrollment.isActive()).isTrue();
    }
}
