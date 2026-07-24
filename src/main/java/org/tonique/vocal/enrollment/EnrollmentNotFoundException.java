package org.tonique.vocal.enrollment;

public class EnrollmentNotFoundException extends RuntimeException {

    public EnrollmentNotFoundException(Long id) {
        super("Зарахування не знайдено: id=" + id);
    }
}
