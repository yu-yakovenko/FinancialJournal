package org.tonique.vocal.payment;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses the standardized-but-hand-typed payment comment
 * "Оплата за уроки вокалу, МІСЯЦЬ[, РІК], ПІБ" into a declared month, an optional
 * declared year, and a payer name. Accepts "Сплата" as well as "Оплата" since some
 * payers use that spelling. Tolerant of missing/extra separators and
 * surrounding whitespace, and of stray Latin letters that are visually identical
 * to their Cyrillic counterparts (a phone left on the wrong keyboard layout).
 * A payer catching up on several months at once may write a range like
 * "червень-серпень" instead of a single month; this is recorded against the
 * last (most recent) month of the range, since that's the month the payer is
 * actually settling as of this payment. Anything that doesn't fit the expected
 * shape is left unparsed for manual review.
 */
public final class PaymentCommentParser {

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    // Latin letters that render identically to a Cyrillic letter, mapped to that letter,
    // so a comment typed on the wrong keyboard layout still matches.
    private static final Map<Character, Character> LATIN_HOMOGLYPHS = Map.ofEntries(
            Map.entry('a', 'а'), Map.entry('A', 'А'),
            Map.entry('c', 'с'), Map.entry('C', 'С'),
            Map.entry('e', 'е'), Map.entry('E', 'Е'),
            Map.entry('o', 'о'), Map.entry('O', 'О'),
            Map.entry('p', 'р'), Map.entry('P', 'Р'),
            Map.entry('x', 'х'), Map.entry('X', 'Х'),
            Map.entry('y', 'у'), Map.entry('Y', 'У')
    );

    private static final Pattern COMMENT_PATTERN = Pattern.compile(
            "^(?:о|с)плата\\s+за\\s+уроки\\s+вокалу\\s*[,;-]?\\s*"
                    + "([А-ЯІЇЄҐа-яіїєґ']+)\\s*[,;-]?\\s*"
                    + "(?:(\\d{4})\\s*[,;-]?\\s*)?"
                    + "(.+)$",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    // Matches "MONTH1-MONTH2" right after the header, e.g. "...вокалу, червень-серпень, ...".
    // Only collapsed to MONTH2 once both sides are confirmed to be real month names (see
    // collapseMonthRange), so a hyphen used as an ordinary separator before the payer name
    // (e.g. "...вокалу серпень-Прізвище...") is left untouched.
    private static final Pattern MONTH_RANGE = Pattern.compile(
            "^((?:о|с)плата\\s+за\\s+уроки\\s+вокалу\\s*[,;-]?\\s*)"
                    + "([А-ЯІЇЄҐа-яіїєґ']+)\\s*-\\s*([А-ЯІЇЄҐа-яіїєґ']+)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    private PaymentCommentParser() {
    }

    public static Optional<ParsedComment> parse(String rawComment) {
        if (rawComment == null) {
            return Optional.empty();
        }
        String normalized = WHITESPACE.matcher(rawComment.trim()).replaceAll(" ");
        normalized = replaceLatinHomoglyphs(normalized);
        normalized = collapseMonthRange(normalized);

        Matcher matcher = COMMENT_PATTERN.matcher(normalized);
        if (!matcher.matches()) {
            return Optional.empty();
        }

        String monthToken = matcher.group(1);
        String yearToken = matcher.group(2);
        String payerName = matcher.group(3).trim();
        if (payerName.isEmpty()) {
            return Optional.empty();
        }

        Integer year = yearToken != null ? Integer.valueOf(yearToken) : null;
        return UkrainianMonths.monthNumber(monthToken)
                .map(month -> new ParsedComment(month, year, payerName));
    }

    private static String collapseMonthRange(String input) {
        Matcher rangeMatcher = MONTH_RANGE.matcher(input);
        if (!rangeMatcher.find()) {
            return input;
        }
        String firstMonth = rangeMatcher.group(2);
        String lastMonth = rangeMatcher.group(3);
        if (UkrainianMonths.monthNumber(firstMonth).isEmpty() || UkrainianMonths.monthNumber(lastMonth).isEmpty()) {
            return input;
        }
        return input.substring(0, rangeMatcher.start(2)) + lastMonth + input.substring(rangeMatcher.end(3));
    }

    private static String replaceLatinHomoglyphs(String input) {
        StringBuilder result = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            result.append(LATIN_HOMOGLYPHS.getOrDefault(c, c));
        }
        return result.toString();
    }

    public record ParsedComment(int month, Integer year, String payerName) {
    }
}
