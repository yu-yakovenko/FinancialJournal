package org.tonique.vocal.journal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.tonique.vocal.enrollment.EnrollmentService;
import org.tonique.vocal.enrollment.TariffEnrollment;
import org.tonique.vocal.payment.Payment;
import org.tonique.vocal.payment.PaymentMatchStatus;
import org.tonique.vocal.payment.PaymentRepository;
import org.tonique.vocal.student.Student;
import org.tonique.vocal.student.StudentService;
import org.tonique.vocal.tariff.ServiceType;
import org.tonique.vocal.tariff.TariffPlan;
import org.tonique.vocal.tariff.TariffPricingService;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JournalServiceTest {

    @Mock
    private StudentService studentService;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private TariffPricingService tariffPricingService;

    @Mock
    private EnrollmentService enrollmentService;

    private JournalService journalService;

    @BeforeEach
    void setUp() {
        journalService = new JournalService(studentService, paymentRepository, tariffPricingService, enrollmentService);
    }

    @Test
    void buildsOneRowPerEnrollmentWithIndependentSumsAndHidesCellsBeforeValidFrom() {
        Student student = withId(new Student("Дворецька Софія Романівна"), 1L);
        TariffPlan choir = withId(new TariffPlan(ServiceType.CHOIR, "Хор, стандартний"), 10L);
        TariffPlan individual = withId(new TariffPlan(ServiceType.INDIVIDUAL, "Індивідуальні, базовий"), 20L);

        TariffEnrollment choirEnrollment = new TariffEnrollment(student, choir, LocalDate.of(2026, 1, 1));
        TariffEnrollment individualEnrollment = new TariffEnrollment(student, individual, LocalDate.of(2026, 6, 1));

        when(studentService.findActive()).thenReturn(List.of(student));
        when(enrollmentService.findForStudents(List.of(1L))).thenReturn(List.of(choirEnrollment, individualEnrollment));

        Payment choirPayment = Payment.cash(student, choir, 170_000, LocalDate.of(2026, 8, 3), 2026, 8, "хор");
        Payment individualPayment = Payment.cash(student, individual, 30_000, LocalDate.of(2026, 8, 4), 2026, 8, "індивідуальні");
        when(paymentRepository.findByMatchStatusAndPeriodYear(PaymentMatchStatus.MATCHED, 2026))
                .thenReturn(List.of(choirPayment, individualPayment));

        when(tariffPricingService.amountForPlanAt(eq(choir), any(LocalDate.class))).thenReturn(Optional.of(170_000L));
        when(tariffPricingService.amountForPlanAt(eq(individual), any(LocalDate.class))).thenReturn(Optional.of(50_000L));

        JournalGrid grid = journalService.buildGrid(2026);

        assertThat(grid.rows()).hasSize(2);

        JournalRow choirRow = grid.rows().stream().filter(r -> r.tariffPlanId().equals(10L)).findFirst().orElseThrow();
        JournalRow individualRow = grid.rows().stream().filter(r -> r.tariffPlanId().equals(20L)).findFirst().orElseThrow();

        // choir enrollment started in January -> August (index 7) fully paid
        assertThat(choirRow.cells().get(7).status()).isEqualTo(JournalCell.CellStatus.GREEN);
        assertThat(choirRow.cells().get(7).amountKopiykas()).isEqualTo(170_000L);
        assertThat(choirRow.cells().get(0)).isNotNull(); // January visible, just unpaid (RED)
        assertThat(choirRow.cells().get(0).status()).isEqualTo(JournalCell.CellStatus.RED);

        // individual enrollment only started in June -> earlier months must be hidden
        assertThat(individualRow.cells().get(0)).isNull();
        assertThat(individualRow.cells().get(4)).isNull();
        assertThat(individualRow.cells().get(5)).isNotNull();
        assertThat(individualRow.cells().get(7).status()).isEqualTo(JournalCell.CellStatus.YELLOW);
        assertThat(individualRow.cells().get(7).amountKopiykas()).isEqualTo(30_000L);
    }

    @Test
    void enrollmentEndedBeforeTheSelectedYearIsExcludedEntirely() {
        Student student = withId(new Student("Коваль Андрій Борисович"), 2L);
        TariffPlan plan = withId(new TariffPlan(ServiceType.INDIVIDUAL, "Індивідуальні, базовий"), 21L);

        TariffEnrollment endedEnrollment = new TariffEnrollment(student, plan, LocalDate.of(2024, 1, 1));
        endedEnrollment.setValidTo(LocalDate.of(2024, 12, 31));

        when(studentService.findActive()).thenReturn(List.of(student));
        when(enrollmentService.findForStudents(List.of(2L))).thenReturn(List.of(endedEnrollment));
        when(paymentRepository.findByMatchStatusAndPeriodYear(PaymentMatchStatus.MATCHED, 2026)).thenReturn(List.of());

        JournalGrid grid = journalService.buildGrid(2026);

        assertThat(grid.rows()).isEmpty();
    }

    private static <T> T withId(T entity, Long id) {
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }
}
