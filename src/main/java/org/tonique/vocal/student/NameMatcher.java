package org.tonique.vocal.student;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

    /**
     * True if two student names are the same person typed differently — either
     * identical once normalized, or one is a substring of the other (handles a
     * surname typed alone, or a missing patronymic). Used for duplicate-student
     * detection, not payment matching, so unlike {@link #match} it doesn't reason
     * about tokens/initials at all.
     */
    public static boolean isLikelyDuplicate(String fullNameA, String fullNameB) {
        String a = normalize(fullNameA);
        String b = normalize(fullNameB);
        if (a.isBlank() || b.isBlank()) {
            return false;
        }
        return a.equals(b) || a.contains(b) || b.contains(a);
    }

    /**
     * Groups students whose names look like the same person (see {@link #isLikelyDuplicate}).
     * Grouping is transitive (union-find), so a surname-only entry chains together with every
     * fuller name containing it, even if those fuller names aren't themselves duplicates of
     * each other (e.g. two siblings sharing a surname) — {@link #isClique} flags that case so
     * callers can tell a "safe" group (everyone matches everyone) from a merely "connected" one
     * that needs a human to look at it.
     */
    public static List<List<Student>> findDuplicateGroups(List<Student> students) {
        int n = students.size();
        int[] parent = new int[n];
        for (int i = 0; i < n; i++) {
            parent[i] = i;
        }

        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                if (isLikelyDuplicate(students.get(i).getFullName(), students.get(j).getFullName())) {
                    union(parent, i, j);
                }
            }
        }

        Map<Integer, List<Student>> byRoot = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            byRoot.computeIfAbsent(find(parent, i), root -> new ArrayList<>()).add(students.get(i));
        }

        return byRoot.values().stream().filter(group -> group.size() > 1).toList();
    }

    /** True if every pair in the group directly matches — safe to merge without a human check. */
    public static boolean isClique(List<Student> group) {
        for (int i = 0; i < group.size(); i++) {
            for (int j = i + 1; j < group.size(); j++) {
                if (!isLikelyDuplicate(group.get(i).getFullName(), group.get(j).getFullName())) {
                    return false;
                }
            }
        }
        return true;
    }

    private static int find(int[] parent, int i) {
        while (parent[i] != i) {
            parent[i] = parent[parent[i]];
            i = parent[i];
        }
        return i;
    }

    private static void union(int[] parent, int a, int b) {
        int rootA = find(parent, a);
        int rootB = find(parent, b);
        if (rootA != rootB) {
            parent[rootA] = rootB;
        }
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
        String surname = tokens.getFirst();
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
            return candidates.size() == 1 ? Optional.of(candidates.getFirst()) : Optional.empty();
        }
    }
}
