package org.tonique.vocal.ingestion;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.tonique.vocal.monobank.MonobankClient;
import org.tonique.vocal.monobank.StatementItem;
import org.tonique.vocal.payment.PaymentIngestionService;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MonobankIngestionSchedulerTest {

    @Mock
    private MonobankClient monobankClient;

    @Mock
    private PaymentIngestionService paymentIngestionService;

    @Test
    void backfillFetchesTheRangeAndDelegatesToIngestion() throws Exception {
        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to = LocalDate.of(2025, 12, 31);
        List<StatementItem> items = List.of();

        when(monobankClient.loadStatement(from, to)).thenReturn(items);
        when(paymentIngestionService.ingest(items))
                .thenReturn(new PaymentIngestionService.IngestionResult(3, 1, 0));

        PaymentIngestionService.IngestionResult result = newScheduler().backfill(from, to);

        assertThat(result.matched()).isEqualTo(3);
        assertThat(result.needsReview()).isEqualTo(1);
        verify(monobankClient).loadStatement(from, to);
        verify(paymentIngestionService).ingest(items);
    }

    @Test
    void backfillSwallowsClientErrorsAndReturnsZeroedResult() throws Exception {
        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to = LocalDate.of(2025, 1, 31);

        when(monobankClient.loadStatement(from, to)).thenThrow(new RuntimeException("Monobank недоступний"));

        PaymentIngestionService.IngestionResult result = newScheduler().backfill(from, to);

        assertThat(result.matched()).isZero();
        assertThat(result.needsReview()).isZero();
        assertThat(result.skipped()).isZero();
        verify(paymentIngestionService, org.mockito.Mockito.never()).ingest(any());
    }

    private MonobankIngestionScheduler newScheduler() {
        return new MonobankIngestionScheduler(monobankClient, paymentIngestionService);
    }
}
