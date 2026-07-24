package org.tonique.vocal.tariff;

public class TariffPlanNotFoundException extends RuntimeException {

    public TariffPlanNotFoundException(Long id) {
        super("Тариф не знайдено: id=" + id);
    }
}
