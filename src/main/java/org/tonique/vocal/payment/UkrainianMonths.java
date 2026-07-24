package org.tonique.vocal.payment;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Maps the grammatical forms of Ukrainian month names people actually type by hand
 * (nominative/genitive/locative) to a 1-12 month number.
 */
public final class UkrainianMonths {

    private static final Locale UKRAINIAN = Locale.forLanguageTag("uk");

    private static final Map<String, Integer> FORMS = Map.ofEntries(
            Map.entry("січень", 1), Map.entry("січня", 1), Map.entry("січні", 1),
            Map.entry("лютий", 2), Map.entry("лютого", 2), Map.entry("лютому", 2),
            Map.entry("березень", 3), Map.entry("березня", 3), Map.entry("березні", 3),
            Map.entry("квітень", 4), Map.entry("квітня", 4), Map.entry("квітні", 4),
            Map.entry("травень", 5), Map.entry("травня", 5), Map.entry("травні", 5),
            Map.entry("червень", 6), Map.entry("червня", 6), Map.entry("червні", 6),
            Map.entry("липень", 7), Map.entry("липня", 7), Map.entry("липні", 7),
            Map.entry("серпень", 8), Map.entry("серпня", 8), Map.entry("серпні", 8),
            Map.entry("вересень", 9), Map.entry("вересня", 9), Map.entry("вересні", 9),
            Map.entry("жовтень", 10), Map.entry("жовтня", 10), Map.entry("жовтні", 10),
            Map.entry("листопад", 11), Map.entry("листопада", 11), Map.entry("листопаді", 11),
            Map.entry("грудень", 12), Map.entry("грудня", 12), Map.entry("грудні", 12)
    );

    private UkrainianMonths() {
    }

    public static Optional<Integer> monthNumber(String token) {
        if (token == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(FORMS.get(token.toLowerCase(UKRAINIAN)));
    }
}
