package org.tonique.vocal.ingestion;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.tonique.vocal.monobank.MonobankClient;
import org.tonique.vocal.monobank.StatementItem;
import org.tonique.vocal.payment.PaymentIngestionService;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Component
public class MonobankIngestionScheduler {

    private static final ZoneId KYIV_ZONE = ZoneId.of("Europe/Kyiv");

    private final MonobankClient monobankClient;
    private final PaymentIngestionService paymentIngestionService;

    public MonobankIngestionScheduler(MonobankClient monobankClient, PaymentIngestionService paymentIngestionService) {
        this.monobankClient = monobankClient;
        this.paymentIngestionService = paymentIngestionService;
    }

    /**
     * Запускається щодня о 00:05 за київським часом
     * і завантажує та зберігає виписку за попередній день.
     */
    @Scheduled(cron = "0 5 0 * * *", zone = "Europe/Kyiv")
    public void loadPreviousDayStatement() {
        LocalDate date = LocalDate.now(KYIV_ZONE).minusDays(1);
        ingest(date);
    }

    public PaymentIngestionService.IngestionResult ingest(LocalDate date) {
        try {
            List<StatementItem> statement = monobankClient.loadStatement(date);
            PaymentIngestionService.IngestionResult result = paymentIngestionService.ingest(statement, date);

            System.out.printf(
                    "Виписка за %s: отримано %d операцій, зараховано %d, на перевірку %d, пропущено %d%n",
                    date,
                    statement.size(),
                    result.matched(),
                    result.needsReview(),
                    result.skipped()
            );

            return result;
        } catch (Exception exception) {
            System.err.printf(
                    "Не вдалося отримати виписку за %s: %s%n",
                    date,
                    exception.getMessage()
            );
            return new PaymentIngestionService.IngestionResult(0, 0, 0);
        }
    }
}
