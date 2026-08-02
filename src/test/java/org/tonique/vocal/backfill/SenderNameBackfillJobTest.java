package org.tonique.vocal.backfill;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.tonique.vocal.monobank.MonobankClient;
import org.tonique.vocal.monobank.StatementItem;
import org.tonique.vocal.payment.Payment;
import org.tonique.vocal.payment.PaymentMatchStatus;
import org.tonique.vocal.payment.PaymentRepository;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SenderNameBackfillJobTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private MonobankClient monobankClient;

    private SenderNameBackfillJob job;

    @BeforeEach
    void setUp() {
        job = new SenderNameBackfillJob(paymentRepository, monobankClient);
    }

    @Test
    void doesNothingWhenNoPendingPaymentIsMissingASenderName() throws Exception {
        when(paymentRepository.findByMatchStatusOrderByPaymentDateDesc(PaymentMatchStatus.NEEDS_REVIEW))
                .thenReturn(List.of());

        job.runJob();

        verifyNoInteractions(monobankClient);
    }

    @Test
    void fetchesTheStatementForThePendingDateRangeAndFillsInMatchingSenderNames() throws Exception {
        Payment matched = Payment.bank("tx-1", 170_000, LocalDate.of(2026, 6, 1), "Оплата за уроки вокалу, червень", null);
        Payment noStatementMatch = Payment.bank("tx-2", 170_000, LocalDate.of(2026, 8, 1), "Оплата за уроки вокалу, серпень", null);

        when(paymentRepository.findByMatchStatusOrderByPaymentDateDesc(PaymentMatchStatus.NEEDS_REVIEW))
                .thenReturn(List.of(matched, noStatementMatch));

        StatementItem item = new StatementItem(
                "tx-1", 0L, "description", 0, 0, false, 170_000, 170_000, 980, 0, 0, 0,
                "Оплата за уроки вокалу, червень", null, null, null, null, "Іваненко Ольга Петрівна"
        );
        when(monobankClient.loadStatement(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 8, 1)))
                .thenReturn(List.of(item));

        job.runJob();

        assertThat(matched.getSenderName()).isEqualTo("Іваненко Ольга Петрівна");
        assertThat(noStatementMatch.getSenderName()).isNull();
        verify(paymentRepository).save(matched);
        verify(paymentRepository, never()).save(noStatementMatch);
    }

    @Test
    void skipsPaymentsAlreadyHavingASenderName() throws Exception {
        Payment alreadyHasSenderName = Payment.bank("tx-3", 170_000, LocalDate.of(2026, 6, 1), "Оплата", "Вже відомо");

        when(paymentRepository.findByMatchStatusOrderByPaymentDateDesc(PaymentMatchStatus.NEEDS_REVIEW))
                .thenReturn(List.of(alreadyHasSenderName));

        job.runJob();

        verifyNoInteractions(monobankClient);
    }
}
