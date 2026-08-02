package org.tonique.vocal.payment;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentCommentParserTest {

    @Test
    void parsesStandardComment() {
        Optional<PaymentCommentParser.ParsedComment> result =
                PaymentCommentParser.parse("Оплата за уроки вокалу, серпень, Іваненко Ольга Петрівна");

        assertThat(result).isPresent();
        assertThat(result.get().month()).isEqualTo(8);
        assertThat(result.get().year()).isNull();
        assertThat(result.get().payerName()).isEqualTo("Іваненко Ольга Петрівна");
    }

    @Test
    void parsesOptionalDeclaredYear() {
        Optional<PaymentCommentParser.ParsedComment> result =
                PaymentCommentParser.parse("Оплата за уроки вокалу, червень 2026, Шевченко Олег");

        assertThat(result).isPresent();
        assertThat(result.get().month()).isEqualTo(6);
        assertThat(result.get().year()).isEqualTo(2026);
        assertThat(result.get().payerName()).isEqualTo("Шевченко Олег");
    }

    @Test
    void parsesDeclaredYearWithCommaSeparator() {
        Optional<PaymentCommentParser.ParsedComment> result =
                PaymentCommentParser.parse("Оплата за уроки вокалу, грудня, 2025, Коваль А. Б.");

        assertThat(result).isPresent();
        assertThat(result.get().month()).isEqualTo(12);
        assertThat(result.get().year()).isEqualTo(2025);
        assertThat(result.get().payerName()).isEqualTo("Коваль А. Б.");
    }

    @Test
    void parsesGenitiveMonthFormAndSurnameInitials() {
        Optional<PaymentCommentParser.ParsedComment> result =
                PaymentCommentParser.parse("оплата за уроки вокалу вересня Іваненко О.П.");

        assertThat(result).isPresent();
        assertThat(result.get().month()).isEqualTo(9);
        assertThat(result.get().payerName()).isEqualTo("Іваненко О.П.");
    }

    @Test
    void toleratesExtraWhitespaceAndSemicolons() {
        Optional<PaymentCommentParser.ParsedComment> result =
                PaymentCommentParser.parse("  Оплата  за уроки  вокалу;   грудні;   Коваль А. Б.  ");

        assertThat(result).isPresent();
        assertThat(result.get().month()).isEqualTo(12);
        assertThat(result.get().payerName()).isEqualTo("Коваль А. Б.");
    }

    @Test
    void acceptsSplataSpellingAsAlternativeToOplata() {
        Optional<PaymentCommentParser.ParsedComment> result =
                PaymentCommentParser.parse("Сплата за уроки вокалу, серпень, Іваненко Ольга Петрівна");

        assertThat(result).isPresent();
        assertThat(result.get().month()).isEqualTo(8);
        assertThat(result.get().payerName()).isEqualTo("Іваненко Ольга Петрівна");
    }

    @Test
    void acceptsSplataSpellingAsAlternativeToOplataLatinLaterS() {
        Optional<PaymentCommentParser.ParsedComment> result =
                PaymentCommentParser.parse("cплата за уроки вокалу, серпень, Іваненко Ольга Петрівна");

        assertThat(result).isPresent();
        assertThat(result.get().month()).isEqualTo(8);
        assertThat(result.get().payerName()).isEqualTo("Іваненко Ольга Петрівна");
    }

    @Test
    void acceptsSplataSpellingAsAlternativeToOplataSmallLater() {
        Optional<PaymentCommentParser.ParsedComment> result =
                PaymentCommentParser.parse("сплата за уроки вокалу, серпень, Іваненко Ольга Петрівна");

        assertThat(result).isPresent();
        assertThat(result.get().month()).isEqualTo(8);
        assertThat(result.get().payerName()).isEqualTo("Іваненко Ольга Петрівна");
    }

    @Test
    void parsesMonthRangeAsLastMonth() {
        Optional<PaymentCommentParser.ParsedComment> result =
                PaymentCommentParser.parse("Оплата за уроки вокалу, червень-серпень, Молодець Петро Васильович");

        assertThat(result).isPresent();
        assertThat(result.get().month()).isEqualTo(8);
        assertThat(result.get().payerName()).isEqualTo("Молодець Петро Васильович");
    }

    @Test
    void parsesMonthRangeWithSpacesAroundHyphen() {
        Optional<PaymentCommentParser.ParsedComment> result =
                PaymentCommentParser.parse("Оплата за уроки вокалу, червень - серпень, Молодець Петро Васильович");

        assertThat(result).isPresent();
        assertThat(result.get().month()).isEqualTo(8);
        assertThat(result.get().payerName()).isEqualTo("Молодець Петро Васильович");
    }

    @Test
    void treatsHyphenAsSeparatorWhenSecondTokenIsNotAMonth() {
        Optional<PaymentCommentParser.ParsedComment> result =
                PaymentCommentParser.parse("Оплата за уроки вокалу серпень-Молодець Петро Васильович");

        assertThat(result).isPresent();
        assertThat(result.get().month()).isEqualTo(8);
        assertThat(result.get().payerName()).isEqualTo("Молодець Петро Васильович");
    }

    @Test
    void failsOnUnknownMonthToken() {
        Optional<PaymentCommentParser.ParsedComment> result =
                PaymentCommentParser.parse("Оплата за уроки вокалу, місяцьX, Іваненко Ольга Петрівна");

        assertThat(result).isEmpty();
    }

    @Test
    void failsOnUnrelatedComment() {
        Optional<PaymentCommentParser.ParsedComment> result =
                PaymentCommentParser.parse("Поповнення рахунку");

        assertThat(result).isEmpty();
    }
}
