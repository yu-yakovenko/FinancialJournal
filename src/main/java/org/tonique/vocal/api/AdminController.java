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

/** Manual trigger for the same daily import the scheduler runs — for local testing, since there's no history backfill. */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final MonobankIngestionScheduler scheduler;

    public AdminController(MonobankIngestionScheduler scheduler) {
        this.scheduler = scheduler;
    }

    @PostMapping("/ingest")
    public IngestResponse ingest(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        PaymentIngestionService.IngestionResult result = scheduler.ingest(date);
        return new IngestResponse(result.matched(), result.needsReview(), result.skipped());
    }
}
