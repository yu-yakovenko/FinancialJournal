package org.tonique.vocal.payment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "student_id")
    private Student student;

    /** Which tariff this payment counts toward — set only once matching succeeds
     *  (a student can be on several tariffs at once, so this can't be inferred from
     *  the student alone). */
    @ManyToOne
    @JoinColumn(name = "tariff_plan_id")
    private TariffPlan tariffPlan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentSource source;

    /** Idempotency key for the daily bank import; null for CASH payments. */
    @Column(unique = true)
    private String monobankTransactionId;

    @Column(nullable = false)
    private long amountKopiykas;

    @Column(nullable = false)
    private LocalDate paymentDate;

    /** The month/year the payment is declared FOR (parsed from the comment), not paymentDate. */
    private Integer periodYear;

    private Integer periodMonth;

    @Column(length = 1000)
    private String rawComment;

    private String parsedPayerName;

    /** Monobank's own counterparty name on the statement line ("Відправник"), independent of parsedPayerName. */
    private String senderName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMatchStatus matchStatus;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected Payment() {
    }

    public static Payment bank(String monobankTransactionId, long amountKopiykas, LocalDate paymentDate, String rawComment,
                                String senderName) {
        Payment payment = new Payment();
        payment.source = PaymentSource.BANK;
        payment.monobankTransactionId = monobankTransactionId;
        payment.amountKopiykas = amountKopiykas;
        payment.paymentDate = paymentDate;
        payment.rawComment = rawComment;
        payment.senderName = senderName;
        payment.matchStatus = PaymentMatchStatus.NEEDS_REVIEW;
        return payment;
    }

    public static Payment cash(Student student, TariffPlan tariffPlan, long amountKopiykas, LocalDate paymentDate,
                                int periodYear, int periodMonth, String comment) {
        Payment payment = new Payment();
        payment.source = PaymentSource.CASH;
        payment.student = student;
        payment.tariffPlan = tariffPlan;
        payment.amountKopiykas = amountKopiykas;
        payment.paymentDate = paymentDate;
        payment.periodYear = periodYear;
        payment.periodMonth = periodMonth;
        payment.rawComment = comment;
        payment.matchStatus = PaymentMatchStatus.MATCHED;
        return payment;
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

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }

    public TariffPlan getTariffPlan() {
        return tariffPlan;
    }

    public void setTariffPlan(TariffPlan tariffPlan) {
        this.tariffPlan = tariffPlan;
    }

    public PaymentSource getSource() {
        return source;
    }

    public String getMonobankTransactionId() {
        return monobankTransactionId;
    }

    public long getAmountKopiykas() {
        return amountKopiykas;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public Integer getPeriodYear() {
        return periodYear;
    }

    public void setPeriodYear(Integer periodYear) {
        this.periodYear = periodYear;
    }

    public Integer getPeriodMonth() {
        return periodMonth;
    }

    public void setPeriodMonth(Integer periodMonth) {
        this.periodMonth = periodMonth;
    }

    public String getRawComment() {
        return rawComment;
    }

    public String getParsedPayerName() {
        return parsedPayerName;
    }

    public void setParsedPayerName(String parsedPayerName) {
        this.parsedPayerName = parsedPayerName;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public PaymentMatchStatus getMatchStatus() {
        return matchStatus;
    }

    public void setMatchStatus(PaymentMatchStatus matchStatus) {
        this.matchStatus = matchStatus;
    }

}
