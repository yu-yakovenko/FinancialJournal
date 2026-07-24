package org.tonique.vocal.payment;

import org.springframework.stereotype.Service;
import org.tonique.vocal.enrollment.EnrollmentService;
import org.tonique.vocal.monobank.StatementItem;
import org.tonique.vocal.student.NameMatcher;
import org.tonique.vocal.student.Student;
import org.tonique.vocal.student.StudentService;
import org.tonique.vocal.tariff.TariffPlan;
import org.tonique.vocal.tariff.TariffPricingService;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

/**
 * Turns raw Monobank statement items into Payment records: parses the declared
 * month/payer out of the comment, requires the amount to uniquely identify a single
 * tariff plan's price for the declared period (a student can be on several tariffs
 * at once, so an ambiguous or unrecognized amount can never be safely guessed),
 * matches the payer against the student roster (or auto-creates a student), and
 * ensures an active enrollment links the student to that tariff. Anything that
 * can't be resolved deterministically is left NEEDS_REVIEW rather than guessed.
 */
@Service
public class PaymentIngestionService {

    private final PaymentRepository paymentRepository;
    private final StudentService studentService;
    private final TariffPricingService tariffPricingService;
    private final EnrollmentService enrollmentService;

    public PaymentIngestionService(
            PaymentRepository paymentRepository,
            StudentService studentService,
            TariffPricingService tariffPricingService,
            EnrollmentService enrollmentService
    ) {
        this.paymentRepository = paymentRepository;
        this.studentService = studentService;
        this.tariffPricingService = tariffPricingService;
        this.enrollmentService = enrollmentService;
    }

    public IngestionResult ingest(List<StatementItem> items, LocalDate statementDate) {
        int matched = 0;
        int needsReview = 0;
        int skipped = 0;

        for (StatementItem item : items) {
            try {
                switch (ingestOne(item, statementDate)) {
                    case MATCHED -> matched++;
                    case NEEDS_REVIEW -> needsReview++;
                    case SKIPPED -> skipped++;
                }
            } catch (Exception exception) {
                needsReview++;
            }
        }
        return new IngestionResult(matched, needsReview, skipped);
    }

    private Result ingestOne(StatementItem item, LocalDate statementDate) {
        if (item.amount() <= 0) {
            // outgoing transactions / fees on the account, not tuition income
            return Result.SKIPPED;
        }
        if (paymentRepository.existsByMonobankTransactionId(item.id())) {
            return Result.SKIPPED;
        }

        Payment payment = Payment.bank(item.id(), item.amount(), statementDate, item.comment());

        Optional<PaymentCommentParser.ParsedComment> parsed = PaymentCommentParser.parse(item.comment());
        if (parsed.isEmpty()) {
            paymentRepository.save(payment);
            return Result.NEEDS_REVIEW;
        }

        String payerName = parsed.get().payerName();
        int declaredMonth = parsed.get().month();
        payment.setParsedPayerName(payerName);

        YearMonth period = resolvePeriod(declaredMonth, statementDate);
        payment.setPeriodYear(period.getYear());
        payment.setPeriodMonth(period.getMonthValue());

        // The amount must uniquely identify one tariff plan's price for this period.
        // A partial payment, or one whose amount happens to fit more than one
        // tariff, can't be safely assigned automatically — a student may be on
        // several tariffs, so there's no single "expected" amount to fall back on.
        Optional<TariffPlan> tariffPlan =
                tariffPricingService.plansForAmountAt(item.amount(), period.atDay(1)).unique();
        if (tariffPlan.isEmpty()) {
            paymentRepository.save(payment);
            return Result.NEEDS_REVIEW;
        }

        List<Student> roster = studentService.findActive();
        NameMatcher.MatchResult matchResult = NameMatcher.match(payerName, roster);
        Optional<Student> uniqueMatch = matchResult.unique();

        Student student;
        if (uniqueMatch.isPresent()) {
            student = uniqueMatch.get();
        } else if (matchResult.candidates().isEmpty()) {
            student = studentService.createFromPayment(payerName);
        } else {
            // ambiguous name match among several roster candidates
            paymentRepository.save(payment);
            return Result.NEEDS_REVIEW;
        }

        enrollmentService.ensureActive(student, tariffPlan.get(), period.atDay(1));
        payment.setStudent(student);
        payment.setTariffPlan(tariffPlan.get());
        payment.setMatchStatus(PaymentMatchStatus.MATCHED);
        paymentRepository.save(payment);
        return Result.MATCHED;
    }

    /**
     * The comment never states a year. Default to the transaction's year; if the
     * declared month is far from the transaction month (paying in January for
     * December, or a rare advance payment), shift a year to keep the guess sane.
     * Admins can still correct a wrong guess via PATCH /api/payments/{id}.
     */
    private YearMonth resolvePeriod(int declaredMonth, LocalDate statementDate) {
        int transactionMonth = statementDate.getMonthValue();
        int year = statementDate.getYear();
        int diff = declaredMonth - transactionMonth;
        if (diff > 6) {
            year -= 1;
        } else if (diff < -6) {
            year += 1;
        }
        return YearMonth.of(year, declaredMonth);
    }

    private enum Result {
        MATCHED,
        NEEDS_REVIEW,
        SKIPPED
    }

    public record IngestionResult(int matched, int needsReview, int skipped) {
    }
}
