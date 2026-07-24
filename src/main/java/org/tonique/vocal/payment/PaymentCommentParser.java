package org.tonique.vocal.payment;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses the standardized-but-hand-typed payment comment
 * "Оплата за уроки вокалу, МІСЯЦЬ, ПІБ" into a declared month and payer name.
 * Tolerant of missing/extra separators and surrounding whitespace; anything that
 * doesn't fit the expected shape is left unparsed for manual review.
 */
public final class PaymentCommentParser {

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private static final Pattern COMMENT_PATTERN = Pattern.compile(
            "^оплата\\s+за\\s+уроки\\s+вокалу\\s*[,;-]?\\s*"
                    + "([А-ЯІЇЄҐа-яіїєґ']+)\\s*[,;-]?\\s*"
                    + "(.+)$",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    private PaymentCommentParser() {
    }

    public static Optional<ParsedComment> parse(String rawComment) {
        if (rawComment == null) {
            return Optional.empty();
        }
        String normalized = WHITESPACE.matcher(rawComment.trim()).replaceAll(" ");

        Matcher matcher = COMMENT_PATTERN.matcher(normalized);
        if (!matcher.matches()) {
            return Optional.empty();
        }

        String monthToken = matcher.group(1);
        String payerName = matcher.group(2).trim();
        if (payerName.isEmpty()) {
            return Optional.empty();
        }

        return UkrainianMonths.monthNumber(monthToken)
                .map(month -> new ParsedComment(month, payerName));
    }

    public record ParsedComment(int month, String payerName) {
    }
}
