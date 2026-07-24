package org.tonique.vocal.monobank;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Splits a date range into consecutive windows no longer than a given number of days —
 * Monobank's statement endpoint rejects windows longer than 31 days. Pure logic, no I/O.
 */
public final class DateRangeChunker {

    private DateRangeChunker() {
    }

    public static List<Chunk> chunk(LocalDate from, LocalDate to, int maxDaysPerChunk) {
        if (to.isBefore(from)) {
            throw new IllegalArgumentException("`to` must not be before `from`");
        }
        if (maxDaysPerChunk < 1) {
            throw new IllegalArgumentException("maxDaysPerChunk must be at least 1");
        }

        List<Chunk> chunks = new ArrayList<>();
        LocalDate chunkStart = from;
        while (!chunkStart.isAfter(to)) {
            LocalDate chunkEnd = chunkStart.plusDays(maxDaysPerChunk - 1L);
            if (chunkEnd.isAfter(to)) {
                chunkEnd = to;
            }
            chunks.add(new Chunk(chunkStart, chunkEnd));
            chunkStart = chunkEnd.plusDays(1);
        }
        return chunks;
    }

    public record Chunk(LocalDate from, LocalDate to) {
    }
}
