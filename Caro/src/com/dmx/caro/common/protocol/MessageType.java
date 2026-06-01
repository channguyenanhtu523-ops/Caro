package com.dmx.caro.common.protocol;

public enum MessageType {
    HELLO,
    SERVER_HELLO,
    KEY_EXCHANGE,
    KEY_ACK,
    LOGIN,
    LOGIN_RESULT,
    REMATCH,
    REMATCH_RESULT,
    MATCH_FOUND,
    MOVE,
    CHAT,
    HEARTBEAT,
    GAME_STATE,
    GAME_OVER,
    ERROR
}
