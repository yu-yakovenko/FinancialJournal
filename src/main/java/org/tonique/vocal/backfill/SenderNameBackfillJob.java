package org.tonique.vocal.backfill;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.tonique.vocal.monobank.MonobankClient;
import org.tonique.vocal.monobank.StatementItem;
import org.tonique.vocal.payment.Payment;
import org.tonique.vocal.payment.PaymentMatchStatus;
import org.tonique.vocal.payment.PaymentRepository;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * ONE-OFF, REMOVABLE job (delete this whole package once it has run successfully in
 * production): backfills {@code senderName} on existing NEEDS_REVIEW payments that
 * predate that field, by re-fetching the Monobank statement covering their dates and
 * matching {@link StatementItem#counterName()} back onto each payment via
 * monobankTransactionId. Runs once, automatically, on app startup — off the main
 * thread so a long rate-limited backfill never delays server startup or health
 * checks — and is a no-op once every NEEDS_REVIEW payment already has a senderName
 * (or an attempt was already made and Monobank simply has no data for it), so
 * restarting the app after a successful run costs nothing.
 */
@Component
public class SenderNameBackfillJob implements ApplicationListener<ApplicationReadyEvent> {

    private final PaymentRepository paymentRepository;
    private final MonobankClient monobankClient;

    public SenderNameBackfillJob(PaymentRepository paymentRepository, MonobankClient monobankClient) {
        this.paymentRepository = paymentRepository;
        this.monobankClient = monobankClient;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        Thread thread = new Thread(this::runJob, "sender-name-backfill");
        thread.setDaemon(true);
        thread.start();
    }

    void runJob() {
        List<Payment> pending = paymentRepository.findByMatchStatusOrderByPaymentDateDesc(PaymentMatchStatus.NEEDS_REVIEW)
                .stream()
                .filter(payment -> payment.getSenderName() == null)
                .toList();

        if (pending.isEmpty()) {
            return;
        }

        System.out.printf(
                "[Разова джоба відправників] Довантажуємо відправника для %d неопрацьованих платежів...%n",
                pending.size()
        );

        LocalDate from = pending.stream().map(Payment::getPaymentDate).min(Comparator.naturalOrder()).orElseThrow();
        LocalDate to = pending.stream().map(Payment::getPaymentDate).max(Comparator.naturalOrder()).orElseThrow();

        try {
            // MonobankClient.loadStatement(from, to) already chunks into ≤31-day requests
            // and sleeps ~61s between them to respect Monobank's 1-request-per-minute limit.
            List<StatementItem> statement = monobankClient.loadStatement(from, to);
            Map<String, StatementItem> byTransactionId = statement.stream()
                    .collect(Collectors.toMap(StatementItem::id, Function.identity(), (a, b) -> a));

            int updated = 0;
            for (Payment payment : pending) {
                StatementItem item = byTransactionId.get(payment.getMonobankTransactionId());
                if (item != null && item.counterName() != null) {
                    payment.setSenderName(item.counterName());
                    paymentRepository.save(payment);
                    updated++;
                }
            }

            System.out.printf(
                    "[Разова джоба відправників] Завершено: оновлено відправника для %d із %d платежів.%n",
                    updated, pending.size()
            );
        } catch (Exception exception) {
            System.err.printf(
                    "[Разова джоба відправників] Не вдалося довантажити виписку %s — %s: %s%n",
                    from, to, exception.getMessage()
            );
        }
    }
}
