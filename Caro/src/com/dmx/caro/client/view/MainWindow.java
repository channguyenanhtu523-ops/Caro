package com.dmx.caro.client.view;

import com.dmx.caro.client.model.ChatLine;
import com.dmx.caro.client.model.ClientGameModel;
import com.dmx.caro.common.model.GameStatus;
import com.dmx.caro.common.model.PlayerSymbol;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.BiConsumer;
import java.util.stream.Stream;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicScrollBarUI;

public final class MainWindow extends JFrame {
    private static final String LOGIN_CARD = "login";
    private static final String GAME_CARD = "game";
    private final ClientGameModel model;
    private final BoardPanel boardPanel;
    private final JTextField usernameField;
    private final JButton connectButton;
    private final JLabel turnLabel;
    private final JLabel sessionLabel;
    private final JLabel playerLabel;
    private final JLabel opponentLabel;
    private final JLabel statusLabel;
    private final JTextArea chatArea;
    private final JTextField chatInput;
    private final JButton sendButton;
    private final JButton rematchButton;
    private final JButton emojiButton;
    private final JTabbedPane tabbedPane;
    private final JPanel mainCards;
    private final CardLayout cardLayout;
    private final NavButton loginNavButton;
    private final NavButton gameNavButton;

    // --- BIẾN ĐIỀU KHIỂN GIAO DIỆN ĐEN / TRẮNG ĐỘNG ---
    private static boolean isDarkMode = true;
    private final JButton themeToggleButton;
    private final JLabel logoText;
    private final JLabel chatTitleLabel;
    private final JLabel gameTitleLabel;
    private final JLabel loginTitleLabel;
    private final JLabel loginSubtitleLabel;
    private final JLabel hintLabel;
    private final JLabel lobbyTitleLabel;
    private final JLabel lobbySubtitleLabel;
    private final java.util.List<JLabel> infoKeyLabels = new java.util.ArrayList<>();

