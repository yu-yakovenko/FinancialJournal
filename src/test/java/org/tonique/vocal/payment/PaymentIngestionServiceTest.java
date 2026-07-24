package org.tonique.vocal.payment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.tonique.vocal.enrollment.EnrollmentService;
import org.tonique.vocal.monobank.StatementItem;
import org.tonique.vocal.student.Student;
import org.tonique.vocal.student.StudentService;
import org.tonique.vocal.tariff.ServiceType;
import org.tonique.vocal.tariff.TariffPlan;
import org.tonique.vocal.tariff.TariffPricing;
import org.tonique.vocal.tariff.TariffPricingService;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentIngestionServiceTest {

    private static final LocalDate STATEMENT_DATE = LocalDate.of(2026, 8, 15);

    private static final TariffPlan CHOIR_STANDARD = new TariffPlan(ServiceType.CHOIR, "Хор, стандартний");

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private StudentService studentService;

    @Mock
    private TariffPricingService tariffPricingService;

    @Mock
    private EnrollmentService enrollmentService;

    private PaymentIngestionService ingestionService;

    @BeforeEach
    void setUp() {
        ingestionService = new PaymentIngestionService(paymentRepository, studentService, tariffPricingService, enrollmentService);
    }

    @Test
    void autoCreatesStudentAndEnrollsInTariffOnFirstMatchingPayment() {
        StatementItem item = statementItem("tx-1", 170_000,
                "Оплата за уроки вокалу, серпень, Іваненко Ольга Петрівна");

        when(studentService.findActive()).thenReturn(List.of());
        when(tariffPricingService.plansForAmountAt(170_000, LocalDate.of(2026, 8, 1)))
                .thenReturn(new TariffPricing.TariffMatchResult(List.of(CHOIR_STANDARD)));
        Student created = new Student("Іваненко Ольга Петрівна");
        when(studentService.createFromPayment("Іваненко Ольга Петрівна"))
                .thenReturn(created);

        PaymentIngestionService.IngestionResult result = ingestionService.ingest(List.of(item));

        assertThat(result.matched()).isEqualTo(1);
        assertThat(result.needsReview()).isZero();

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        assertThat(captor.getValue().getMatchStatus()).isEqualTo(PaymentMatchStatus.MATCHED);
        assertThat(captor.getValue().getStudent()).isEqualTo(created);
        assertThat(captor.getValue().getTariffPlan()).isEqualTo(CHOIR_STANDARD);
        assertThat(captor.getValue().getPeriodYear()).isEqualTo(2026);
        assertThat(captor.getValue().getPeriodMonth()).isEqualTo(8);
        verify(enrollmentService).ensureActive(created, CHOIR_STANDARD, LocalDate.of(2026, 8, 1));
    }

    @Test
    void matchesExistingStudentBySurnameAndInitialsWithoutCreatingDuplicate() {
        Student existing = new Student("Іваненко Ольга Петрівна");
        StatementItem item = statementItem("tx-2", 170_000,
                "Оплата за уроки вокалу, вересня, Іваненко О.П.");

        when(studentService.findActive()).thenReturn(List.of(existing));
        when(tariffPricingService.plansForAmountAt(eq(170_000L), any(LocalDate.class)))
                .thenReturn(new TariffPricing.TariffMatchResult(List.of(CHOIR_STANDARD)));

        PaymentIngestionService.IngestionResult result = ingestionService.ingest(List.of(item));

        assertThat(result.matched()).isEqualTo(1);
        verify(studentService, never()).createFromPayment(any());

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        assertThat(captor.getValue().getStudent()).isEqualTo(existing);
        assertThat(captor.getValue().getTariffPlan()).isEqualTo(CHOIR_STANDARD);
    }

    @Test
    void malformedCommentGoesToNeedsReview() {
        StatementItem item = statementItem("tx-3", 170_000, "Поповнення рахунку");

        PaymentIngestionService.IngestionResult result = ingestionService.ingest(List.of(item));

        assertThat(result.needsReview()).isEqualTo(1);
        assertThat(result.matched()).isZero();

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        assertThat(captor.getValue().getMatchStatus()).isEqualTo(PaymentMatchStatus.NEEDS_REVIEW);
        assertThat(captor.getValue().getStudent()).isNull();
    }

    @Test
    void unknownAmountGoesToNeedsReviewWithoutEvenLookingUpTheRoster() {
        StatementItem item = statementItem("tx-4", 999_900,
                "Оплата за уроки вокалу, серпень, Новий Студент Тестович");

        when(tariffPricingService.plansForAmountAt(eq(999_900L), any(LocalDate.class)))
                .thenReturn(new TariffPricing.TariffMatchResult(List.of()));

        PaymentIngestionService.IngestionResult result = ingestionService.ingest(List.of(item));

        assertThat(result.needsReview()).isEqualTo(1);
        verify(studentService, never()).findActive();
        verify(studentService, never()).createFromPayment(any());
    }

    @Test
    void ambiguousTariffMatchGoesToNeedsReviewEvenWithAKnownName() {
        TariffPlan otherPlanSamePrice = new TariffPlan(ServiceType.INDIVIDUAL, "Індивідуальні, тимчасово подорожчали");
        StatementItem item = statementItem("tx-7", 170_000,
                "Оплата за уроки вокалу, серпень, Іваненко Ольга Петрівна");

        when(tariffPricingService.plansForAmountAt(eq(170_000L), any(LocalDate.class)))
                .thenReturn(new TariffPricing.TariffMatchResult(List.of(CHOIR_STANDARD, otherPlanSamePrice)));

        PaymentIngestionService.IngestionResult result = ingestionService.ingest(List.of(item));

        assertThat(result.needsReview()).isEqualTo(1);
        verify(studentService, never()).findActive();
        verify(studentService, never()).createFromPayment(any());
    }

    @Test
    void ambiguousNameMatchGoesToNeedsReviewEvenWithACertainTariff() {
        Student ivanenkoOlga = new Student("Іваненко Ольга Петрівна");
        Student ivanenkoOksana = new Student("Іваненко Оксана Петрівна");
        StatementItem item = statementItem("tx-8", 170_000,
                "Оплата за уроки вокалу, серпень, Іваненко О.П.");

        when(studentService.findActive()).thenReturn(List.of(ivanenkoOlga, ivanenkoOksana));
        when(tariffPricingService.plansForAmountAt(eq(170_000L), any(LocalDate.class)))
                .thenReturn(new TariffPricing.TariffMatchResult(List.of(CHOIR_STANDARD)));

        PaymentIngestionService.IngestionResult result = ingestionService.ingest(List.of(item));

        assertThat(result.needsReview()).isEqualTo(1);
        verify(studentService, never()).createFromPayment(any());
    }

    @Test
    void duplicateTransactionIdIsSkippedForIdempotency() {
        StatementItem item = statementItem("tx-5", 170_000,
                "Оплата за уроки вокалу, серпень, Іваненко Ольга Петрівна");

        when(paymentRepository.existsByMonobankTransactionId("tx-5")).thenReturn(true);

        PaymentIngestionService.IngestionResult result = ingestionService.ingest(List.of(item));

        assertThat(result.skipped()).isEqualTo(1);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void negativeAmountIsSkipped() {
        StatementItem item = statementItem("tx-6", -5_000, "Комісія за обслуговування");

        PaymentIngestionService.IngestionResult result = ingestionService.ingest(List.of(item));

        assertThat(result.skipped()).isEqualTo(1);
        verify(paymentRepository, never()).save(any());
    }

    private static StatementItem statementItem(String id, long amount, String comment) {
        return new StatementItem(
                id, STATEMENT_DATE.atStartOfDay().toEpochSecond(ZoneOffset.UTC),
                "description", 0, 0, false, amount, amount, 980, 0, 0, 0,
                comment, null, null, null, null, null
        );
    }
}
