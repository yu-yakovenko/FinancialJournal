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

    @Test
    void isLikelyDuplicateForIdenticalNames() {
        assertThat(NameMatcher.isLikelyDuplicate("Німчук Д.О.", "Німчук Д.О.")).isTrue();
    }

    @Test
    void isLikelyDuplicateIgnoresCase() {
        assertThat(NameMatcher.isLikelyDuplicate("німчук д.о.", "НІМЧУК Д.О.")).isTrue();
    }

    @Test
    void isLikelyDuplicateWhenOneNameIsASubstringOfTheOther() {
        assertThat(NameMatcher.isLikelyDuplicate("Фурманюк Антон", "Фурманюк Антон Васильович")).isTrue();
        assertThat(NameMatcher.isLikelyDuplicate("Фурманюк Антон Васильович", "Фурманюк Антон")).isTrue();
    }

    @Test
    void isLikelyDuplicateForSurnameOnlyEntry() {
        assertThat(NameMatcher.isLikelyDuplicate("Фурманюк", "Фурманюк Антон Васильович")).isTrue();
    }

    @Test
    void isNotLikelyDuplicateForUnrelatedNames() {
        assertThat(NameMatcher.isLikelyDuplicate("Іваненко Ольга Петрівна", "Коваль Андрій Борисович")).isFalse();
    }

    @Test
    void isNotLikelyDuplicateForBlankNames() {
        assertThat(NameMatcher.isLikelyDuplicate("", "Коваль Андрій Борисович")).isFalse();
        assertThat(NameMatcher.isLikelyDuplicate("Коваль Андрій Борисович", "   ")).isFalse();
        assertThat(NameMatcher.isLikelyDuplicate("", "")).isFalse();
    }

    @Test
    void groupsExactDuplicates() {
        Student a = new Student("Німчук Д.О.");
        Student b = new Student("Німчук Д.О.");

        List<List<Student>> groups = NameMatcher.findDuplicateGroups(List.of(a, b, koval));

        assertThat(groups).containsExactly(List.of(a, b));
    }

    @Test
    void groupsSubstringDuplicates() {
        Student full = new Student("Фурманюк Антон Васильович");
        Student partial = new Student("Фурманюк Антон");

        List<List<Student>> groups = NameMatcher.findDuplicateGroups(List.of(full, partial));

        assertThat(groups).containsExactly(List.of(full, partial));
    }

    @Test
    void doesNotGroupUnrelatedStudents() {
        assertThat(NameMatcher.findDuplicateGroups(List.of(ivanenko, koval))).isEmpty();
    }

    @Test
    void chainsGroupsTransitivelyThroughASurnameOnlyEntry() {
        Student surnameOnly = new Student("Іваненко");

        List<List<Student>> groups = NameMatcher.findDuplicateGroups(List.of(surnameOnly, ivanenko, ivanenkoAnother));

        assertThat(groups).hasSize(1);
        assertThat(groups.getFirst()).containsExactlyInAnyOrder(surnameOnly, ivanenko, ivanenkoAnother);
    }

    @Test
    void chainedGroupThroughSurnameOnlyIsNotAClique() {
        Student surnameOnly = new Student("Іваненко");

        boolean clique = NameMatcher.isClique(List.of(surnameOnly, ivanenko, ivanenkoAnother));

        assertThat(clique).isFalse();
    }

    @Test
    void groupWhereEveryoneMatchesEveryoneIsAClique() {
        Student full = new Student("Фурманюк Антон Васильович");
        Student partial = new Student("Фурманюк Антон");

        assertThat(NameMatcher.isClique(List.of(full, partial))).isTrue();
    }
}
