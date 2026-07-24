package org.tonique.vocal.tariff;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TariffRateRepository extends JpaRepository<TariffRate, Long> {

    List<TariffRate> findByTariffPlanIdOrderByEffectiveFromDesc(Long tariffPlanId);
}
