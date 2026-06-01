package com.dmx.caro.common.model;

import com.dmx.caro.common.protocol.NetworkMessage;
import java.util.List;

public final class GameSnapshot {
    private final String sessionId;
    private final int boardSize;
    private final String boardState;
    private final GameStatus status;
    private final PlayerSymbol nextTurnSymbol;
    private final PlayerSymbol winnerSymbol;
    private final CellPosition lastMove;
    private final List<CellPosition> winningLine;
    private final int moveCount;
    private final String reason;

    public GameSnapshot(
            String sessionId,
            int boardSize,
            String boardState,
            GameStatus status,
            PlayerSymbol nextTurnSymbol,
            PlayerSymbol winnerSymbol,
            CellPosition lastMove,
            List<CellPosition> winningLine,
            int moveCount,
            String reason) {
        this.sessionId = sessionId;
        this.boardSize = boardSize;
        this.boardState = boardState;
        this.status = status;
        this.nextTurnSymbol = nextTurnSymbol;
        this.winnerSymbol = winnerSymbol;
        this.lastMove = lastMove;
        this.winningLine = winningLine == null ? List.of() : List.copyOf(winningLine);
        this.moveCount = moveCount;
        this.reason = reason == null ? "" : reason;
    }

    public static GameSnapshot fromMessage(NetworkMessage message) {
        int lastRow = message.optionalInt("lastRow", -1);
        int lastCol = message.optionalInt("lastCol", -1);
        CellPosition lastMove = lastRow >= 0 && lastCol >= 0 ? new CellPosition(lastRow, lastCol) : null;

        return new GameSnapshot(
                message.required("sessionId"),
                message.requiredInt("boardSize"),
                message.required("board"),
                GameStatus.fromWireValue(message.required("status")),
                PlayerSymbol.fromWireValue(message.optional("nextTurnSymbol", "")),
                PlayerSymbol.fromWireValue(message.optional("winnerSymbol", "")),
                lastMove,
                BoardStateCodec.decodePositions(message.optional("winningLine", "")),
                message.optionalInt("moveCount", 0),
                message.optional("reason", ""));
    }

    public char[][] decodeBoard() {
        return BoardStateCodec.decode(boardState, boardSize);
    }

    public String sessionId() {
        return sessionId;
    }

    public int boardSize() {
        return boardSize;
    }

    public String boardState() {
        return boardState;
    }

    public GameStatus status() {
        return status;
    }

    public PlayerSymbol nextTurnSymbol() {
        return nextTurnSymbol;
    }

    public PlayerSymbol winnerSymbol() {
        return winnerSymbol;
    }

    public CellPosition lastMove() {
        return lastMove;
    }

    public List<CellPosition> winningLine() {
        return winningLine;
    }

    public int moveCount() {
        return moveCount;
    }

    public String reason() {
        return reason;
    }
}
