package org.tonique.vocal.tariff;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;

/**
 * One price point in a {@link TariffPlan}'s history. A price change is recorded as
 * a NEW row (never edits an existing one), so past periods keep evaluating against
 * whatever price was actually in effect at the time.
 */
@Entity
@Table(name = "tariff_rates")
public class TariffRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "tariff_plan_id", nullable = false)
    private TariffPlan tariffPlan;

    @Column(nullable = false)
    private long amountKopiykas;

    @Column(nullable = false)
    private LocalDate effectiveFrom;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected TariffRate() {
    }

    public TariffRate(TariffPlan tariffPlan, long amountKopiykas, LocalDate effectiveFrom) {
        this.tariffPlan = tariffPlan;
        this.amountKopiykas = amountKopiykas;
        this.effectiveFrom = effectiveFrom;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public TariffPlan getTariffPlan() {
        return tariffPlan;
    }

    public long getAmountKopiykas() {
        return amountKopiykas;
    }

    public LocalDate getEffectiveFrom() {
        return effectiveFrom;
    }

}
