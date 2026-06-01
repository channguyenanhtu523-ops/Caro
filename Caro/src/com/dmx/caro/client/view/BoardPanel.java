package com.dmx.caro.client.view;

import com.dmx.caro.client.model.ClientGameModel;
import com.dmx.caro.common.model.BoardStateCodec;
import com.dmx.caro.common.model.CellPosition;
import com.dmx.caro.common.model.PlayerSymbol;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import javax.swing.JPanel;

public final class BoardPanel extends JPanel {
    private static final Color BOARD_BACKGROUND = new Color(245, 224, 188);
    private static final Color GRID_COLOR = new Color(122, 92, 59);
    private static final Color X_COLOR = new Color(193, 62, 62);
    private static final Color O_COLOR = new Color(47, 102, 180);
    private static final Color HOVER_COLOR = new Color(255, 255, 255, 96);
    private static final Color LAST_MOVE_COLOR = new Color(255, 215, 64, 140);
    private static final Color WIN_HIGHLIGHT = new Color(99, 196, 124, 180);
    private static final Color WIN_TEXT_COLOR = new Color(228, 71, 142);  // Màu hồng cánh sen đậm đà giống mẫu
    private static final Color LOSE_TEXT_COLOR = new Color(70, 75, 85);   // Màu xám tối khi thua cuộc
    private static final Color TEXT_BORDER_COLOR = new Color(46, 39, 33); // Màu viền chữ đen xám để làm nổi bật font

    private final ClientGameModel model;
    private BiConsumer<Integer, Integer> moveListener;
    private int hoverRow = -1;
    private int hoverCol = -1;

    public BoardPanel(ClientGameModel model) {
        this.model = model;
        setOpaque(false);
        setPreferredSize(new Dimension(660, 660));

        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent event) {
                updateHover(event.getPoint());
            }

            @Override
            public void mouseExited(MouseEvent event) {
                hoverRow = -1;
                hoverCol = -1;
                repaint();
            }

