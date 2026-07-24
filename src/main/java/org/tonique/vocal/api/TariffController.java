package org.tonique.vocal.api;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.tonique.vocal.api.dto.TariffPlanCreateRequest;
import org.tonique.vocal.api.dto.TariffPlanResponse;
import org.tonique.vocal.api.dto.TariffPlanUpdateRequest;
import org.tonique.vocal.api.dto.TariffRateCreateRequest;
import org.tonique.vocal.api.dto.TariffRateResponse;
import org.tonique.vocal.tariff.TariffPlan;
import org.tonique.vocal.tariff.TariffPricingService;
import org.tonique.vocal.tariff.TariffRate;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/tariffs")
public class TariffController {

    private final TariffPricingService tariffPricingService;

    public TariffController(TariffPricingService tariffPricingService) {
        this.tariffPricingService = tariffPricingService;
    }

    @GetMapping
    public List<TariffPlanResponse> list() {
        return tariffPricingService.findAllPlans().stream().map(this::toResponse).toList();
    }

    @PostMapping
    public TariffPlanResponse create(@Valid @RequestBody TariffPlanCreateRequest request) {
        TariffPlan plan = tariffPricingService.createPlan(
                request.serviceType(),
                request.label(),
                MoneyConversion.toKopiykas(request.initialAmountUah()),
                request.effectiveFrom() != null ? request.effectiveFrom() : LocalDate.now()
        );
        return toResponse(plan);
    }

    @PutMapping("/{id}")
    public TariffPlanResponse update(@PathVariable Long id, @Valid @RequestBody TariffPlanUpdateRequest request) {
        return toResponse(tariffPricingService.updatePlan(id, request.label(), request.active()));
    }

    @GetMapping("/{id}/rates")
    public List<TariffRateResponse> rates(@PathVariable Long id) {
        return tariffPricingService.ratesForPlan(id).stream()
                .map(rate -> new TariffRateResponse(rate.getId(), rate.getAmountKopiykas(), rate.getEffectiveFrom()))
                .toList();
    }

    @PostMapping("/{id}/rates")
    public TariffRateResponse addRate(@PathVariable Long id, @Valid @RequestBody TariffRateCreateRequest request) {
        TariffRate rate = tariffPricingService.addRate(
                id,
                MoneyConversion.toKopiykas(request.amountUah()),
                request.effectiveFrom() != null ? request.effectiveFrom() : LocalDate.now()
        );
        return new TariffRateResponse(rate.getId(), rate.getAmountKopiykas(), rate.getEffectiveFrom());
    }

    private TariffPlanResponse toResponse(TariffPlan plan) {
        Long currentAmount = tariffPricingService.currentAmountForPlan(plan).orElse(null);
        return new TariffPlanResponse(plan.getId(), plan.getServiceType(), plan.getLabel(), plan.isActive(), currentAmount);
    }
}
