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
     */
    public TariffEnrollment ensureActive(Student student, TariffPlan tariffPlan, LocalDate validFrom) {
        return enrollmentRepository
                .findByStudentIdAndTariffPlanIdAndValidToIsNull(student.getId(), tariffPlan.getId())
                .orElseGet(() -> enrollmentRepository.save(new TariffEnrollment(student, tariffPlan, validFrom)));
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
