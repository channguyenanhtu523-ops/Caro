package com.dmx.caro.server;

import com.dmx.caro.common.model.BoardStateCodec;
import com.dmx.caro.common.model.CellPosition;
import com.dmx.caro.common.model.GameSnapshot;
import com.dmx.caro.common.model.GameStatus;
import com.dmx.caro.common.model.MoveRecord;
import com.dmx.caro.common.model.PlayerSymbol;
import com.dmx.caro.common.model.VictoryDetector;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class GameRoom {
    private final String sessionId;
    private final int boardSize;
    private final int winLength;
    private final ClientHandler playerX;
    private final ClientHandler playerO;
    private final char[][] board;
    private final List<MoveRecord> moves;

    private GameStatus status;
    private PlayerSymbol nextTurnSymbol;
    private PlayerSymbol winnerSymbol;
    private CellPosition lastMove;
    private List<CellPosition> winningLine;
    private Instant startedAt;
    private Instant finishedAt;

    public GameRoom(int boardSize, int winLength, ClientHandler playerX, ClientHandler playerO) {
        this.sessionId = UUID.randomUUID().toString();
        this.boardSize = boardSize;
        this.winLength = winLength;
        this.playerX = playerX;
        this.playerO = playerO;
        this.board = BoardStateCodec.createEmptyBoard(boardSize);
        this.moves = new ArrayList<>();
        this.status = GameStatus.IN_PROGRESS;
        this.nextTurnSymbol = PlayerSymbol.X;
        this.winnerSymbol = null;
        this.lastMove = null;
        this.winningLine = List.of();
        this.startedAt = Instant.now();
        this.finishedAt = null;
    }

    public synchronized MoveResult playMove(ClientHandler player, int row, int col) {
        if (status != GameStatus.IN_PROGRESS) {
            return MoveResult.invalid("The game has already finished.");
        }

        PlayerSymbol playerSymbol = symbolOf(player);
        if (playerSymbol == null) {
            return MoveResult.invalid("Player does not belong to this session.");
        }
        if (playerSymbol != nextTurnSymbol) {
            return MoveResult.invalid("It is not your turn.");
        }
        if (!isInsideBoard(row, col)) {
            return MoveResult.invalid("Move is outside the board.");
        }
        if (board[row][col] != BoardStateCodec.EMPTY_CELL) {
            return MoveResult.invalid("Cell is already occupied.");
        }

        board[row][col] = playerSymbol.token();
        lastMove = new CellPosition(row, col);
        moves.add(new MoveRecord(row, col, playerSymbol, Instant.now()));

        List<CellPosition> line = VictoryDetector.findWinningLine(board, lastMove, winLength);
        if (!line.isEmpty()) {
            status = GameStatus.WON;
            winnerSymbol = playerSymbol;
            winningLine = line;
            finishedAt = Instant.now();
            return MoveResult.accepted(snapshot("Victory by five in a row."));
        }

        if (VictoryDetector.isBoardFull(board)) {
            status = GameStatus.DRAW;
            winningLine = List.of();
            finishedAt = Instant.now();
            return MoveResult.accepted(snapshot("Board is full."));
        }

        nextTurnSymbol = nextTurnSymbol.opposite();
        return MoveResult.accepted(snapshot("Move accepted."));
    }

    public synchronized GameSnapshot snapshot(String reason) {
        return new GameSnapshot(
                sessionId,
                boardSize,
                BoardStateCodec.encode(board),
                status,
                status == GameStatus.IN_PROGRESS ? nextTurnSymbol : null,
                winnerSymbol,
                lastMove,
                winningLine,
                moves.size(),
                reason);
    }

    public synchronized GameSnapshot disconnect(ClientHandler leaver) {
        if (status == GameStatus.IN_PROGRESS) {
            PlayerSymbol leaverSymbol = symbolOf(leaver);
            status = GameStatus.DISCONNECTED;
            winnerSymbol = leaverSymbol == null ? null : leaverSymbol.opposite();
            finishedAt = Instant.now();
            return snapshot("Opponent disconnected.");
        }
        return snapshot("Game already resolved.");
    }

    public String sessionId() {
        return sessionId;
    }

    public int boardSize() {
        return boardSize;
    }

    public int winLength() {
        return winLength;
    }

    public ClientHandler playerX() {
        return playerX;
    }

    public ClientHandler playerO() {
        return playerO;
    }

    public synchronized GameStatus status() {
        return status;
    }

    public synchronized PlayerSymbol nextTurnSymbol() {
        return nextTurnSymbol;
    }

    public synchronized PlayerSymbol winnerSymbol() {
        return winnerSymbol;
    }

    public synchronized Instant startedAt() {
        return startedAt;
    }

    public synchronized Instant finishedAt() {
        return finishedAt;
    }

    public synchronized List<MoveRecord> moves() {
        return List.copyOf(moves);
    }

    public String playerName(PlayerSymbol symbol) {
        ClientHandler handler = symbol == PlayerSymbol.X ? playerX : playerO;
        return handler.username();
    }

    public PlayerSymbol symbolOf(ClientHandler player) {
        if (player == playerX) {
            return PlayerSymbol.X;
        }
        if (player == playerO) {
            return PlayerSymbol.O;
        }
        return null;
    }

    public ClientHandler opponentOf(ClientHandler player) {
        if (player == playerX) {
            return playerO;
        }
        if (player == playerO) {
            return playerX;
        }
        return null;
    }

    public synchronized boolean isFinished() {
        return status != GameStatus.IN_PROGRESS;
    }

    private boolean isInsideBoard(int row, int col) {
        return row >= 0 && row < boardSize && col >= 0 && col < boardSize;
    }
}
