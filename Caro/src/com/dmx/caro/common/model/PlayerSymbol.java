package com.dmx.caro.common.model;

public enum PlayerSymbol {
    X('X'),
    O('O');

    private final char token;

    PlayerSymbol(char token) {
        this.token = token;
    }

    public char token() {
        return token;
    }

    public String wireValue() {
        return name();
    }

    public PlayerSymbol opposite() {
        return this == X ? O : X;
    }

    public static PlayerSymbol fromToken(char token) {
        return Character.toUpperCase(token) == 'X' ? X : O;
    }

    public static PlayerSymbol fromWireValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return PlayerSymbol.valueOf(value.trim().toUpperCase());
    }
}
