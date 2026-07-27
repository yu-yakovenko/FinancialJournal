package org.tonique.vocal.student;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {

    List<Student> findByActiveTrueOrderByFullName();

    Optional<Student> findByFullName(String fullName);
}
