package org.tonique.vocal.api;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.tonique.vocal.api.dto.IngestResponse;
import org.tonique.vocal.ingestion.MonobankIngestionScheduler;
import org.tonique.vocal.payment.PaymentIngestionService;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** Manual trigger for the daily import the scheduler runs, plus a one-off historical backfill. */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private static final long MAX_BACKFILL_DAYS = 400;

    private final MonobankIngestionScheduler scheduler;

    public AdminController(MonobankIngestionScheduler scheduler) {
        this.scheduler = scheduler;
    }

    @PostMapping("/ingest")
    public IngestResponse ingest(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        PaymentIngestionService.IngestionResult result = scheduler.ingest(date);
        return new IngestResponse(result.matched(), result.needsReview(), result.skipped());
    }

    /**
     * One-off historical backfill for a date range (e.g. last year's payments).
     * Runs synchronously — for a full year this can take ~10-15 minutes due to
     * Monobank's rate limit, so the caller's connection stays open for the duration.
     */
    @PostMapping("/ingest/backfill")
    public IngestResponse backfill(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        if (to.isBefore(from)) {
            throw new IllegalArgumentException("`to` не може бути раніше за `from`");
        }
        if (ChronoUnit.DAYS.between(from, to) > MAX_BACKFILL_DAYS) {
            throw new IllegalArgumentException("Діапазон занадто великий (максимум %d днів)".formatted(MAX_BACKFILL_DAYS));
        }
        PaymentIngestionService.IngestionResult result = scheduler.backfill(from, to);
        return new IngestResponse(result.matched(), result.needsReview(), result.skipped());
    }
}
