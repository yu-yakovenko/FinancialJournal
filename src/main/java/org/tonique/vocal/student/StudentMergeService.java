package org.tonique.vocal.student;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tonique.vocal.enrollment.TariffEnrollment;
import org.tonique.vocal.enrollment.TariffEnrollmentRepository;
import org.tonique.vocal.payment.Payment;
import org.tonique.vocal.payment.PaymentRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Merges duplicate students created when a hand-typed payment comment doesn't match
 * any existing roster entry exactly (surname typed alone, patronymic dropped, etc. —
 * see {@link NameMatcher}). Merging reassigns every {@link Payment} and
 * {@link TariffEnrollment} from the duplicates onto the surviving student, then deletes
 * the duplicates — safe because neither entity cascades from {@code Student} and both
 * FKs get explicitly repointed first.
 */
@Service
@Transactional
public class StudentMergeService {

    private final StudentService studentService;
    private final StudentRepository studentRepository;
    private final PaymentRepository paymentRepository;
    private final TariffEnrollmentRepository enrollmentRepository;

    public StudentMergeService(StudentService studentService, StudentRepository studentRepository,
                                PaymentRepository paymentRepository, TariffEnrollmentRepository enrollmentRepository) {
        this.studentService = studentService;
        this.studentRepository = studentRepository;
        this.paymentRepository = paymentRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    @Transactional(readOnly = true)
    public List<List<Student>> findDuplicateGroups() {
        return NameMatcher.findDuplicateGroups(studentService.findActive());
    }

    @Transactional(readOnly = true)
    public Optional<Payment> lastPayment(Long studentId) {
        return paymentRepository.findFirstByStudentIdOrderByPaymentDateDesc(studentId);
    }

    /**
     * The student a group should merge into: the one with the fullest name (longest
     * once normalized), tie-broken by whoever's been on the roster longest.
     */
    public Student pickTarget(List<Student> group) {
        return group.stream()
                .sorted(Comparator
                        .comparingInt((Student s) -> NameMatcher.normalize(s.getFullName()).length())
                        .reversed()
                        .thenComparing(Student::getCreatedAt))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Група для об'єднання не може бути порожньою"));
    }

    /** Merges every checked student in {@code studentIds}, auto-picking the survivor. */
    public Student mergeGroup(List<Long> studentIds) {
        return mergeGroup(studentIds, null);
    }

    /**
     * Merges every student in {@code studentIds}. If {@code explicitTargetId} is given
     * (e.g. the user picked "merge into this one" on the student's own edit form), that
     * student survives regardless of name length; otherwise the survivor is auto-picked.
     */
    public Student mergeGroup(List<Long> studentIds, Long explicitTargetId) {
        List<Long> distinctIds = studentIds.stream().distinct().toList();
        if (distinctIds.size() < 2) {
            throw new IllegalArgumentException("Потрібно обрати щонайменше двох студентів для об'єднання");
        }
        List<Student> students = distinctIds.stream().map(studentService::getOrThrow).toList();
        Student target = explicitTargetId != null
                ? students.stream()
                        .filter(s -> s.getId().equals(explicitTargetId))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Основний студент має бути серед обраних"))
                : pickTarget(students);
        List<Long> sourceIds = distinctIds.stream().filter(id -> !id.equals(target.getId())).toList();
        return merge(target.getId(), sourceIds);
    }

    /**
     * Merges every duplicate-detection group that's a clique (every member matches every
     * other member directly) — a group only connected transitively through a shared
     * surname-only entry is left alone, since that can chain together two different
     * people (e.g. siblings) and needs a human to pick which is right.
     */
    public MergeSummary mergeAutomatically() {
        int mergedGroups = 0;
        int mergedStudents = 0;
        for (List<Student> group : findDuplicateGroups()) {
            if (!NameMatcher.isClique(group)) {
                continue;
            }
            Student target = pickTarget(group);
            List<Long> sourceIds = group.stream().map(Student::getId).filter(id -> !id.equals(target.getId())).toList();
            merge(target.getId(), sourceIds);
            mergedGroups++;
            mergedStudents += sourceIds.size();
        }
        return new MergeSummary(mergedGroups, mergedStudents);
    }

    private Student merge(Long targetId, List<Long> sourceIds) {
        Student target = studentService.getOrThrow(targetId);
        for (Long sourceId : sourceIds) {
            if (sourceId.equals(targetId)) {
                continue;
            }
            Student source = studentService.getOrThrow(sourceId);
            reassignPayments(source, target);
            reassignEnrollments(source, target);
            studentRepository.delete(source);
        }
        return target;
    }

    private void reassignPayments(Student source, Student target) {
        List<Payment> payments = paymentRepository.findByStudentId(source.getId());
        payments.forEach(payment -> payment.setStudent(target));
        paymentRepository.saveAll(payments);
    }

    private void reassignEnrollments(Student source, Student target) {
        for (TariffEnrollment enrollment : enrollmentRepository.findByStudentIdOrderByValidFrom(source.getId())) {
            boolean targetAlreadyHasActiveEnrollment = enrollment.isActive()
                    && enrollmentRepository
                            .findByStudentIdAndTariffPlanIdAndValidToIsNull(target.getId(), enrollment.getTariffPlan().getId())
                            .isPresent();
            if (targetAlreadyHasActiveEnrollment) {
                // target is already actively enrolled in this tariff — the duplicate's row is redundant
                enrollmentRepository.delete(enrollment);
            } else {
                enrollment.setStudent(target);
                enrollmentRepository.save(enrollment);
            }
        }
    }

    public record MergeSummary(int mergedGroups, int mergedStudents) {
    }
}
