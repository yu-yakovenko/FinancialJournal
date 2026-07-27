package org.tonique.vocal.enrollment;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tonique.vocal.student.Student;
import org.tonique.vocal.tariff.TariffPlan;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class EnrollmentService {

    private final TariffEnrollmentRepository enrollmentRepository;

    public EnrollmentService(TariffEnrollmentRepository enrollmentRepository) {
        this.enrollmentRepository = enrollmentRepository;
    }

    @Transactional(readOnly = true)
    public List<TariffEnrollment> findByStudent(Long studentId) {
        return enrollmentRepository.findByStudentIdOrderByValidFrom(studentId);
    }

    @Transactional(readOnly = true)
    public List<TariffEnrollment> findForStudents(List<Long> studentIds) {
        return enrollmentRepository.findByStudentIdIn(studentIds);
    }

    /**
     * Reuses the currently-active enrollment for this (student, tariff) pair if one
     * exists; otherwise starts a new one from validFrom. Used both by automatic
     * ingestion and by the admin "add tariff to student" action, so a payment or a
     * manual pick never creates a duplicate active row for a tariff the student is
     * already on.
     * <p>
     * If a payment for an earlier period shows up later (e.g. a historical backfill
     * run after the enrollment already started from a more recent payment),
     * validFrom is pulled backward to that earlier date — otherwise the backfilled
     * month would fall outside [validFrom, validTo] and silently disappear from the
     * journal grid despite the payment being correctly matched.
     */
    public TariffEnrollment ensureActive(Student student, TariffPlan tariffPlan, LocalDate validFrom) {
        return enrollmentRepository
                .findByStudentIdAndTariffPlanIdAndValidToIsNull(student.getId(), tariffPlan.getId())
                .map(enrollment -> extendBackwardIfNeeded(enrollment, validFrom))
                .orElseGet(() -> enrollmentRepository.save(new TariffEnrollment(student, tariffPlan, validFrom)));
    }

    private TariffEnrollment extendBackwardIfNeeded(TariffEnrollment enrollment, LocalDate validFrom) {
        if (validFrom.isBefore(enrollment.getValidFrom())) {
            enrollment.setValidFrom(validFrom);
            return enrollmentRepository.save(enrollment);
        }
        return enrollment;
    }

    public TariffEnrollment end(Long id, LocalDate validTo) {
        TariffEnrollment enrollment = getOrThrow(id);
        enrollment.setValidTo(validTo);
        return enrollmentRepository.save(enrollment);
    }

    public TariffEnrollment reactivate(Long id) {
        TariffEnrollment enrollment = getOrThrow(id);
        enrollment.setValidTo(null);
        return enrollmentRepository.save(enrollment);
    }

    private TariffEnrollment getOrThrow(Long id) {
        return enrollmentRepository.findById(id).orElseThrow(() -> new EnrollmentNotFoundException(id));
    }
}
