package org.tonique.vocal.api;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.tonique.vocal.api.dto.EnrollmentEndRequest;
import org.tonique.vocal.api.dto.EnrollmentResponse;
import org.tonique.vocal.enrollment.EnrollmentService;
import org.tonique.vocal.enrollment.TariffEnrollment;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/enrollments")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    public EnrollmentController(EnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    @PostMapping("/{id}/end")
    public EnrollmentResponse end(@PathVariable Long id, @RequestBody(required = false) EnrollmentEndRequest request) {
        LocalDate validTo = (request != null && request.validTo() != null) ? request.validTo() : LocalDate.now();
        return toResponse(enrollmentService.end(id, validTo));
    }

    @PostMapping("/{id}/reactivate")
    public EnrollmentResponse reactivate(@PathVariable Long id) {
        return toResponse(enrollmentService.reactivate(id));
    }

    private EnrollmentResponse toResponse(TariffEnrollment enrollment) {
        return new EnrollmentResponse(
                enrollment.getId(),
                enrollment.getStudent().getId(),
                enrollment.getTariffPlan().getId(),
                enrollment.getTariffPlan().getLabel(),
                enrollment.getValidFrom(),
                enrollment.getValidTo(),
                enrollment.isActive()
        );
    }
}
