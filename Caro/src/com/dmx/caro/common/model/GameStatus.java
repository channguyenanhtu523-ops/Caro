package com.dmx.caro.common.model;

public enum GameStatus {
    WAITING,
    IN_PROGRESS,
    WON,
    DRAW,
    DISCONNECTED;

    public static GameStatus fromWireValue(String value) {
        if (value == null || value.isBlank()) {
            return WAITING;
        }
        return GameStatus.valueOf(value.trim().toUpperCase());
    }
}
