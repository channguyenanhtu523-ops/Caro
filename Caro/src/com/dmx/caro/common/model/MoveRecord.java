package com.dmx.caro.common.model;

import java.time.Instant;

public final class MoveRecord {
    private final int row;
    private final int col;
    private final PlayerSymbol symbol;
    private final Instant playedAt;

    public MoveRecord(int row, int col, PlayerSymbol symbol, Instant playedAt) {
        this.row = row;
        this.col = col;
        this.symbol = symbol;
        this.playedAt = playedAt;
    }

    public int row() {
        return row;
    }

    public int col() {
        return col;
    }

    public PlayerSymbol symbol() {
        return symbol;
    }

    public Instant playedAt() {
        return playedAt;
    }
}
