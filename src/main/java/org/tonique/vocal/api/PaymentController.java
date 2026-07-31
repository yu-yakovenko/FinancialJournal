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
                request.tariffPlanId(),
                MoneyConversion.toKopiykas(request.amountUah()),
                request.paymentDate(),
                request.periodYear(),
                request.periodMonth(),
                request.comment()
        ));
    }

    @PatchMapping("/{id}")
    public PaymentResponse patch(@PathVariable Long id, @RequestBody PaymentPatchRequest request) {
        return PaymentResponse.from(
                paymentService.patch(id, request.studentId(), request.tariffPlanId(), request.periodYear(),
                        request.periodMonth(), request.matchStatus())
        );
    }

    @GetMapping
    public List<PaymentResponse> all() {
        return paymentService.listAll().stream().map(PaymentResponse::from).toList();
    }

    @GetMapping("/unmatched")
    public List<PaymentResponse> unmatched() {
        return paymentService.listUnmatched().stream().map(PaymentResponse::from).toList();
    }

    @PostMapping("/{id}/resolve")
    public PaymentResponse resolve(@PathVariable Long id, @Valid @RequestBody PaymentResolveRequest request) {
        return PaymentResponse.from(
                paymentService.resolve(id, request.studentId(), request.tariffPlanId(), request.periodYear(), request.periodMonth())
        );
    }

    @PostMapping("/{id}/ignore")
    public PaymentResponse ignore(@PathVariable Long id) {
        return PaymentResponse.from(paymentService.ignore(id));
    }
}
