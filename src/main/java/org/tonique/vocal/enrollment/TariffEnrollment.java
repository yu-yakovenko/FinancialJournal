package org.tonique.vocal.enrollment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import org.tonique.vocal.student.Student;
import org.tonique.vocal.tariff.TariffPlan;

import java.time.Instant;
import java.time.LocalDate;

/**
 * A student's enrollment in one tariff plan for a stretch of time — a student can
 * have several of these at once (e.g. choir AND individual lessons), each tracked
 * independently. validTo == null means the enrollment is currently active.
 */
@Entity
@Table(name = "tariff_enrollments")
public class TariffEnrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne
    @JoinColumn(name = "tariff_plan_id", nullable = false)
    private TariffPlan tariffPlan;

    @Column(nullable = false)
    private LocalDate validFrom;

    private LocalDate validTo;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected TariffEnrollment() {
    }

    public TariffEnrollment(Student student, TariffPlan tariffPlan, LocalDate validFrom) {
        this.student = student;
        this.tariffPlan = tariffPlan;
        this.validFrom = validFrom;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public boolean isActive() {
        return validTo == null;
    }

    public boolean overlaps(int year) {
        if (validFrom.getYear() > year) {
            return false;
        }
        return validTo == null || !validTo.isBefore(LocalDate.of(year, 1, 1));
    }

    public Long getId() {
        return id;
    }

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }

    public TariffPlan getTariffPlan() {
        return tariffPlan;
    }

    public LocalDate getValidFrom() {
        return validFrom;
    }

    public LocalDate getValidTo() {
        return validTo;
    }

    public void setValidTo(LocalDate validTo) {
        this.validTo = validTo;
    }

}
