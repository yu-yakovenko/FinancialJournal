package org.tonique.vocal.tariff;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TariffPlanRepository extends JpaRepository<TariffPlan, Long> {

    List<TariffPlan> findAllByOrderByLabel();
}
