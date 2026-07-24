package org.tonique.vocal.enrollment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TariffEnrollmentRepository extends JpaRepository<TariffEnrollment, Long> {

    List<TariffEnrollment> findByStudentIdOrderByValidFrom(Long studentId);

    List<TariffEnrollment> findByStudentIdIn(List<Long> studentIds);

    Optional<TariffEnrollment> findByStudentIdAndTariffPlanIdAndValidToIsNull(Long studentId, Long tariffPlanId);
}