    public MainWindow(ClientGameModel model) {
        super("Caro Online Multiplayer");
        this.model = model;
        this.boardPanel = new BoardPanel(model);

        this.loginNavButton = new NavButton("Đăng nhập");
        this.gameNavButton = new NavButton("Chơi game");

        this.usernameField = new ModernTextField("Player" + (System.currentTimeMillis() % 1000), 16);
        this.connectButton = createPrimaryButton("Vào phòng");
        this.turnLabel = createValueLabel();
        this.sessionLabel = createValueLabel();
        this.playerLabel = createValueLabel();
        this.opponentLabel = createValueLabel();
        this.statusLabel = createValueLabel();
        this.chatArea = new JTextArea();
        this.chatInput = new ModernTextField("", 16);
        this.sendButton = createPrimaryButton("Gửi");
        this.rematchButton = createSecondaryButton("Ghép trận lại");

        this.emojiButton = createSecondaryButton("☺");
        this.emojiButton.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));

        this.themeToggleButton = createSecondaryButton("🌙");
        this.themeToggleButton.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        this.themeToggleButton.setPreferredSize(new Dimension(42, 36));
        this.themeToggleButton.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        this.logoText = new JLabel("CARO ONLINE");
        this.chatTitleLabel = new JLabel("Trò chuyện");
        this.gameTitleLabel = new JLabel("Cờ Caro Trực Tuyến");
        this.loginTitleLabel = new JLabel("Cờ Caro Trực Tuyến");
        this.loginSubtitleLabel = new JLabel("Nhập tên người chơi để vào phòng đấu mạng trực tuyến.");
        this.hintLabel = new JLabel("Đang chờ ghép trận sau khi đăng nhập thành công.");
        this.lobbyTitleLabel = new JLabel("Đang đợi đối thủ...");
        this.lobbySubtitleLabel = new JLabel("Khi hệ thống tìm thấy đối thủ, bạn sẽ vào bàn cờ ngay.");

        this.tabbedPane = new CleanTabbedPane();
        this.cardLayout = new CardLayout();
        this.mainCards = new JPanel(cardLayout);

        initialize();
    }

    private void initialize() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1220, 820));
        setLocationByPlatform(true);

        updateThemeUIComponents();

        JPanel root = new GradientPanel();
        root.setLayout(new BorderLayout(14, 14));
        root.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        JPanel navBar = buildNavBar();
        root.add(navBar, BorderLayout.NORTH);

        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 13));
        tabbedPane.addTab("Đăng nhập", buildLoginTab());
        tabbedPane.addTab("Chơi game", buildGameTab());
        tabbedPane.setEnabledAt(1, false);

        loginNavButton.addActionListener(e -> tabbedPane.setSelectedIndex(0));
        gameNavButton.addActionListener(e -> tabbedPane.setSelectedIndex(1));

        themeToggleButton.addActionListener(e -> {
            isDarkMode = !isDarkMode;
            themeToggleButton.setText(isDarkMode ? "🌙" : "☀️");
            updateThemeUIComponents();

            repaint();
            root.revalidate();
        });

        emojiButton.addActionListener(e -> {
            if (!chatInput.isEnabled()) return;

            javax.swing.JPopupMenu emojiMenu = new javax.swing.JPopupMenu();
            emojiMenu.setBackground(isDarkMode ? new Color(30, 31, 34) : Color.WHITE);
            emojiMenu.setBorder(BorderFactory.createLineBorder(isDarkMode ? new Color(79, 84, 92) : new Color(210, 214, 219)));

            JPanel gridPanel = new JPanel(new java.awt.GridLayout(0, 4, 6, 6));
            gridPanel.setOpaque(false);
            gridPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

            String[] emojis = {"😊", "😂", "🤣", "❤️", "😮", "😡", "👍", "👎", "🔥", "👑", "❌", "⭕"};

            for (String emoji : emojis) {
                JButton btn = new JButton(emoji);
                btn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));
                btn.setForeground(isDarkMode ? Color.WHITE : new Color(43, 45, 49));
                btn.setBackground(isDarkMode ? new Color(43, 45, 49) : new Color(240, 242, 245));
                btn.setContentAreaFilled(true);
                btn.setFocusPainted(false);
                btn.setBorderPainted(false);
                btn.setMargin(new java.awt.Insets(0, 0, 0, 0));
                btn.setPreferredSize(new Dimension(46, 42));
                btn.addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override
                    public void mouseEntered(java.awt.event.MouseEvent event) {
                        btn.setBackground(new Color(88, 101, 242));
                        btn.setForeground(Color.WHITE);
                    }
                    @Override
                    public void mouseExited(java.awt.event.MouseEvent event) {
                        btn.setBackground(isDarkMode ? new Color(43, 45, 49) : new Color(240, 242, 245));
                        btn.setForeground(isDarkMode ? Color.WHITE : new Color(43, 45, 49));
                    }
                });

                btn.addActionListener(ae -> {
                    chatInput.replaceSelection(emoji);
                    chatInput.requestFocusInWindow();
                    emojiMenu.setVisible(false);
                });

                gridPanel.add(btn);
            }

            emojiMenu.add(gridPanel);
            emojiMenu.show(emojiButton, -6, -emojiMenu.getPreferredSize().height);
        });

        tabbedPane.addChangeListener(e -> syncNavBar());
        syncNavBar();

        root.add(tabbedPane, BorderLayout.CENTER);
        setContentPane(root);
    }

    private void updateThemeUIComponents() {
        Color mainText = isDarkMode ? new Color(242, 243, 245) : new Color(43, 45, 49);
        Color mutedText = isDarkMode ? new Color(148, 155, 164) : new Color(110, 118, 125);
        Color chatBg = isDarkMode ? new Color(30, 31, 34) : new Color(245, 246, 248);

        UIManager.put("TabbedPane.selected", isDarkMode ? new Color(43, 45, 49) : new Color(240, 242, 245));
        UIManager.put("TabbedPane.background", isDarkMode ? new Color(30, 31, 34) : new Color(240, 242, 245));
        UIManager.put("TabbedPane.foreground", mainText);

        logoText.setForeground(mainText);
        chatTitleLabel.setForeground(mainText);
        gameTitleLabel.setForeground(mainText);
        loginTitleLabel.setForeground(mainText);
        lobbyTitleLabel.setForeground(mainText);

        loginSubtitleLabel.setForeground(mutedText);
        hintLabel.setForeground(mutedText);
        lobbySubtitleLabel.setForeground(mutedText);

        chatArea.setBackground(chatBg);
        chatArea.setForeground(mainText);

        turnLabel.setForeground(mainText);
        sessionLabel.setForeground(mainText);
        playerLabel.setText(model.username().isBlank() ? "-" : model.username() + symbolSuffix());
        playerLabel.setForeground(mainText);
        opponentLabel.setForeground(mainText);

        infoKeyLabels.forEach(lbl -> lbl.setForeground(mutedText));
    }

    private JPanel buildNavBar() {
        JPanel navBar = new JPanel(new BorderLayout());
        navBar.setOpaque(false);
        navBar.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));

        JPanel logoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        logoPanel.setOpaque(false);

        logoText.setFont(new Font("Segoe UI", Font.BOLD, 16));

        JLabel dot = new JLabel("●");
        dot.setFont(new Font("Segoe UI", Font.BOLD, 14));
        dot.setForeground(new Color(35, 165, 90));
        logoPanel.add(logoText);
        logoPanel.add(dot);

        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        buttonsPanel.setOpaque(false);
        buttonsPanel.add(themeToggleButton);
        buttonsPanel.add(loginNavButton);
        buttonsPanel.add(gameNavButton);

        navBar.add(logoPanel, BorderLayout.WEST);
        navBar.add(buttonsPanel, BorderLayout.EAST);

        return navBar;
    }

    private void syncNavBar() {
        int selected = tabbedPane.getSelectedIndex();
        loginNavButton.setSelected(selected == 0);
        gameNavButton.setSelected(selected == 1);

        loginNavButton.setEnabled(tabbedPane.isEnabledAt(0));
        gameNavButton.setEnabled(tabbedPane.isEnabledAt(1));
    }

    private JPanel buildLoginTab() {
        JPanel container = new JPanel(new GridBagLayout());
        container.setOpaque(false);

        ModernCardPanel panel = new ModernCardPanel();
        panel.setLayout(new GridBagLayout());
        panel.setPreferredSize(new Dimension(560, 350));
        panel.setBorder(BorderFactory.createEmptyBorder(28, 32, 28, 32));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;

        loginTitleLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        panel.add(loginTitleLabel, gbc);

        gbc.gridy++;
        loginSubtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        panel.add(loginSubtitleLabel, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(22, 8, 6, 8);
        panel.add(createCaptionLabel("Tên người chơi của bạn"), gbc);

        gbc.gridy++;
        gbc.insets = new Insets(6, 8, 8, 8);
        panel.add(usernameField, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(20, 8, 8, 8);
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(connectButton, gbc);

        container.add(panel);
        return container;
    }

    private JPanel buildGameTab() {
        JPanel panel = new JPanel(new BorderLayout(18, 18));
        panel.setOpaque(false);
        panel.add(buildGameHeader(), BorderLayout.NORTH);
        mainCards.setOpaque(false);
        mainCards.add(buildLobbyCard(), LOGIN_CARD);
        mainCards.add(buildGameContent(), GAME_CARD);
        panel.add(mainCards, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildGameHeader() {
        JPanel panel = new ModernCardPanel();
        panel.setOpaque(false);
        panel.setLayout(new BorderLayout(16, 0));
        panel.setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));

        gameTitleLabel.setFont(new Font("Segoe UI", Font.BOLD, 26));

        hintLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        hintLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        panel.add(gameTitleLabel, BorderLayout.WEST);
        panel.add(hintLabel, BorderLayout.EAST);
        return panel;
    }

    private JPanel buildLobbyCard() {
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setOpaque(false);

        ModernCardPanel panel = new ModernCardPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setPreferredSize(new Dimension(560, 260));
        panel.setBorder(BorderFactory.createEmptyBorder(28, 32, 28, 32));

        lobbyTitleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lobbyTitleLabel.setAlignmentX(LEFT_ALIGNMENT);

        lobbySubtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lobbySubtitleLabel.setAlignmentX(LEFT_ALIGNMENT);

        JPanel statusRow = new JPanel(new BorderLayout(12, 0));
        statusRow.setOpaque(false);
        statusRow.setAlignmentX(LEFT_ALIGNMENT);
        statusRow.add(createInfoTitle("Người chơi"), BorderLayout.WEST);
        statusRow.add(playerLabel, BorderLayout.CENTER);

        JPanel sessionRow = new JPanel(new BorderLayout(12, 0));
        sessionRow.setOpaque(false);
        sessionRow.setAlignmentX(LEFT_ALIGNMENT);
        sessionRow.add(createInfoTitle("Trạng thái"), BorderLayout.WEST);
        sessionRow.add(statusLabel, BorderLayout.CENTER);

        panel.add(lobbyTitleLabel);
        panel.add(Box.createVerticalStrut(10));
        panel.add(lobbySubtitleLabel);
        panel.add(Box.createVerticalStrut(28));
        panel.add(statusRow);
        panel.add(Box.createVerticalStrut(14));
        panel.add(sessionRow);

        wrapper.add(panel);
        return wrapper;
    }

    private JPanel buildGameContent() {
        JPanel content = new JPanel(new BorderLayout(18, 18));
        content.setOpaque(false);

        ModernCardPanel left = new ModernCardPanel();
        left.setLayout(new BorderLayout());
        left.add(boardPanel, BorderLayout.CENTER);

        ModernCardPanel right = new ModernCardPanel();
        right.setLayout(new BorderLayout(0, 12));
        right.setPreferredSize(new Dimension(360, 0));
        right.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        right.add(buildSidebarTop(), BorderLayout.NORTH);
        right.add(buildChatPanel(), BorderLayout.CENTER);

        content.add(left, BorderLayout.CENTER);
        content.add(right, BorderLayout.EAST);
        return content;
    }

    private JPanel buildSidebarTop() {
        JPanel panel = new JPanel(new BorderLayout(0, 14));
        panel.setOpaque(false);
        panel.add(buildInfoPanel(), BorderLayout.CENTER);

        JPanel actionRow = new JPanel(new BorderLayout());
        actionRow.setOpaque(false);
        rematchButton.setEnabled(false);
        actionRow.add(rematchButton, BorderLayout.EAST);
        panel.add(actionRow, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildInfoPanel() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(createInfoRow("Bạn", playerLabel));
        panel.add(Box.createVerticalStrut(10));
        panel.add(createInfoRow("Đối thủ", opponentLabel));
        panel.add(Box.createVerticalStrut(10));
        panel.add(createInfoRow("Phiên đấu", sessionLabel));
        panel.add(Box.createVerticalStrut(10));
        panel.add(createInfoRow("Lượt đi", turnLabel));
        panel.add(Box.createVerticalStrut(10));
        panel.add(createInfoRow("Trạng thái", statusLabel));
        return panel;
    }

    private JPanel buildChatPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setOpaque(false);

        chatTitleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));

        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatArea.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JScrollPane scrollPane = new JScrollPane(chatArea);
        scrollPane.getViewport().setOpaque(false);

        scrollPane.getVerticalScrollBar().setUI(new ModernScrollBarUI());
        scrollPane.getHorizontalScrollBar().setUI(new ModernScrollBarUI());

        JPanel inputPanel = new JPanel(new BorderLayout(8, 0));
        inputPanel.setOpaque(false);

        inputPanel.add(emojiButton, BorderLayout.WEST);
        inputPanel.add(chatInput, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        panel.add(chatTitleLabel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(inputPanel, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createInfoRow(String label, JLabel value) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        JLabel keyLabel = createInfoTitle(label);
        value.setHorizontalAlignment(SwingConstants.RIGHT);
        value.setFont(new Font("Segoe UI", Font.BOLD, 14));
        row.add(keyLabel, BorderLayout.WEST);
        row.add(value, BorderLayout.CENTER);
        return row;
    }

    private JLabel createInfoTitle(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 13));
        infoKeyLabels.add(label);
        return label;
    }

    private JLabel createCaptionLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 12));
        infoKeyLabels.add(label);
        return label;
    }

    private JLabel createValueLabel() {
        JLabel label = new JLabel("-");
        label.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        return label;
    }

    private JButton createPrimaryButton(String text) {
        return new ModernButton(
                text,
                new Color(88, 101, 242),
                new Color(71, 82, 196),
                Color.WHITE
        );
    }

    private JButton createSecondaryButton(String text) {
        return new ModernButton(
                text,
                new Color(79, 84, 92),
                new Color(60, 63, 65),
                new Color(242, 243, 245)
        );
    }

    public void bindConnectAction(Runnable action) {
        connectButton.addActionListener(event -> action.run());
        usernameField.addActionListener(event -> action.run());
    }

    public void bindMoveAction(BiConsumer<Integer, Integer> action) {
        boardPanel.setMoveListener(action);
    }

    public void bindSendChatAction(Runnable action) {
        sendButton.addActionListener(event -> action.run());
        chatInput.addActionListener(event -> action.run());
    }

    public void bindRematchAction(Runnable action) {
        rematchButton.addActionListener(event -> action.run());
    }

    public String username() {
        return usernameField.getText().trim();
    }

    public String chatText() {
        return chatInput.getText().trim();
    }

    public void clearChatInput() {
        chatInput.setText("");
    }

    public void setConnecting(boolean connecting) {
        connectButton.setEnabled(!connecting);
        usernameField.setEnabled(!connecting);
    }

    public void showLoginTab() {
        tabbedPane.setSelectedIndex(0);
        tabbedPane.setEnabledAt(1, false);
        cardLayout.show(mainCards, LOGIN_CARD);
        chatInput.setEnabled(false);
        sendButton.setEnabled(false);
        emojiButton.setEnabled(false);
        rematchButton.setEnabled(false);
        usernameField.requestFocusInWindow();
    }

    public void showGameTab() {
        tabbedPane.setEnabledAt(1, true);
        tabbedPane.setSelectedIndex(1);
        showLobbyView();
    }

    public void showLobbyView() {
        cardLayout.show(mainCards, LOGIN_CARD);
        chatInput.setEnabled(false);
        sendButton.setEnabled(false);
        emojiButton.setEnabled(false);
        rematchButton.setEnabled(false);
    }

    public void showMatchView() {
        cardLayout.show(mainCards, GAME_CARD);
        chatInput.setEnabled(model.connected());
        sendButton.setEnabled(model.connected());
        emojiButton.setEnabled(model.connected());
    }

    public void setRematchEnabled(boolean enabled) {
        rematchButton.setEnabled(enabled);
    }

    public void focusChatInput() {
        chatInput.requestFocusInWindow();
    }

    public void appendChat(ChatLine line) {
        if (!chatArea.getText().isBlank()) {
            chatArea.append("\n");
        }
        chatArea.append("[" + line.timestamp() + "] " + line.sender() + ": " + line.text());
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }

    public void refresh() {
        playerLabel.setText(model.username().isBlank() ? "-" : model.username() + symbolSuffix());
        opponentLabel.setText(model.opponent().isBlank() ? "Đang chờ..." : model.opponent());
        sessionLabel.setText(model.sessionId().isBlank() ? "-" : shorten(model.sessionId()));
        turnLabel.setText(turnText());
        statusLabel.setText(model.statusMessage());
        statusLabel.setForeground(statusColor());
        if (model.connected()) {
            tabbedPane.setEnabledAt(1, true);
            if (model.sessionId().isBlank()) {
                showLobbyView();
            } else {
                showMatchView();
            }
        } else {
            tabbedPane.setEnabledAt(1, false);
            cardLayout.show(mainCards, LOGIN_CARD);
            chatInput.setEnabled(false);
            sendButton.setEnabled(false);
            emojiButton.setEnabled(false);
            rematchButton.setEnabled(false);
        }
        rematchButton.setEnabled(model.connected() && canRequestRematch());
        syncNavBar();
        repaint();
    }

    private String symbolSuffix() {
        PlayerSymbol symbol = model.mySymbol();
        return symbol == null ? "" : " (" + symbol.name() + ")";
    }

    private String turnText() {
        if (!model.connected()) {
            return "Ngoại tuyến";
        }
        if (model.status() != GameStatus.IN_PROGRESS) {
            return switch (model.status()) {
                case WAITING -> "Đang chờ";
                case WON -> "Kết thúc";
                case DRAW -> "Hòa";
                case DISCONNECTED -> "Ngắt kết nối";
                case IN_PROGRESS -> "";
            };
        }
        return model.isMyTurn() ? "Đến lượt bạn" : "Đến lượt đối thủ";
    }

    private Color statusColor() {
        return switch (model.status()) {
            case WON -> new Color(35, 165, 90);
            case DRAW -> new Color(148, 155, 164);
            case DISCONNECTED -> new Color(242, 63, 67);
            default -> isDarkMode ? new Color(242, 243, 245) : new Color(43, 45, 49);
        };
    }

    private boolean canRequestRematch() {
        return !model.sessionId().isBlank() && switch (model.status()) {
            case WON, DRAW, DISCONNECTED -> true;
            default -> false;
        };
    }

    private String shorten(String sessionId) {
        return sessionId.length() <= 8 ? sessionId : sessionId.substring(0, 8).toUpperCase();
    }

    public void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Caro Client", JOptionPane.ERROR_MESSAGE);
    }

    private static final class CleanTabbedPane extends JTabbedPane {
        public CleanTabbedPane() {
            setOpaque(false);
            setUI(new javax.swing.plaf.basic.BasicTabbedPaneUI() {
                @Override
                protected int calculateTabHeight(int tabPlacement, int tabIndex, int fontHeight) {
                    return 0;
                }
                @Override
                protected void paintTabArea(Graphics g, int tabPlacement, int selectedIndex) {
                }
                @Override
                protected void paintContentBorder(Graphics g, int tabPlacement, int selectedIndex) {
                }
            });
        }
    }

    private static final class NavButton extends JButton {
        private final Color activeBg = new Color(88, 101, 242);
        private boolean isSelected = false;
        private boolean isHovered = false;

        public NavButton(String text) {
            super(text);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setFont(new Font("Segoe UI", Font.BOLD, 13));
            setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    isHovered = true;
                    repaint();
                }
                @Override
                public void mouseExited(MouseEvent e) {
                    isHovered = false;
                    repaint();
                }
            });
        }

        public void setSelected(boolean selected) {
            this.isSelected = selected;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Color inactiveBg = isDarkMode ? new Color(43, 45, 49) : new Color(225, 228, 233);
            Color hoverBg = isDarkMode ? new Color(71, 82, 196) : new Color(210, 214, 219);

            if (!isEnabled()) {
                g2.setColor(isDarkMode ? new Color(35, 36, 40) : new Color(240, 242, 245));
                setForeground(isDarkMode ? new Color(90, 90, 90) : new Color(180, 185, 190));
            } else if (isSelected) {
                g2.setColor(activeBg);
                setForeground(Color.WHITE);
            } else {
                g2.setColor(isHovered ? hoverBg : inactiveBg);
                setForeground(isDarkMode ? new Color(148, 155, 164) : new Color(43, 45, 49));
            }
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static final class ModernCardPanel extends JPanel {
        public ModernCardPanel() {
            setOpaque(false);
        }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(isDarkMode ? new Color(43, 45, 49) : Color.WHITE);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);

            g2.setColor(isDarkMode ? new Color(79, 84, 92, 140) : new Color(218, 222, 228));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static final class GradientPanel extends JPanel {
        private GradientPanel() {
            setOpaque(false);
        }
        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            Color colorStart = isDarkMode ? new Color(30, 31, 34) : new Color(240, 242, 245);
            Color colorEnd = isDarkMode ? new Color(20, 20, 22) : new Color(248, 249, 250);

            GradientPaint paint = new GradientPaint(0, 0, colorStart, getWidth(), getHeight(), colorEnd);
            g2.setPaint(paint);
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.dispose();
            super.paintComponent(graphics);
        }
    }

    private static final class ModernButton extends JButton {
        private final Color baseColor;
        private final Color hoverColor;
        private boolean isHovered = false;

        public ModernButton(String text, Color baseColor, Color hoverColor, Color textColor) {
            super(text);
            this.baseColor = baseColor;
            this.hoverColor = hoverColor;
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setForeground(textColor);
            setFont(new Font("Segoe UI", Font.BOLD, 14));
            setBorder(BorderFactory.createEmptyBorder(10, 24, 10, 24));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    isHovered = true;
                    repaint();
                }
                @Override
                public void mouseExited(MouseEvent e) {
                    isHovered = false;
                    repaint();
                }
            });
        }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (!isEnabled()) {
                g2.setColor(isDarkMode ? new Color(55, 57, 62) : new Color(225, 228, 233));
            } else {
                if (baseColor.getRGB() == new Color(79, 84, 92).getRGB() && !isDarkMode) {
                    g2.setColor(isHovered ? new Color(200, 205, 212) : new Color(225, 228, 233));
                    setForeground(new Color(43, 45, 49));
                } else {
                    g2.setColor(isHovered ? hoverColor : baseColor);
                    setForeground(Color.WHITE);
                }
            }
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static final class ModernTextField extends JTextField {
        private boolean isFocused = false;
        public ModernTextField(String text, int columns) {
            super(text, columns);
            setOpaque(false);
            setFont(new Font("Segoe UI", Font.PLAIN, 14));
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createEmptyBorder(2, 2, 2, 2),
                    BorderFactory.createEmptyBorder(10, 14, 10, 14)));
            addFocusListener(new FocusListener() {
                @Override
                public void focusGained(FocusEvent e) {
                    isFocused = true;
                    repaint();
                }
                @Override
                public void focusLost(FocusEvent e) {
                    isFocused = false;
                    repaint();
                }
            });
        }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            setForeground(isDarkMode ? new Color(242, 243, 245) : new Color(43, 45, 49));
            setCaretColor(isDarkMode ? new Color(242, 243, 245) : new Color(43, 45, 49));

            g2.setColor(isDarkMode ? new Color(30, 31, 34) : new Color(245, 246, 248));
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);

            if (isFocused) {
                g2.setColor(new Color(88, 101, 242));
                g2.setStroke(new java.awt.BasicStroke(1.5f));
            } else {
                g2.setColor(isDarkMode ? new Color(79, 84, 92) : new Color(210, 214, 219));
                g2.setStroke(new java.awt.BasicStroke(1f));
            }
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static final class ModernScrollBarUI extends BasicScrollBarUI {
        protected void configureScrollBarParameters() {
            scrollbar.setPreferredSize(new Dimension(8, 0));
            scrollbar.setOpaque(false);
        }
        @Override
        protected void paintTrack(Graphics g, javax.swing.JComponent c, java.awt.Rectangle trackBounds) {
        }
        @Override
        protected void paintThumb(Graphics g, javax.swing.JComponent c, java.awt.Rectangle thumbBounds) {
            if (thumbBounds.isEmpty() || !scrollbar.isEnabled()) return;
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(isDarkMode ? new Color(148, 155, 164, 100) : new Color(160, 168, 176, 120));
            g2.fillRoundRect(thumbBounds.x, thumbBounds.y, thumbBounds.width, thumbBounds.height, 6, 6);
            g2.dispose();
        }
        @Override
        protected JButton createDecreaseButton(int orientation) {
            return createZeroButton();
        }
        @Override
        protected JButton createIncreaseButton(int orientation) {
            return createZeroButton();
        }
        private JButton createZeroButton() {
            JButton button = new JButton();
            button.setPreferredSize(new Dimension(0, 0));
            button.setMinimumSize(new Dimension(0, 0));
            button.setMaximumSize(new Dimension(0, 0));
            return button;
        }
    }
}