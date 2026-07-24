package org.tonique.vocal.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.tonique.vocal.journal.JournalGrid;
import org.tonique.vocal.journal.JournalService;
import org.tonique.vocal.journal.PaymentDetail;

import java.time.Year;
import java.util.List;

@RestController
@RequestMapping("/api")
public class JournalController {

    private final JournalService journalService;

    public JournalController(JournalService journalService) {
        this.journalService = journalService;
    }

    @GetMapping("/journal")
    public JournalGrid journal(@RequestParam(required = false) Integer year) {
        return journalService.buildGrid(year != null ? year : Year.now().getValue());
    }

    @GetMapping("/students/{id}/payments")
    public List<PaymentDetail> studentPayments(
            @PathVariable Long id,
            @RequestParam Long tariffPlanId,
            @RequestParam int year,
            @RequestParam int month
    ) {
        return journalService.studentMonthDetail(id, tariffPlanId, year, month);
    }
}
