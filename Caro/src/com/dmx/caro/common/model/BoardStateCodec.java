package com.dmx.caro.common.model;

import java.util.ArrayList;
import java.util.List;

public final class BoardStateCodec {
    public static final char EMPTY_CELL = '.';

    private BoardStateCodec() {
    }

    public static char[][] createEmptyBoard(int size) {
        char[][] board = new char[size][size];
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                board[row][col] = EMPTY_CELL;
            }
        }
        return board;
    }

    public static String encode(char[][] board) {
        StringBuilder builder = new StringBuilder();
        for (int row = 0; row < board.length; row++) {
            if (row > 0) {
                builder.append('/');
            }
            for (int col = 0; col < board[row].length; col++) {
                builder.append(board[row][col]);
            }
        }
        return builder.toString();
    }

    public static char[][] decode(String encodedBoard, int boardSize) {
        char[][] board = createEmptyBoard(boardSize);
        if (encodedBoard == null || encodedBoard.isBlank()) {
            return board;
        }

        String[] rows = encodedBoard.split("/");
        for (int row = 0; row < Math.min(rows.length, boardSize); row++) {
            char[] chars = rows[row].toCharArray();
            for (int col = 0; col < Math.min(chars.length, boardSize); col++) {
                board[row][col] = chars[col];
            }
        }
        return board;
    }

    public static String encodePositions(List<CellPosition> positions) {
        if (positions == null || positions.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        for (CellPosition position : positions) {
            if (builder.length() > 0) {
                builder.append(';');
            }
            builder.append(position.row()).append(',').append(position.col());
        }
        return builder.toString();
    }

    public static List<CellPosition> decodePositions(String encodedPositions) {
        List<CellPosition> result = new ArrayList<>();
        if (encodedPositions == null || encodedPositions.isBlank()) {
            return result;
        }

        String[] tokens = encodedPositions.split(";");
        for (String token : tokens) {
            String[] coordinates = token.split(",");
            if (coordinates.length != 2) {
                continue;
            }

            try {
                int row = Integer.parseInt(coordinates[0].trim());
                int col = Integer.parseInt(coordinates[1].trim());
                result.add(new CellPosition(row, col));
            } catch (NumberFormatException ignored) {
                // Ignore malformed coordinates rather than breaking an entire snapshot.
            }
        }
        return result;
    }
}
