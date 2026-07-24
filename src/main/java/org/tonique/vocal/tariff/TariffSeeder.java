package org.tonique.vocal.tariff;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/** Seeds the four originally hard-coded tariffs on first boot, so existing behavior
 *  carries over once tariffs become admin-editable instead of an enum. */
@Component
public class TariffSeeder implements ApplicationRunner {

    private static final LocalDate BASELINE = LocalDate.of(2020, 1, 1);

    private final TariffPlanRepository tariffPlanRepository;
    private final TariffPricingService tariffPricingService;

    public TariffSeeder(TariffPlanRepository tariffPlanRepository, TariffPricingService tariffPricingService) {
        this.tariffPlanRepository = tariffPlanRepository;
        this.tariffPricingService = tariffPricingService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (tariffPlanRepository.count() > 0) {
            return;
        }
        tariffPricingService.createPlan(ServiceType.INDIVIDUAL, "Індивідуальні, базовий", 50_000, BASELINE);
        tariffPricingService.createPlan(ServiceType.INDIVIDUAL, "Індивідуальні, підвищений", 70_000, BASELINE);
        tariffPricingService.createPlan(ServiceType.CHOIR, "Хор, ВПО", 68_000, BASELINE);
        tariffPricingService.createPlan(ServiceType.CHOIR, "Хор, стандартний", 170_000, BASELINE);
    }
}
