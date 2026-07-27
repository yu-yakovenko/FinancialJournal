package org.tonique.vocal.student;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.tonique.vocal.enrollment.TariffEnrollment;
import org.tonique.vocal.enrollment.TariffEnrollmentRepository;
import org.tonique.vocal.payment.Payment;
import org.tonique.vocal.payment.PaymentRepository;
import org.tonique.vocal.tariff.ServiceType;
import org.tonique.vocal.tariff.TariffPlan;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentMergeServiceTest {

    @Mock
    private StudentRepository studentRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private TariffEnrollmentRepository enrollmentRepository;

    private StudentMergeService mergeService;

    private final TariffPlan plan = withId(new TariffPlan(ServiceType.INDIVIDUAL, "Індивідуальні, стандарт"), 10L);

    @BeforeEach
    void setUp() {
        StudentService studentService = new StudentService(studentRepository);
        mergeService = new StudentMergeService(studentService, studentRepository, paymentRepository, enrollmentRepository);
    }

    @Test
    void mergeGroupReassignsPaymentsAndEnrollmentsThenDeletesTheSource() {
        Student target = student(1L, "Фурманюк Антон Васильович", Instant.parse("2026-01-01T00:00:00Z"));
        Student source = student(2L, "Фурманюк Антон", Instant.parse("2026-02-01T00:00:00Z"));
        when(studentRepository.findById(1L)).thenReturn(Optional.of(target));
        when(studentRepository.findById(2L)).thenReturn(Optional.of(source));

        Payment payment = Payment.cash(source, plan, 100000L, LocalDate.of(2026, 5, 1), 2026, 5, "готівка");
        when(paymentRepository.findByStudentId(2L)).thenReturn(List.of(payment));

        TariffEnrollment enrollment = new TariffEnrollment(source, plan, LocalDate.of(2026, 1, 1));
        when(enrollmentRepository.findByStudentIdOrderByValidFrom(2L)).thenReturn(List.of(enrollment));
        when(enrollmentRepository.findByStudentIdAndTariffPlanIdAndValidToIsNull(1L, 10L)).thenReturn(Optional.empty());

        Student result = mergeService.mergeGroup(List.of(1L, 2L));

        assertThat(result).isSameAs(target);
        assertThat(payment.getStudent()).isSameAs(target);
        assertThat(enrollment.getStudent()).isSameAs(target);
        verify(enrollmentRepository).save(enrollment);
        verify(studentRepository).delete(source);
    }

    @Test
    void mergeGroupPicksTheStudentWithTheFullestNameAsTarget() {
        Student partial = student(1L, "Фурманюк Антон", Instant.parse("2026-01-01T00:00:00Z"));
        Student full = student(2L, "Фурманюк Антон Васильович", Instant.parse("2026-02-01T00:00:00Z"));
        when(studentRepository.findById(1L)).thenReturn(Optional.of(partial));
        when(studentRepository.findById(2L)).thenReturn(Optional.of(full));
        when(paymentRepository.findByStudentId(1L)).thenReturn(List.of());
        when(enrollmentRepository.findByStudentIdOrderByValidFrom(1L)).thenReturn(List.of());

        Student result = mergeService.mergeGroup(List.of(1L, 2L));

        assertThat(result).isSameAs(full);
        verify(studentRepository).delete(partial);
        verify(studentRepository, never()).delete(full);
    }

    @Test
    void mergeGroupRequiresAtLeastTwoDistinctStudents() {
        assertThatThrownBy(() -> mergeService.mergeGroup(List.of(1L)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> mergeService.mergeGroup(List.of(1L, 1L)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void redundantActiveEnrollmentOnTheSourceIsDeletedRatherThanReassigned() {
        Student target = student(1L, "Фурманюк Антон Васильович", Instant.parse("2026-01-01T00:00:00Z"));
        Student source = student(2L, "Фурманюк Антон", Instant.parse("2026-02-01T00:00:00Z"));
        when(studentRepository.findById(1L)).thenReturn(Optional.of(target));
        when(studentRepository.findById(2L)).thenReturn(Optional.of(source));
        when(paymentRepository.findByStudentId(2L)).thenReturn(List.of());

        TariffEnrollment sourceEnrollment = new TariffEnrollment(source, plan, LocalDate.of(2026, 3, 1));
        TariffEnrollment targetEnrollment = new TariffEnrollment(target, plan, LocalDate.of(2026, 1, 1));
        when(enrollmentRepository.findByStudentIdOrderByValidFrom(2L)).thenReturn(List.of(sourceEnrollment));
        when(enrollmentRepository.findByStudentIdAndTariffPlanIdAndValidToIsNull(1L, 10L))
                .thenReturn(Optional.of(targetEnrollment));

        mergeService.mergeGroup(List.of(1L, 2L));

        verify(enrollmentRepository).delete(sourceEnrollment);
        verify(enrollmentRepository, never()).save(sourceEnrollment);
        assertThat(sourceEnrollment.getStudent()).isSameAs(source);
    }

    @Test
    void mergeAutomaticallySkipsGroupsThatAreOnlyTransitivelyConnected() {
        Student surnameOnly = student(1L, "Іваненко", Instant.parse("2026-01-01T00:00:00Z"));
        Student sibling1 = student(2L, "Іваненко Ольга Петрівна", Instant.parse("2026-01-01T00:00:00Z"));
        Student sibling2 = student(3L, "Іваненко Оксана Дмитрівна", Instant.parse("2026-01-01T00:00:00Z"));
        when(studentRepository.findByActiveTrueOrderByFullName()).thenReturn(List.of(surnameOnly, sibling1, sibling2));

        StudentMergeService.MergeSummary summary = mergeService.mergeAutomatically();

        assertThat(summary.mergedGroups()).isZero();
        assertThat(summary.mergedStudents()).isZero();
        verify(studentRepository, never()).delete(eq(surnameOnly));
    }

    @Test
    void mergeAutomaticallyMergesACliqueGroup() {
        Student full = student(1L, "Німчук Дмитро Олегович", Instant.parse("2026-01-01T00:00:00Z"));
        Student duplicate = student(2L, "Німчук Дмитро Олегович", Instant.parse("2026-02-01T00:00:00Z"));
        when(studentRepository.findByActiveTrueOrderByFullName()).thenReturn(List.of(full, duplicate));
        when(studentRepository.findById(1L)).thenReturn(Optional.of(full));
        when(studentRepository.findById(2L)).thenReturn(Optional.of(duplicate));
        when(paymentRepository.findByStudentId(2L)).thenReturn(List.of());
        when(enrollmentRepository.findByStudentIdOrderByValidFrom(2L)).thenReturn(List.of());

        StudentMergeService.MergeSummary summary = mergeService.mergeAutomatically();

        assertThat(summary.mergedGroups()).isEqualTo(1);
        assertThat(summary.mergedStudents()).isEqualTo(1);
        verify(studentRepository).delete(duplicate);
    }

    private static Student student(Long id, String fullName, Instant createdAt) {
        Student student = new Student(fullName);
        ReflectionTestUtils.setField(student, "id", id);
        ReflectionTestUtils.setField(student, "createdAt", createdAt);
        return student;
    }

    private static <T> T withId(T entity, Long id) {
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }
}
