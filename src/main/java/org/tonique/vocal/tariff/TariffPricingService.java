package org.tonique.vocal.tariff;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class TariffPricingService {

    private final TariffPlanRepository tariffPlanRepository;
    private final TariffRateRepository tariffRateRepository;

    public TariffPricingService(TariffPlanRepository tariffPlanRepository, TariffRateRepository tariffRateRepository) {
        this.tariffPlanRepository = tariffPlanRepository;
        this.tariffRateRepository = tariffRateRepository;
    }

    @Transactional(readOnly = true)
    public List<TariffPlan> findAllPlans() {
        return tariffPlanRepository.findAllByOrderByLabel();
    }

    @Transactional(readOnly = true)
    public TariffPlan getOrThrow(Long id) {
        return tariffPlanRepository.findById(id).orElseThrow(() -> new TariffPlanNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public Optional<TariffPlan> findByLabel(String label) {
        return tariffPlanRepository.findByLabel(label);
    }

    @Transactional(readOnly = true)
    public List<TariffRate> ratesForPlan(Long planId) {
        return tariffRateRepository.findByTariffPlanIdOrderByEffectiveFromDesc(planId);
    }

    @Transactional(readOnly = true)
    public Optional<Long> currentAmountForPlan(TariffPlan plan) {
        return amountForPlanAt(plan, LocalDate.now());
    }

    @Transactional(readOnly = true)
    public Optional<Long> amountForPlanAt(TariffPlan plan, LocalDate asOf) {
        return TariffPricing.amountForPlanAt(tariffRateRepository.findAll(), plan, asOf);
    }

    @Transactional(readOnly = true)
    public TariffPricing.TariffMatchResult plansForAmountAt(long amountKopiykas, LocalDate asOf) {
        return TariffPricing.plansForAmountAt(tariffRateRepository.findAll(), amountKopiykas, asOf);
    }

    public TariffPlan createPlan(ServiceType serviceType, String label, long initialAmountKopiykas, LocalDate effectiveFrom) {
        TariffPlan plan = tariffPlanRepository.save(new TariffPlan(serviceType, label));
        tariffRateRepository.save(new TariffRate(plan, initialAmountKopiykas, effectiveFrom));
        return plan;
    }

    public TariffPlan updatePlan(Long id, String label, boolean active) {
        TariffPlan plan = getOrThrow(id);
        plan.setLabel(label);
        plan.setActive(active);
        return tariffPlanRepository.save(plan);
    }

    /** Records a price change as a new rate row — never mutates a past rate. */
    public TariffRate addRate(Long planId, long amountKopiykas, LocalDate effectiveFrom) {
        TariffPlan plan = getOrThrow(planId);
        return tariffRateRepository.save(new TariffRate(plan, amountKopiykas, effectiveFrom));
    }

    /**
     * Corrects a data-entry mistake on an already-created rate in place. Unlike
     * {@link #addRate}, this rewrites history — only use it to fix a wrong
     * amount/date typed in, never to record an actual price change.
     */
    public TariffRate updateRate(Long rateId, long amountKopiykas, LocalDate effectiveFrom) {
        TariffRate rate = tariffRateRepository.findById(rateId).orElseThrow(() -> new TariffRateNotFoundException(rateId));
        rate.setAmountKopiykas(amountKopiykas);
        rate.setEffectiveFrom(effectiveFrom);
        return tariffRateRepository.save(rate);
    }
}
