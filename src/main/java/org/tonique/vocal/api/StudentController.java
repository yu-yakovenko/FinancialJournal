package org.tonique.vocal.api;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.tonique.vocal.api.dto.EnrollmentCreateRequest;
import org.tonique.vocal.api.dto.EnrollmentResponse;
import org.tonique.vocal.api.dto.StudentCreateRequest;
import org.tonique.vocal.api.dto.StudentResponse;
import org.tonique.vocal.api.dto.StudentUpdateRequest;
import org.tonique.vocal.enrollment.EnrollmentService;
import org.tonique.vocal.enrollment.TariffEnrollment;
import org.tonique.vocal.student.Student;
import org.tonique.vocal.student.StudentService;
import org.tonique.vocal.tariff.TariffPricingService;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/students")
public class StudentController {

    private final StudentService studentService;
    private final EnrollmentService enrollmentService;
    private final TariffPricingService tariffPricingService;

    public StudentController(StudentService studentService, EnrollmentService enrollmentService, TariffPricingService tariffPricingService) {
        this.studentService = studentService;
        this.enrollmentService = enrollmentService;
        this.tariffPricingService = tariffPricingService;
    }

    @GetMapping
    public List<StudentResponse> list() {
        return studentService.findActive().stream().map(this::toResponse).toList();
    }

    @PostMapping
    public StudentResponse create(@Valid @RequestBody StudentCreateRequest request) {
        return toResponse(studentService.create(request.fullName()));
    }

    @PutMapping("/{id}")
    public StudentResponse update(@PathVariable Long id, @Valid @RequestBody StudentUpdateRequest request) {
        return toResponse(studentService.update(id, request.fullName(), request.active()));
    }

    @DeleteMapping("/{id}")
    public void deactivate(@PathVariable Long id) {
        studentService.deactivate(id);
    }

    @GetMapping("/{id}/enrollments")
    public List<EnrollmentResponse> enrollments(@PathVariable Long id) {
        return enrollmentService.findByStudent(id).stream().map(this::toResponse).toList();
    }

    @PostMapping("/{id}/enrollments")
    public EnrollmentResponse addEnrollment(@PathVariable Long id, @Valid @RequestBody EnrollmentCreateRequest request) {
        Student student = studentService.getOrThrow(id);
        LocalDate validFrom = request.validFrom() != null ? request.validFrom() : LocalDate.now();
        TariffEnrollment enrollment = enrollmentService.ensureActive(student, tariffPricingService.getOrThrow(request.tariffPlanId()), validFrom);
        return toResponse(enrollment);
    }

    private StudentResponse toResponse(Student student) {
        List<String> activeTariffLabels = enrollmentService.findByStudent(student.getId()).stream()
                .filter(TariffEnrollment::isActive)
                .map(e -> e.getTariffPlan().getLabel())
                .toList();
        return new StudentResponse(
                student.getId(),
                student.getFullName(),
                activeTariffLabels,
                student.isActive(),
                student.getCreatedAt()
        );
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
