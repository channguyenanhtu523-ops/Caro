package com.dmx.caro.server;

import com.dmx.caro.common.model.GameSnapshot;

public final class MoveResult {
    private final boolean accepted;
    private final String message;
    private final GameSnapshot snapshot;

    private MoveResult(boolean accepted, String message, GameSnapshot snapshot) {
        this.accepted = accepted;
        this.message = message;
        this.snapshot = snapshot;
    }

    public static MoveResult accepted(GameSnapshot snapshot) {
        return new MoveResult(true, snapshot.reason(), snapshot);
    }

    public static MoveResult invalid(String message) {
        return new MoveResult(false, message, null);
    }

    public boolean accepted() {
        return accepted;
    }

    public String message() {
        return message;
    }

    public GameSnapshot snapshot() {
        return snapshot;
    }
}
