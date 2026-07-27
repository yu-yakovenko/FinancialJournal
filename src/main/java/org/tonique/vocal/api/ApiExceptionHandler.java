package org.tonique.vocal.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.tonique.vocal.enrollment.EnrollmentNotFoundException;
import org.tonique.vocal.payment.PaymentNotFoundException;
import org.tonique.vocal.student.StudentNotFoundException;
import org.tonique.vocal.tariff.TariffPlanNotFoundException;
import org.tonique.vocal.tariff.TariffRateNotFoundException;

import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler({
            StudentNotFoundException.class,
            PaymentNotFoundException.class,
            TariffPlanNotFoundException.class,
            TariffRateNotFoundException.class,
            EnrollmentNotFoundException.class
    })
    public ResponseEntity<Map<String, String>> handleNotFound(RuntimeException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler({IllegalArgumentException.class, ArithmeticException.class})
    public ResponseEntity<Map<String, String>> handleBadRequest(RuntimeException exception) {
        return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
    }
}
