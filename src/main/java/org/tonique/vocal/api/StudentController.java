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
import org.tonique.vocal.api.dto.StudentCreateRequest;
import org.tonique.vocal.api.dto.StudentResponse;
import org.tonique.vocal.api.dto.StudentUpdateRequest;
import org.tonique.vocal.student.StudentService;

import java.util.List;

@RestController
@RequestMapping("/api/students")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @GetMapping
    public List<StudentResponse> list() {
        return studentService.findActive().stream().map(StudentResponse::from).toList();
    }

    @PostMapping
    public StudentResponse create(@Valid @RequestBody StudentCreateRequest request) {
        return StudentResponse.from(studentService.create(request.fullName(), request.tariff()));
    }

    @PutMapping("/{id}")
    public StudentResponse update(@PathVariable Long id, @Valid @RequestBody StudentUpdateRequest request) {
        return StudentResponse.from(studentService.update(id, request.fullName(), request.tariff(), request.active()));
    }

    @DeleteMapping("/{id}")
    public void deactivate(@PathVariable Long id) {
        studentService.deactivate(id);
    }
}
