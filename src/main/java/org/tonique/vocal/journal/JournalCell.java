package org.tonique.vocal.journal;

/** expectedAmountKopiykas is the tariff price that was actually in effect for that
 *  specific month (null if the student had no tariff plan assigned at the time). */
public record JournalCell(long amountKopiykas, Long expectedAmountKopiykas, CellStatus status) {

    public enum CellStatus {
        GREEN,
        YELLOW,
        RED
    }
}