            @Override
            public void mouseClicked(MouseEvent event) {
                if (moveListener == null) {
                    return;
                }
                Point cell = toCell(event.getPoint());
                if (cell.x >= 0 && model.canPlayAt(cell.x, cell.y)) {
                    moveListener.accept(cell.x, cell.y);
                }
            }
        };

        addMouseListener(mouseAdapter);
        addMouseMotionListener(mouseAdapter);
    }

    public void setMoveListener(BiConsumer<Integer, Integer> moveListener) {
        this.moveListener = moveListener;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        int size = model.boardSize();
        int padding = 28;
        int available = Math.min(getWidth(), getHeight()) - padding * 2;
        int cellSize = Math.max(1, available / size);
        int boardPixels = cellSize * size;
        int startX = (getWidth() - boardPixels) / 2;
        int startY = (getHeight() - boardPixels) / 2;

        g2.setColor(BOARD_BACKGROUND);
        g2.fillRoundRect(startX - 14, startY - 14, boardPixels + 28, boardPixels + 28, 28, 28);

        paintHighlights(g2, startX, startY, cellSize);

        g2.setColor(GRID_COLOR);
        g2.setStroke(new BasicStroke(1.2f));
        for (int index = 0; index <= size; index++) {
            int x = startX + index * cellSize;
            int y = startY + index * cellSize;
            g2.drawLine(startX, y, startX + boardPixels, y);
            g2.drawLine(x, startY, x, startY + boardPixels);
        }

        char[][] board = model.boardCopy();
        g2.setFont(new Font("Segoe UI", Font.BOLD, Math.max(16, cellSize - 8)));
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                char token = board[row][col];
                if (token == BoardStateCodec.EMPTY_CELL) {
                    continue;
                }
                int cellX = startX + col * cellSize;
                int cellY = startY + row * cellSize;
                if (token == PlayerSymbol.X.token()) {
                    paintX(g2, cellX, cellY, cellSize);
                } else {
                    paintO(g2, cellX, cellY, cellSize);
                }
            }
        }

        if (model.status() == com.dmx.caro.common.model.GameStatus.WON) {
            boolean iWon = model.statusMessage().contains("bạn chiến thắng") || model.statusMessage().contains("Bạn chiến thắng");

            if (model.winningLine() != null && !model.winningLine().isEmpty()) {
                com.dmx.caro.common.model.CellPosition pos = model.winningLine().get(0);
                char winToken = board[pos.row()][pos.col()];
                if (model.mySymbol() != null) {
                    iWon = (winToken == model.mySymbol().token());
                }
            }

            // Thiết lập nội dung chữ hiển thị
            String endMessage = iWon ? "Bạn Đã Thắng!" : "Bạn Đã Thua!";
            Color messageColor = iWon ? WIN_TEXT_COLOR : LOSE_TEXT_COLOR;

            // Cấu hình Font chữ to và dày dặn
            g2.setFont(new Font("Segoe UI", Font.BOLD, 48));
            java.awt.FontMetrics fm = g2.getFontMetrics();

            // Tính toán vị trí tọa độ X, Y để chữ luôn nằm chính giữa tâm BoardPanel
            int textX = (getWidth() - fm.stringWidth(endMessage)) / 2;
            int textY = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();

            // 1. Vẽ một lớp nền mờ phía sau để bàn cờ tối nhẹ xuống, làm nổi bật chữ
            g2.setColor(new Color(20, 20, 22, 100)); // Màu đen mờ trong suốt
            g2.fillRoundRect(startX - 14, startY - 14, boardPixels + 28, boardPixels + 28, 28, 28);

            // 2. Vẽ viền chữ nghệ thuật (Bằng cách vẽ outline dạng Shape)
            java.awt.font.TextLayout textLayout = new java.awt.font.TextLayout(endMessage, g2.getFont(), g2.getFontRenderContext());
            java.awt.Shape shape = textLayout.getOutline(java.awt.geom.AffineTransform.getTranslateInstance(textX, textY));

            g2.setColor(TEXT_BORDER_COLOR);
            g2.setStroke(new BasicStroke(8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)); // Độ dày viền chữ
            g2.draw(shape);

            // 3. Đổ màu ruột chữ đè lên trên viền
            g2.setColor(messageColor);
            g2.drawString(endMessage, textX, textY);
        }
        g2.dispose();
    }

    private void paintHighlights(Graphics2D g2, int startX, int startY, int cellSize) {
        if (hoverRow >= 0 && model.canPlayAt(hoverRow, hoverCol)) {
            g2.setColor(HOVER_COLOR);
            g2.fillRoundRect(
                    startX + hoverCol * cellSize + 2,
                    startY + hoverRow * cellSize + 2,
                    cellSize - 4,
                    cellSize - 4,
                    12,
                    12);
        }

        CellPosition lastMove = model.lastMove();
        if (lastMove != null) {
            g2.setColor(LAST_MOVE_COLOR);
            g2.fillRoundRect(
                    startX + lastMove.col() * cellSize + 3,
                    startY + lastMove.row() * cellSize + 3,
                    cellSize - 6,
                    cellSize - 6,
                    14,
                    14);
        }

        List<CellPosition> winningLine = model.winningLine();
        if (!winningLine.isEmpty()) {
            g2.setColor(WIN_HIGHLIGHT);
            for (CellPosition position : winningLine) {
                g2.fillRoundRect(
                        startX + position.col() * cellSize + 2,
                        startY + position.row() * cellSize + 2,
                        cellSize - 4,
                        cellSize - 4,
                        14,
                        14);
            }
        }
    }

    private void paintX(Graphics2D g2, int x, int y, int cellSize) {
        g2.setColor(X_COLOR);
        g2.setStroke(new BasicStroke(Math.max(3.2f, cellSize / 9f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int inset = Math.max(10, cellSize / 5);
        g2.drawLine(x + inset, y + inset, x + cellSize - inset, y + cellSize - inset);
        g2.drawLine(x + inset, y + cellSize - inset, x + cellSize - inset, y + inset);
    }

    private void paintO(Graphics2D g2, int x, int y, int cellSize) {
        g2.setColor(O_COLOR);
        g2.setStroke(new BasicStroke(Math.max(3.2f, cellSize / 9f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int inset = Math.max(10, cellSize / 5);
        g2.drawOval(x + inset, y + inset, cellSize - inset * 2, cellSize - inset * 2);
    }

    private void updateHover(Point point) {
        Point cell = toCell(point);
        if (hoverRow != cell.x || hoverCol != cell.y) {
            hoverRow = cell.x;
            hoverCol = cell.y;
            repaint();
        }
    }

    private Point toCell(Point point) {
        int size = model.boardSize();
        int padding = 28;
        int available = Math.min(getWidth(), getHeight()) - padding * 2;
        int cellSize = Math.max(1, available / size);
        int boardPixels = cellSize * size;
        int startX = (getWidth() - boardPixels) / 2;
        int startY = (getHeight() - boardPixels) / 2;
        int col = (point.x - startX) / cellSize;
        int row = (point.y - startY) / cellSize;
        if (row < 0 || row >= size || col < 0 || col >= size) {
            return new Point(-1, -1);
        }
        return new Point(row, col);
    }
}
