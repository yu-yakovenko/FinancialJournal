package org.tonique.vocal.tariff;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Date-aware tariff price lookups. Pure logic, no persistence: given the full set of
 * rates, works out which rate was actually in effect for a plan on a given date
 * (the latest rate with effectiveFrom &lt;= that date), so a later price change never
 * retroactively changes how past periods are evaluated.
 */
public final class TariffPricing {

    private TariffPricing() {
    }

    public static TariffMatchResult plansForAmountAt(List<TariffRate> allRates, long amountKopiykas, LocalDate asOf) {
        List<TariffPlan> candidates = new ArrayList<>();
        for (Map.Entry<TariffPlan, TariffRate> entry : currentRatesAt(allRates, asOf).entrySet()) {
            if (entry.getValue().getAmountKopiykas() == amountKopiykas) {
                candidates.add(entry.getKey());
            }
        }
        return new TariffMatchResult(candidates);
    }

    public static Optional<Long> amountForPlanAt(List<TariffRate> allRates, TariffPlan plan, LocalDate asOf) {
        return Optional.ofNullable(currentRatesAt(allRates, asOf).get(plan))
                .map(TariffRate::getAmountKopiykas);
    }

    private static Map<TariffPlan, TariffRate> currentRatesAt(List<TariffRate> allRates, LocalDate asOf) {
        Map<TariffPlan, TariffRate> latestByPlan = new HashMap<>();
        for (TariffRate rate : allRates) {
            if (rate.getEffectiveFrom().isAfter(asOf)) {
                continue;
            }
            TariffRate current = latestByPlan.get(rate.getTariffPlan());
            if (current == null || rate.getEffectiveFrom().isAfter(current.getEffectiveFrom())) {
                latestByPlan.put(rate.getTariffPlan(), rate);
            }
        }
        return latestByPlan;
    }

    public record TariffMatchResult(List<TariffPlan> candidates) {

        public Optional<TariffPlan> unique() {
            return candidates.size() == 1 ? Optional.of(candidates.getFirst()) : Optional.empty();
        }
    }
}
