package org.tonique.vocal.tariff;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TariffPricingTest {

    private final TariffPlan choirStandard = new TariffPlan(ServiceType.CHOIR, "Хор, стандартний");
    private final TariffPlan choirVpo = new TariffPlan(ServiceType.CHOIR, "Хор, ВПО");

    @Test
    void priceChangeDoesNotAffectPastPeriods() {
        List<TariffRate> rates = List.of(
                new TariffRate(choirStandard, 170_000, LocalDate.of(2020, 1, 1)),
                new TariffRate(choirStandard, 180_000, LocalDate.of(2027, 1, 1))
        );

        // December 2026 was still priced at the old rate...
        assertThat(TariffPricing.amountForPlanAt(rates, choirStandard, LocalDate.of(2026, 12, 1)))
                .contains(170_000L);
        // ...and January 2027 onward reflects the new one.
        assertThat(TariffPricing.amountForPlanAt(rates, choirStandard, LocalDate.of(2027, 1, 1)))
                .contains(180_000L);
    }

    @Test
    void matchesOldPriceForAnOldPaymentEvenAfterALaterPriceChange() {
        List<TariffRate> rates = List.of(
                new TariffRate(choirStandard, 170_000, LocalDate.of(2020, 1, 1)),
                new TariffRate(choirStandard, 180_000, LocalDate.of(2027, 1, 1))
        );

        TariffPricing.TariffMatchResult result = TariffPricing.plansForAmountAt(rates, 170_000, LocalDate.of(2026, 8, 1));

        assertThat(result.unique()).contains(choirStandard);
    }

    @Test
    void noRateYetActiveReturnsEmpty() {
        List<TariffRate> rates = List.of(
                new TariffRate(choirStandard, 170_000, LocalDate.of(2027, 1, 1))
        );

        assertThat(TariffPricing.amountForPlanAt(rates, choirStandard, LocalDate.of(2026, 1, 1))).isEmpty();
        assertThat(TariffPricing.plansForAmountAt(rates, 170_000, LocalDate.of(2026, 1, 1)).candidates()).isEmpty();
    }

    @Test
    void ambiguousWhenTwoPlansShareTheSameCurrentPrice() {
        List<TariffRate> rates = List.of(
                new TariffRate(choirStandard, 170_000, LocalDate.of(2020, 1, 1)),
                new TariffRate(choirVpo, 170_000, LocalDate.of(2020, 1, 1))
        );

        TariffPricing.TariffMatchResult result = TariffPricing.plansForAmountAt(rates, 170_000, LocalDate.of(2026, 8, 1));

        assertThat(result.unique()).isEmpty();
        assertThat(result.candidates()).containsExactlyInAnyOrder(choirStandard, choirVpo);
    }

    @Test
    void unrelatedAmountMatchesNoPlan() {
        List<TariffRate> rates = List.of(
                new TariffRate(choirStandard, 170_000, LocalDate.of(2020, 1, 1))
        );

        assertThat(TariffPricing.plansForAmountAt(rates, 999_900, LocalDate.of(2026, 8, 1)).candidates()).isEmpty();
    }
}
