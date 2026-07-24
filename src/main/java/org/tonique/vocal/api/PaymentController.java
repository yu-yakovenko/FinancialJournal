package org.tonique.vocal.api;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.tonique.vocal.api.dto.CashPaymentRequest;
import org.tonique.vocal.api.dto.PaymentPatchRequest;
import org.tonique.vocal.api.dto.PaymentResolveRequest;
import org.tonique.vocal.api.dto.PaymentResponse;
import org.tonique.vocal.payment.PaymentService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/cash")
    public PaymentResponse addCash(@Valid @RequestBody CashPaymentRequest request) {
        return PaymentResponse.from(paymentService.addCash(
                request.studentId(),
                toKopiykas(request.amountUah()),
                request.paymentDate(),
                request.periodYear(),
                request.periodMonth(),
                request.comment()
        ));
    }

    @PatchMapping("/{id}")
    public PaymentResponse patch(@PathVariable Long id, @RequestBody PaymentPatchRequest request) {
        return PaymentResponse.from(
                paymentService.patch(id, request.studentId(), request.periodYear(), request.periodMonth())
        );
    }

    @GetMapping("/unmatched")
    public List<PaymentResponse> unmatched() {
        return paymentService.listUnmatched().stream().map(PaymentResponse::from).toList();
    }

    @PostMapping("/{id}/resolve")
    public PaymentResponse resolve(@PathVariable Long id, @Valid @RequestBody PaymentResolveRequest request) {
        return PaymentResponse.from(
                paymentService.resolve(id, request.studentId(), request.periodYear(), request.periodMonth())
        );
    }

    @PostMapping("/{id}/ignore")
    public PaymentResponse ignore(@PathVariable Long id) {
        return PaymentResponse.from(paymentService.ignore(id));
    }

    private long toKopiykas(BigDecimal amountUah) {
        return amountUah.setScale(2, RoundingMode.HALF_UP)
                .movePointRight(2)
                .longValueExact();
    }
}
