package org.tonique.vocal.journal;

import java.util.List;

/** cells has exactly 12 entries (months 1-12); an entry is null for a month before the student joined. */
public record JournalRow(
        Long studentId,
        String fullName,
        Long tariffPlanId,
        String tariffLabel,
        List<JournalCell> cells
) {
}
