package org.tonique.vocal.student;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NameMatcherTest {

    private final Student ivanenko = new Student("Іваненко Ольга Петрівна");
    private final Student ivanenkoAnother = new Student("Іваненко Оксана Дмитрівна");
    private final Student koval = new Student("Коваль Андрій Борисович");

    @Test
    void matchesFullName() {
        NameMatcher.MatchResult result = NameMatcher.match("Іваненко Ольга Петрівна", List.of(ivanenko, koval));

        assertThat(result.unique()).contains(ivanenko);
    }

    @Test
    void matchesFullNameCaseInsensitively() {
        NameMatcher.MatchResult result = NameMatcher.match("іваненко ольга петрівна", List.of(ivanenko, koval));

        assertThat(result.unique()).contains(ivanenko);
    }

    @Test
    void matchesSurnameWithBothInitials() {
        NameMatcher.MatchResult result = NameMatcher.match("Іваненко О.П.", List.of(ivanenko, ivanenkoAnother, koval));

        assertThat(result.unique()).contains(ivanenko);
    }

    @Test
    void matchesSurnameWithSpacedInitials() {
        NameMatcher.MatchResult result = NameMatcher.match("Іваненко О. П.", List.of(ivanenko, ivanenkoAnother, koval));

        assertThat(result.unique()).contains(ivanenko);
    }

    @Test
    void ambiguousWithOnlyOneInitialAmongSameSurname() {
        NameMatcher.MatchResult result = NameMatcher.match("Іваненко О.", List.of(ivanenko, ivanenkoAnother, koval));

        assertThat(result.unique()).isEmpty();
        assertThat(result.candidates()).containsExactlyInAnyOrder(ivanenko, ivanenkoAnother);
    }

    @Test
    void noCandidatesForUnknownName() {
        NameMatcher.MatchResult result = NameMatcher.match("Петренко В.В.", List.of(ivanenko, koval));

        assertThat(result.candidates()).isEmpty();
        assertThat(result.unique()).isEmpty();
    }

    @Test
    void doesNotMatchDifferentSurnameEvenWithSameInitials() {
        Student other = new Student("Петренко Ольга Петрівна");

        NameMatcher.MatchResult result = NameMatcher.match("Іваненко О.П.", List.of(other));

        assertThat(result.candidates()).isEmpty();
    }
}
