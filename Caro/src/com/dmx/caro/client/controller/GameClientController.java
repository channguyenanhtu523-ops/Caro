package com.dmx.caro.client.controller;

import com.dmx.caro.client.model.ChatLine;
import com.dmx.caro.client.model.ClientGameModel;
import com.dmx.caro.client.net.ClientConnection;
import com.dmx.caro.client.view.MainWindow;
import com.dmx.caro.common.config.AppConfig;
import com.dmx.caro.common.model.GameSnapshot;
import com.dmx.caro.common.model.PlayerSymbol;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.SwingUtilities;

public final class GameClientController implements ClientEventListener, AutoCloseable {
    private final AppConfig config;
    private final ClientGameModel model;
    private final MainWindow view;
    private final ExecutorService worker;

    private ClientConnection connection;

    public GameClientController(AppConfig config, ClientGameModel model, MainWindow view) {
        this.config = config;
        this.model = model;
        this.view = view;
        this.worker = Executors.newSingleThreadExecutor();
    }

    public void initialize() {
        view.bindConnectAction(this::connect);
        view.bindMoveAction(this::playMove);
        view.bindSendChatAction(this::sendChat);
        view.bindRematchAction(this::requestRematch);
        view.showLoginTab();
        view.refresh();
    }

    private void connect() {
        String username = view.username();
        if (username.isBlank()) {
            view.showError("Vui lòng nhập tên người chơi.");
            return;
        }

        view.setConnecting(true);
        worker.submit(() -> {
            try {
                if (connection != null) {
                    connection.close();
                }
                ClientConnection newConnection = new ClientConnection(config, this);
                newConnection.connectAndLogin(config.serverHost(), config.serverPort(), username);
                connection = newConnection;
            } catch (Exception exception) {
                runOnEdt(() -> {
                    model.markDisconnected("Kết nối thất bại");
                    view.setConnecting(false);
                    view.showError(exception.getMessage());
                    view.showLoginTab();
                    view.refresh();
                });
            }
        });
    }

    private void playMove(int row, int col) {
        ClientConnection activeConnection = connection;
        if (activeConnection == null) {
            return;
        }

        worker.submit(() -> {
            try {
                activeConnection.sendMove(model.sessionId(), row, col, model.moveCount() + 1);
            } catch (Exception exception) {
                onError("Không gửi được nước đi: " + exception.getMessage());
            }
        });
    }

    private void sendChat() {
        String text = view.chatText();
        if (text.isBlank()) {
            return;
        }
        ClientConnection activeConnection = connection;
        if (activeConnection == null) {
            onError("Bạn chưa kết nối.");
            return;
        }
        if (text.length() > activeConnection.maxChatLength()) {
            onError("Tin nhắn vượt quá độ dài cho phép.");
            return;
        }

        view.clearChatInput();
        worker.submit(() -> {
            try {
                activeConnection.sendChat(model.sessionId(), model.username(), text);
            } catch (Exception exception) {
                onError("Không gửi được tin nhắn: " + exception.getMessage());
            }
        });
    }

    private void requestRematch() {
        ClientConnection activeConnection = connection;
        if (activeConnection == null) {
            onError("Bạn chưa kết nối.");
            return;
        }

        view.setRematchEnabled(false);
        worker.submit(() -> {
            try {
                activeConnection.requestRematch();
            } catch (Exception exception) {
                runOnEdt(() -> {
                    view.setRematchEnabled(true);
                    onError("Không thể ghép trận lại: " + exception.getMessage());
                });
            }
        });
    }

    @Override
    public void onConnected(int boardSize, int winLength, int maxChatLength) {
        runOnEdt(() -> {
            model.setServerSettings(boardSize, winLength);
            model.prepareLobby();
            model.markConnected(true, "Đang xác nhận đăng nhập...");
            view.showGameTab();
            view.refresh();
        });
    }

    @Override
    public void onLoginAccepted(String username, String message) {
        runOnEdt(() -> {
            model.setUsername(username);
            model.markConnected(true, message);
            view.showGameTab();
            view.refresh();
        });
    }

    @Override
    public void onRematchQueued(String message) {
        runOnEdt(() -> {
            model.prepareLobby();
            model.markConnected(true, message);
            view.showLobbyView();
            view.refresh();
        });
    }

    @Override
    public void onMatchFound(String opponent, String sessionId, PlayerSymbol mySymbol, int boardSize, int winLength) {
        runOnEdt(() -> {
            model.prepareMatch(sessionId, opponent, boardSize, winLength, mySymbol);
            model.markConnected(true, "Đã ghép trận với " + opponent + ".");
            view.showMatchView();
            view.focusChatInput();
            view.refresh();
        });
    }

    @Override
    public void onGameSnapshot(GameSnapshot snapshot, String statusMessage) {
        runOnEdt(() -> {
            model.applySnapshot(snapshot, statusMessage);
            view.refresh();
        });
    }

    @Override
    public void onChat(ChatLine line) {
        runOnEdt(() -> view.appendChat(line));
    }

    @Override
    public void onError(String message) {
        runOnEdt(() -> {
            model.markConnected(model.connected(), message);
            view.refresh();
            view.showError(message);
        });
    }

    @Override
    public void onDisconnected(String message) {
        runOnEdt(() -> {
            model.markDisconnected(message);
            connection = null;
            view.setConnecting(false);
            view.setRematchEnabled(false);
            view.showLoginTab();
            view.refresh();
        });
    }

    @Override
    public void close() {
        if (connection != null) {
            connection.close();
        }
        worker.shutdownNow();
    }

    private void runOnEdt(Runnable action) {
        if (SwingUtilities.isEventDispatchThread()) {
            action.run();
        } else {
            SwingUtilities.invokeLater(action);
        }
    }
}
