package org.tonique.vocal.tariff;

public class TariffRateNotFoundException extends RuntimeException {

    public TariffRateNotFoundException(Long id) {
        super("Запис ціни не знайдено: id=" + id);
    }
}
