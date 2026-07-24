package org.tonique.vocal.enrollment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.tonique.vocal.student.Student;
import org.tonique.vocal.tariff.ServiceType;
import org.tonique.vocal.tariff.TariffPlan;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {

    @Mock
    private TariffEnrollmentRepository enrollmentRepository;

    private EnrollmentService enrollmentService;

    private final Student student = withId(new Student("Дворецька Софія Романівна"), 1L);
    private final TariffPlan plan = withId(new TariffPlan(ServiceType.CHOIR, "Хор, стандартний"), 10L);

    @BeforeEach
    void setUp() {
        enrollmentService = new EnrollmentService(enrollmentRepository);
    }

    @Test
    void reusesTheExistingActiveEnrollmentInsteadOfCreatingADuplicate() {
        TariffEnrollment existing = new TariffEnrollment(student, plan, LocalDate.of(2026, 1, 1));
        when(enrollmentRepository.findByStudentIdAndTariffPlanIdAndValidToIsNull(1L, 10L))
                .thenReturn(Optional.of(existing));

        TariffEnrollment result = enrollmentService.ensureActive(student, plan, LocalDate.of(2026, 8, 1));

        assertThat(result).isSameAs(existing);
        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    void createsANewEnrollmentWhenNoneIsCurrentlyActive() {
        when(enrollmentRepository.findByStudentIdAndTariffPlanIdAndValidToIsNull(1L, 10L))
                .thenReturn(Optional.empty());
        when(enrollmentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        TariffEnrollment result = enrollmentService.ensureActive(student, plan, LocalDate.of(2026, 8, 1));

        ArgumentCaptor<TariffEnrollment> captor = ArgumentCaptor.forClass(TariffEnrollment.class);
        verify(enrollmentRepository).save(captor.capture());
        assertThat(captor.getValue()).isSameAs(result);
        assertThat(result.getStudent()).isSameAs(student);
        assertThat(result.getTariffPlan()).isSameAs(plan);
        assertThat(result.getValidFrom()).isEqualTo(LocalDate.of(2026, 8, 1));
    }

    @Test
    void endSetsValidToAndSaves() {
        TariffEnrollment enrollment = new TariffEnrollment(student, plan, LocalDate.of(2026, 1, 1));
        when(enrollmentRepository.findById(5L)).thenReturn(Optional.of(enrollment));
        when(enrollmentRepository.save(enrollment)).thenReturn(enrollment);

        TariffEnrollment result = enrollmentService.end(5L, LocalDate.of(2026, 8, 1));

        assertThat(result.getValidTo()).isEqualTo(LocalDate.of(2026, 8, 1));
        assertThat(result.isActive()).isFalse();
    }

    @Test
    void reactivateClearsValidTo() {
        TariffEnrollment enrollment = new TariffEnrollment(student, plan, LocalDate.of(2026, 1, 1));
        enrollment.setValidTo(LocalDate.of(2026, 8, 1));
        when(enrollmentRepository.findById(5L)).thenReturn(Optional.of(enrollment));
        when(enrollmentRepository.save(enrollment)).thenReturn(enrollment);

        TariffEnrollment result = enrollmentService.reactivate(5L);

        assertThat(result.getValidTo()).isNull();
        assertThat(result.isActive()).isTrue();
    }

    @Test
    void endingAnUnknownEnrollmentThrows() {
        when(enrollmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> enrollmentService.end(99L, LocalDate.now()))
                .isInstanceOf(EnrollmentNotFoundException.class);
    }

    private static <T> T withId(T entity, Long id) {
        org.springframework.test.util.ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }
}
