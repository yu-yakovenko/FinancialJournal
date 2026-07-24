package org.tonique.vocal.journal;

public record JournalCell(long amountKopiykas, CellStatus status) {

    public enum CellStatus {
        GREEN,
        YELLOW,
        RED
    }
}
