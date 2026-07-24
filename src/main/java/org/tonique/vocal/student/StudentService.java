package org.tonique.vocal.student;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class StudentService {

    private final StudentRepository studentRepository;

    public StudentService(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    @Transactional(readOnly = true)
    public List<Student> findActive() {
        return studentRepository.findByActiveTrueOrderByFullName();
    }

    @Transactional(readOnly = true)
    public Student getOrThrow(Long id) {
        return studentRepository.findById(id).orElseThrow(() -> new StudentNotFoundException(id));
    }

    public Student create(String fullName, Tariff tariff) {
        return studentRepository.save(new Student(fullName, tariff));
    }

    public Student update(Long id, String fullName, Tariff tariff, boolean active) {
        Student student = getOrThrow(id);
        student.setFullName(fullName);
        student.setTariff(tariff);
        student.setActive(active);
        return studentRepository.save(student);
    }

    public void deactivate(Long id) {
        Student student = getOrThrow(id);
        student.setActive(false);
        studentRepository.save(student);
    }

    /**
     * Called from payment ingestion when a payment matches no known student but
     * parses to a name and a recognized tariff amount.
     */
    public Student createFromPayment(String fullName, Tariff tariff) {
        return create(fullName, tariff);
    }

    /**
     * A student's tariff is fixed by whichever comes first: a manual admin edit,
     * or the first matching payment. Once set, ingestion never overwrites it.
     */
    public void assignTariffIfAbsent(Student student, Tariff tariff) {
        if (student.getTariff() == null) {
            student.setTariff(tariff);
            studentRepository.save(student);
        }
    }
}
