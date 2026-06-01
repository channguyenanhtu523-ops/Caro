package com.dmx.caro.common.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class VictoryDetector {
    private static final int[][] DIRECTIONS = {
            {1, 0},
            {0, 1},
            {1, 1},
            {1, -1}
    };

    private VictoryDetector() {
    }

    public static List<CellPosition> findWinningLine(char[][] board, CellPosition lastMove, int winLength) {
        if (lastMove == null) {
            return List.of();
        }

        char target = board[lastMove.row()][lastMove.col()];
        if (target == BoardStateCodec.EMPTY_CELL) {
            return List.of();
        }

        for (int[] direction : DIRECTIONS) {
            List<CellPosition> line = new ArrayList<>();
            line.add(lastMove);
            collect(board, target, lastMove, direction[0], direction[1], line);
            collect(board, target, lastMove, -direction[0], -direction[1], line);

            if (line.size() >= winLength) {
                return trimToWinningLine(line, lastMove, direction[0], direction[1], winLength);
            }
        }
        return List.of();
    }

    public static boolean isBoardFull(char[][] board) {
        for (char[] row : board) {
            for (char cell : row) {
                if (cell == BoardStateCodec.EMPTY_CELL) {
                    return false;
                }
            }
        }
        return true;
    }

    private static void collect(
            char[][] board,
            char target,
            CellPosition origin,
            int rowDelta,
            int colDelta,
            List<CellPosition> positions) {
        int row = origin.row() + rowDelta;
        int col = origin.col() + colDelta;
        while (row >= 0
                && row < board.length
                && col >= 0
                && col < board[row].length
                && board[row][col] == target) {
            positions.add(new CellPosition(row, col));
            row += rowDelta;
            col += colDelta;
        }
    }

    private static List<CellPosition> trimToWinningLine(
            List<CellPosition> positions,
            CellPosition lastMove,
            int rowDelta,
            int colDelta,
            int winLength) {
        positions.sort(Comparator
                .comparingInt((CellPosition position) -> projection(position, rowDelta, colDelta))
                .thenComparingInt(CellPosition::row)
                .thenComparingInt(CellPosition::col));

        int originIndex = positions.indexOf(lastMove);
        int start = Math.max(0, originIndex - (winLength - 1));
        int end = Math.min(positions.size(), start + winLength);
        if (end - start < winLength) {
            start = Math.max(0, end - winLength);
        }
        return List.copyOf(positions.subList(start, start + winLength));
    }

    private static int projection(CellPosition position, int rowDelta, int colDelta) {
        return position.row() * rowDelta + position.col() * colDelta;
    }
}
