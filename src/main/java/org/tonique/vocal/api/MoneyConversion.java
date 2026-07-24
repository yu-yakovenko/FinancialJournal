package org.tonique.vocal.api;

import java.math.BigDecimal;
import java.math.RoundingMode;

final class MoneyConversion {

    private MoneyConversion() {
    }

    static long toKopiykas(BigDecimal amountUah) {
        return amountUah.setScale(2, RoundingMode.HALF_UP)
                .movePointRight(2)
                .longValueExact();
    }
}
