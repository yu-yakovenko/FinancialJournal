package org.tonique.vocal.student;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

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
    public Optional<Student> findByFullName(String fullName) {
        return studentRepository.findByFullName(fullName);
    }

    @Transactional(readOnly = true)
    public Student getOrThrow(Long id) {
        return studentRepository.findById(id).orElseThrow(() -> new StudentNotFoundException(id));
    }

    public Student create(String fullName) {
        return studentRepository.save(new Student(fullName));
    }

    public Student update(Long id, String fullName, boolean active) {
        Student student = getOrThrow(id);
        student.setFullName(fullName);
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
    public Student createFromPayment(String fullName) {
        return create(fullName);
    }
}
