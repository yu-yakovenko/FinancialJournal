package org.tonique.vocal.student;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Deterministic matching of a payer name parsed from a bank comment (typed by hand,
 * either as a full name "Прізвище Ім'я По-батькові" or as surname+initials
 * "Прізвище О.П.") against the student roster. No fuzzy/ML matching — ambiguous or
 * zero-candidate results are surfaced for manual review rather than guessed.
 */
public final class NameMatcher {

    private static final Pattern DISALLOWED_CHARS = Pattern.compile("[^А-ЯІЇЄҐа-яіїєґ'.\\s-]");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Pattern INITIAL_SEPARATOR = Pattern.compile("[.\\s]+");
    private static final Locale UKRAINIAN = Locale.forLanguageTag("uk");

    private NameMatcher() {
    }

    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        String cleaned = DISALLOWED_CHARS.matcher(raw).replaceAll(" ");
        cleaned = WHITESPACE.matcher(cleaned).replaceAll(" ").trim();
        return cleaned.toLowerCase(UKRAINIAN);
    }

    public static MatchResult match(String parsedName, List<Student> roster) {
        String normalized = normalize(parsedName);
        List<String> tokens = tokenize(normalized);
        if (tokens.isEmpty()) {
            return new MatchResult(List.of());
        }

        boolean surnameInitialsForm = normalized.contains(".") || tokens.size() <= 2;

        List<Student> candidates = surnameInitialsForm
                ? matchSurnameInitials(tokens, roster)
                : matchFullName(tokens, roster);

        return new MatchResult(candidates);
    }

    private static List<String> tokenize(String normalized) {
        return Arrays.stream(normalized.split("\\s+"))
                .filter(token -> !token.isBlank())
                .toList();
    }

    private static List<Student> matchFullName(List<String> tokens, List<Student> roster) {
        List<String> queryFirst3 = tokens.subList(0, Math.min(3, tokens.size()));
        List<Student> result = new ArrayList<>();
        for (Student student : roster) {
            List<String> studentTokens = tokenize(normalize(student.getFullName()));
            if (studentTokens.size() < 3) {
                continue;
            }
            List<String> studentFirst3 = studentTokens.subList(0, 3);
            if (studentFirst3.equals(queryFirst3)) {
                result.add(student);
            }
        }
        return result;
    }

    private static List<Student> matchSurnameInitials(List<String> tokens, List<Student> roster) {
        String surname = tokens.get(0);
        String rest = String.join(" ", tokens.subList(1, tokens.size()));
        List<Character> initials = extractInitials(rest);
        if (initials.isEmpty()) {
            return List.of();
        }

        List<Student> result = new ArrayList<>();
        for (Student student : roster) {
            List<String> studentTokens = tokenize(normalize(student.getFullName()));
            if (studentTokens.size() < 2 || !studentTokens.get(0).equals(surname)) {
                continue;
            }
            char studentFirstInitial = studentTokens.get(1).charAt(0);
            Character studentPatronymicInitial = studentTokens.size() >= 3
                    ? studentTokens.get(2).charAt(0)
                    : null;

            if (initials.get(0) != studentFirstInitial) {
                continue;
            }
            if (initials.size() >= 2) {
                if (studentPatronymicInitial == null || !studentPatronymicInitial.equals(initials.get(1))) {
                    continue;
                }
            }
            result.add(student);
        }
        return result;
    }

    private static List<Character> extractInitials(String rest) {
        List<Character> initials = new ArrayList<>();
        for (String chunk : INITIAL_SEPARATOR.split(rest)) {
            if (!chunk.isBlank()) {
                initials.add(chunk.charAt(0));
            }
        }
        return initials;
    }

    public record MatchResult(List<Student> candidates) {

        public Optional<Student> unique() {
            return candidates.size() == 1 ? Optional.of(candidates.get(0)) : Optional.empty();
        }
    }
}
