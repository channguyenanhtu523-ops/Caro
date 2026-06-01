package com.dmx.caro.common.protocol;

import com.dmx.caro.common.config.AppConfig;
import com.dmx.caro.common.model.BoardStateCodec;
import com.dmx.caro.common.model.GameSnapshot;
import com.dmx.caro.common.model.PlayerSymbol;

public final class Messages {
    private Messages() {
    }

    public static NetworkMessage hello(String version) {
        return NetworkMessage.type(MessageType.HELLO)
                .field("version", version)
                .build();
    }

    public static NetworkMessage serverHello(String serverName, String publicKey, AppConfig config) {
        return NetworkMessage.type(MessageType.SERVER_HELLO)
                .field("serverName", serverName)
                .field("publicKey", publicKey)
                .field("boardSize", config.boardSize())
                .field("winLength", config.winLength())
                .field("heartbeatSeconds", config.heartbeatSeconds())
                .field("maxChatLength", config.maxChatLength())
                .build();
    }

    public static NetworkMessage keyExchange(String encryptedBundle) {
        return NetworkMessage.type(MessageType.KEY_EXCHANGE)
                .field("bundle", encryptedBundle)
                .build();
    }

    public static NetworkMessage keyAck(String message) {
        return NetworkMessage.type(MessageType.KEY_ACK)
                .field("message", message)
                .build();
    }

    public static NetworkMessage login(String username) {
        return NetworkMessage.type(MessageType.LOGIN)
                .field("username", username)
                .build();
    }

    public static NetworkMessage loginResult(boolean accepted, String message) {
        return NetworkMessage.type(MessageType.LOGIN_RESULT)
                .field("accepted", accepted)
                .field("message", message)
                .build();
    }

    public static NetworkMessage rematch() {
        return NetworkMessage.type(MessageType.REMATCH).build();
    }

    public static NetworkMessage rematchResult(boolean accepted, String message) {
        return NetworkMessage.type(MessageType.REMATCH_RESULT)
                .field("accepted", accepted)
                .field("message", message)
                .build();
    }

    public static NetworkMessage matchFound(
            String sessionId,
            PlayerSymbol playerSymbol,
            String opponent,
            int boardSize,
            int winLength) {
        return NetworkMessage.type(MessageType.MATCH_FOUND)
                .field("sessionId", sessionId)
                .field("symbol", playerSymbol.wireValue())
                .field("opponent", opponent)
                .field("boardSize", boardSize)
                .field("winLength", winLength)
                .build();
    }

    public static NetworkMessage move(String sessionId, int row, int col, int moveNumber) {
        return NetworkMessage.type(MessageType.MOVE)
                .field("sessionId", sessionId)
                .field("row", row)
                .field("col", col)
                .field("moveNumber", moveNumber)
                .build();
    }

    public static NetworkMessage chat(String sessionId, String from, String text, String sentAt) {
        return NetworkMessage.type(MessageType.CHAT)
                .field("sessionId", sessionId)
                .field("from", from)
                .field("text", text)
                .field("sentAt", sentAt)
                .build();
    }

    public static NetworkMessage heartbeat(long timestamp) {
        return NetworkMessage.type(MessageType.HEARTBEAT)
                .field("timestamp", timestamp)
                .build();
    }

    public static NetworkMessage gameState(
            GameSnapshot snapshot,
            String nextTurnPlayer,
            String winnerPlayer,
            String statusMessage) {
        NetworkMessage.Builder builder = NetworkMessage.type(MessageType.GAME_STATE)
                .field("sessionId", snapshot.sessionId())
                .field("boardSize", snapshot.boardSize())
                .field("board", snapshot.boardState())
                .field("status", snapshot.status().name())
                .field("moveCount", snapshot.moveCount())
                .field("reason", snapshot.reason())
                .field("winningLine", BoardStateCodec.encodePositions(snapshot.winningLine()))
                .field("statusMessage", statusMessage)
                .field("nextTurnPlayer", nextTurnPlayer)
                .field("winnerPlayer", winnerPlayer);

        if (snapshot.nextTurnSymbol() != null) {
            builder.field("nextTurnSymbol", snapshot.nextTurnSymbol().wireValue());
        }
        if (snapshot.winnerSymbol() != null) {
            builder.field("winnerSymbol", snapshot.winnerSymbol().wireValue());
        }
        if (snapshot.lastMove() != null) {
            builder.field("lastRow", snapshot.lastMove().row());
            builder.field("lastCol", snapshot.lastMove().col());
        }
        return builder.build();
    }

    public static NetworkMessage gameOver(GameSnapshot snapshot, String winnerPlayer, String reason) {
        return NetworkMessage.type(MessageType.GAME_OVER)
                .field("sessionId", snapshot.sessionId())
                .field("boardSize", snapshot.boardSize())
                .field("board", snapshot.boardState())
                .field("status", snapshot.status().name())
                .field("moveCount", snapshot.moveCount())
                .field("reason", reason)
                .field("winningLine", BoardStateCodec.encodePositions(snapshot.winningLine()))
                .field("winnerSymbol", snapshot.winnerSymbol() == null ? "" : snapshot.winnerSymbol().wireValue())
                .field("winnerPlayer", winnerPlayer)
                .field("lastRow", snapshot.lastMove() == null ? -1 : snapshot.lastMove().row())
                .field("lastCol", snapshot.lastMove() == null ? -1 : snapshot.lastMove().col())
                .build();
    }

    public static NetworkMessage error(String code, String message) {
        return NetworkMessage.type(MessageType.ERROR)
                .field("code", code)
                .field("message", message)
                .build();
    }
}
