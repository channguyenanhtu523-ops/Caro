package com.dmx.caro.client.model;

import com.dmx.caro.common.model.BoardStateCodec;
import com.dmx.caro.common.model.CellPosition;
import com.dmx.caro.common.model.GameSnapshot;
import com.dmx.caro.common.model.GameStatus;
import com.dmx.caro.common.model.PlayerSymbol;
import java.util.List;

public final class ClientGameModel {
    private String username = "";
    private String opponent = "";
    private String sessionId = "";
    private int boardSize = 15;
    private int winLength = 5;
    private PlayerSymbol mySymbol;
    private PlayerSymbol nextTurnSymbol;
    private PlayerSymbol winnerSymbol;
    private GameStatus status = GameStatus.WAITING;
    private char[][] board = BoardStateCodec.createEmptyBoard(boardSize);
    private CellPosition lastMove;
    private List<CellPosition> winningLine = List.of();
    private int moveCount;
    private String statusMessage = "Chưa kết nối";
    private boolean connected;

    public synchronized void setUsername(String username) {
        this.username = username;
    }

    public synchronized void setServerSettings(int boardSize, int winLength) {
        this.boardSize = boardSize;
        this.winLength = winLength;
        this.board = BoardStateCodec.createEmptyBoard(boardSize);
    }

    public synchronized void prepareLobby() {
        resetSessionState(GameStatus.WAITING);
        statusMessage = "Đang chờ ghép trận...";
    }

    public synchronized void prepareMatch(
            String sessionId,
            String opponent,
            int boardSize,
            int winLength,
            PlayerSymbol mySymbol) {
        this.sessionId = sessionId;
        this.opponent = opponent;
        this.boardSize = boardSize;
        this.winLength = winLength;
        this.mySymbol = mySymbol;
        this.board = BoardStateCodec.createEmptyBoard(boardSize);
        this.status = GameStatus.IN_PROGRESS;
        this.nextTurnSymbol = PlayerSymbol.X;
        this.winnerSymbol = null;
        this.lastMove = null;
        this.winningLine = List.of();
        this.moveCount = 0;
        this.statusMessage = "Match found. " + (mySymbol == PlayerSymbol.X ? "You go first." : "Opponent goes first.");
    }

    public synchronized void applySnapshot(GameSnapshot snapshot, String statusMessage) {
        this.sessionId = snapshot.sessionId();
        this.boardSize = snapshot.boardSize();
        this.board = snapshot.decodeBoard();
        this.status = snapshot.status();
        this.nextTurnSymbol = snapshot.nextTurnSymbol();
        this.winnerSymbol = snapshot.winnerSymbol();
        this.lastMove = snapshot.lastMove();
        this.winningLine = snapshot.winningLine();
        this.moveCount = snapshot.moveCount();
        this.statusMessage = statusMessage == null || statusMessage.isBlank() ? snapshot.reason() : statusMessage;
    }

    public synchronized void markConnected(boolean connected, String statusMessage) {
        this.connected = connected;
        if (statusMessage != null && !statusMessage.isBlank()) {
            this.statusMessage = statusMessage;
        }
    }

    public synchronized void markDisconnected(String statusMessage) {
        connected = false;
        resetSessionState(GameStatus.DISCONNECTED);
        if (statusMessage != null && !statusMessage.isBlank()) {
            this.statusMessage = statusMessage;
        }
    }

    public synchronized String username() {
        return username;
    }

    public synchronized String opponent() {
        return opponent;
    }

    public synchronized String sessionId() {
        return sessionId;
    }

    public synchronized int boardSize() {
        return boardSize;
    }

    public synchronized int winLength() {
        return winLength;
    }

    public synchronized PlayerSymbol mySymbol() {
        return mySymbol;
    }

    public synchronized PlayerSymbol nextTurnSymbol() {
        return nextTurnSymbol;
    }

    public synchronized PlayerSymbol winnerSymbol() {
        return winnerSymbol;
    }

    public synchronized GameStatus status() {
        return status;
    }

    public synchronized char[][] boardCopy() {
        char[][] snapshot = new char[board.length][board.length];
        for (int row = 0; row < board.length; row++) {
            System.arraycopy(board[row], 0, snapshot[row], 0, board[row].length);
        }
        return snapshot;
    }

    public synchronized CellPosition lastMove() {
        return lastMove;
    }

    public synchronized List<CellPosition> winningLine() {
        return winningLine;
    }

    public synchronized int moveCount() {
        return moveCount;
    }

    public synchronized String statusMessage() {
        return statusMessage;
    }

    public synchronized boolean connected() {
        return connected;
    }

    public synchronized boolean isMyTurn() {
        return status == GameStatus.IN_PROGRESS && mySymbol != null && mySymbol == nextTurnSymbol;
    }

    public synchronized boolean canPlayAt(int row, int col) {
        return isMyTurn() && row >= 0 && row < boardSize && col >= 0 && col < boardSize
                && board[row][col] == BoardStateCodec.EMPTY_CELL;
    }

    private void resetSessionState(GameStatus nextStatus) {
        opponent = "";
        sessionId = "";
        mySymbol = null;
        nextTurnSymbol = null;
        winnerSymbol = null;
        status = nextStatus;
        board = BoardStateCodec.createEmptyBoard(boardSize);
        lastMove = null;
        winningLine = List.of();
        moveCount = 0;
    }
}
