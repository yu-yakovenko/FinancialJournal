package org.tonique.vocal.payment;

import org.springframework.stereotype.Service;
import org.tonique.vocal.monobank.StatementItem;
import org.tonique.vocal.student.NameMatcher;
import org.tonique.vocal.student.Student;
import org.tonique.vocal.student.StudentService;
import org.tonique.vocal.student.Tariff;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

/**
 * Turns raw Monobank statement items into Payment records: parses the declared
 * month/payer out of the comment, matches the payer against the student roster
 * (or auto-creates a student when the amount matches a known tariff), and assigns
 * a student's tariff from their first matching payment if it isn't already set.
 * Anything that can't be resolved deterministically is left NEEDS_REVIEW rather
 * than guessed.
 */
@Service
public class PaymentIngestionService {

    private final PaymentRepository paymentRepository;
    private final StudentService studentService;

    public PaymentIngestionService(PaymentRepository paymentRepository, StudentService studentService) {
        this.paymentRepository = paymentRepository;
        this.studentService = studentService;
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

        Optional<Tariff> tariff = Tariff.byAmountKopiykas(item.amount());

        List<Student> roster = studentService.findActive();
        NameMatcher.MatchResult matchResult = NameMatcher.match(payerName, roster);
        Optional<Student> uniqueMatch = matchResult.unique();

        if (uniqueMatch.isPresent()) {
            Student student = uniqueMatch.get();
            payment.setStudent(student);
            tariff.ifPresent(t -> studentService.assignTariffIfAbsent(student, t));
            payment.setMatchStatus(PaymentMatchStatus.MATCHED);
            paymentRepository.save(payment);
            return Result.MATCHED;
        }

        if (matchResult.candidates().isEmpty() && tariff.isPresent()) {
            Student created = studentService.createFromPayment(payerName, tariff.get());
            payment.setStudent(created);
            payment.setMatchStatus(PaymentMatchStatus.MATCHED);
            paymentRepository.save(payment);
            return Result.MATCHED;
        }

        // Ambiguous match (several roster candidates) or an unmatched name paired
        // with an amount that doesn't correspond to any known tariff.
        paymentRepository.save(payment);
        return Result.NEEDS_REVIEW;
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
