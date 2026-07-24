package org.tonique.vocal.student;

public class StudentNotFoundException extends RuntimeException {

    public StudentNotFoundException(Long id) {
        super("Студента не знайдено: id=" + id);
    }
}
