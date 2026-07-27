package org.tonique.vocal.tariff;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TariffPlanRepository extends JpaRepository<TariffPlan, Long> {

    List<TariffPlan> findAllByOrderByLabel();

    Optional<TariffPlan> findByLabel(String label);
}
