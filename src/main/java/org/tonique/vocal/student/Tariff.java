package org.tonique.vocal.student;

import java.util.Arrays;
import java.util.Optional;

public enum Tariff {

    INDIVIDUAL_500(ServiceType.INDIVIDUAL, 50_000),
    INDIVIDUAL_700(ServiceType.INDIVIDUAL, 70_000),
    CHOIR_VPO_680(ServiceType.CHOIR, 68_000),
    CHOIR_STANDARD_1700(ServiceType.CHOIR, 170_000);

    private final ServiceType serviceType;
    private final long amountKopiykas;

    Tariff(ServiceType serviceType, long amountKopiykas) {
        this.serviceType = serviceType;
        this.amountKopiykas = amountKopiykas;
    }

    public ServiceType serviceType() {
        return serviceType;
    }

    public long amountKopiykas() {
        return amountKopiykas;
    }

    public static Optional<Tariff> byAmountKopiykas(long amountKopiykas) {
        return Arrays.stream(values())
                .filter(tariff -> tariff.amountKopiykas == amountKopiykas)
                .findFirst();
    }

    public enum ServiceType {
        INDIVIDUAL,
        CHOIR
    }
}
