package org.tonique.vocal.monobank;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DateRangeChunkerTest {

    @Test
    void singleDayRangeIsOneChunk() {
        LocalDate day = LocalDate.of(2025, 3, 10);

        List<DateRangeChunker.Chunk> chunks = DateRangeChunker.chunk(day, day, 31);

        assertThat(chunks).containsExactly(new DateRangeChunker.Chunk(day, day));
    }

    @Test
    void rangeOfExactlyMaxDaysIsOneChunk() {
        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to = from.plusDays(30); // 31 days inclusive

        List<DateRangeChunker.Chunk> chunks = DateRangeChunker.chunk(from, to, 31);

        assertThat(chunks).containsExactly(new DateRangeChunker.Chunk(from, to));
    }

    @Test
    void rangeOneDayOverMaxSplitsIntoTwoChunks() {
        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to = from.plusDays(31); // 32 days inclusive

        List<DateRangeChunker.Chunk> chunks = DateRangeChunker.chunk(from, to, 31);

        assertThat(chunks).containsExactly(
                new DateRangeChunker.Chunk(from, from.plusDays(30)),
                new DateRangeChunker.Chunk(from.plusDays(31), to)
        );
    }

    @Test
    void fullYearSplitsIntoTwelveTrailingPartialChunk() {
        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to = LocalDate.of(2025, 12, 31); // 365 days inclusive

        List<DateRangeChunker.Chunk> chunks = DateRangeChunker.chunk(from, to, 31);

        assertThat(chunks).hasSize(12);
        assertThat(chunks.getFirst().from()).isEqualTo(from);
        assertThat(chunks.getLast().to()).isEqualTo(to);
        // chunks are contiguous with no gaps or overlaps
        for (int i = 1; i < chunks.size(); i++) {
            assertThat(chunks.get(i).from()).isEqualTo(chunks.get(i - 1).to().plusDays(1));
        }
    }

    @Test
    void toBeforeFromIsRejected() {
        LocalDate from = LocalDate.of(2025, 5, 1);
        LocalDate to = from.minusDays(1);

        assertThatThrownBy(() -> DateRangeChunker.chunk(from, to, 31))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
