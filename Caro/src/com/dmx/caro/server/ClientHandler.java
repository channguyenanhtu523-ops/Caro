package com.dmx.caro.server;

import com.dmx.caro.common.config.AppConfig;
import com.dmx.caro.common.crypto.CryptoEngine;
import com.dmx.caro.common.crypto.SessionKeys;
import com.dmx.caro.common.model.GameSnapshot;
import com.dmx.caro.common.model.GameStatus;
import com.dmx.caro.common.model.PlayerSymbol;
import com.dmx.caro.common.net.SecureChannel;
import com.dmx.caro.common.protocol.MessageType;
import com.dmx.caro.common.protocol.Messages;
import com.dmx.caro.common.protocol.NetworkMessage;
import com.dmx.caro.common.protocol.ProtocolException;
import java.io.IOException;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ClientHandler implements Runnable {
    private final Socket socket;
    private final GameServer server;
    private final AppConfig config;
    private final KeyPair serverKeyPair;
    private final SecureChannel channel;
    private final AtomicBoolean closed;

    private volatile String username;
    private volatile GameRoom room;
    private volatile long lastHeartbeatAt;
    private volatile boolean loggedIn;
    private volatile boolean encryptionReady;

    public ClientHandler(Socket socket, GameServer server, AppConfig config, KeyPair serverKeyPair) throws IOException {
        this.socket = socket;
        this.server = server;
        this.config = config;
        this.serverKeyPair = serverKeyPair;
        this.channel = new SecureChannel(socket);
        this.closed = new AtomicBoolean(false);
        this.lastHeartbeatAt = System.currentTimeMillis();
    }

    @Override
    public void run() {
        try {
            performHandshake();
            performLogin();
            server.enqueueForMatchmaking(this);

            while (!closed.get()) {
                NetworkMessage message = channel.readMessage();
                handleMessage(message);
            }
        } catch (IOException | GeneralSecurityException | ProtocolException exception) {
            if (!closed.get()) {
                System.err.println("Connection closed for " + safeUsername() + ": " + exception.getMessage());
            }
        } finally {
            cleanup();
        }
    }

    public boolean isReadyForMatch() {
        return loggedIn && room == null && !closed.get();
    }

    public void attachRoom(GameRoom room) {
        this.room = room;
    }

    public void onMatchFound() {
        GameRoom activeRoom = room;
        if (activeRoom == null) {
            return;
        }

        try {
            PlayerSymbol symbol = activeRoom.symbolOf(this);
            String opponent = activeRoom.opponentOf(this).username();
            sendSecure(Messages.matchFound(
                    activeRoom.sessionId(),
                    symbol,
                    opponent,
                    activeRoom.boardSize(),
                    activeRoom.winLength()));
        } catch (Exception exception) {
            closeSilently();
        }
    }

    public void broadcastInitialSnapshot() {
        GameRoom activeRoom = room;
        if (activeRoom == null) {
            return;
        }
        try {
            broadcastSnapshot(activeRoom.snapshot("Match started."));
        } catch (Exception exception) {
            closeSilently();
        }
    }

    public String username() {
        return username;
    }

    private void performHandshake() throws IOException, ProtocolException, GeneralSecurityException {
        NetworkMessage hello = channel.readMessage();
        ensureType(hello, MessageType.HELLO);

        channel.sendPlain(Messages.serverHello(
                "Caro Online Server",
                CryptoEngine.encodePublicKey(serverKeyPair.getPublic()),
                config));

        NetworkMessage keyExchange = channel.readMessage();
        ensureType(keyExchange, MessageType.KEY_EXCHANGE);
        SessionKeys sessionKeys = CryptoEngine.decryptSessionKeys(
                keyExchange.required("bundle"),
                serverKeyPair.getPrivate());
        channel.setSessionKeys(sessionKeys);
        encryptionReady = true;
        sendSecure(Messages.keyAck("Secure channel established."));
    }

    private void performLogin() throws IOException, ProtocolException, GeneralSecurityException {
        NetworkMessage login = channel.readMessage();
        ensureType(login, MessageType.LOGIN);

        String requestedName = sanitizeUsername(login.required("username"));
        if (requestedName == null) {
            sendSecure(Messages.loginResult(false, "Tên người chơi phải dài 3-16 ký tự chữ, số hoặc dấu gạch dưới."));
            throw new ProtocolException("Client supplied an invalid username.");
        }
        if (!server.registerUsername(requestedName)) {
            sendSecure(Messages.loginResult(false, "Tên người chơi này đã được sử dụng."));
            throw new ProtocolException("Duplicate username.");
        }

        username = requestedName;
        loggedIn = true;
        sendSecure(Messages.loginResult(true, "Chào " + username + ". Đang chờ đối thủ..."));
    }

    private void handleMessage(NetworkMessage message) throws IOException, GeneralSecurityException {
        switch (message.type()) {
            case MOVE -> handleMove(message);
            case CHAT -> handleChat(message);
            case HEARTBEAT -> handleHeartbeat(message);
            case REMATCH -> handleRematch();
            default -> sendSecure(Messages.error("UNSUPPORTED", "Unsupported message type: " + message.type()));
        }
    }

    private void handleMove(NetworkMessage message) throws IOException, GeneralSecurityException {
        GameRoom activeRoom = room;
        if (activeRoom == null) {
            sendSecure(Messages.error("NO_SESSION", "Bạn hiện chưa ở trong ván chơi nào."));
            return;
        }
        if (!activeRoom.sessionId().equals(message.required("sessionId"))) {
            sendSecure(Messages.error("BAD_SESSION", "Nước đi thuộc về một phiên không hợp lệ."));
            return;
        }

        int row = message.requiredInt("row");
        int col = message.requiredInt("col");
        MoveResult result = activeRoom.playMove(this, row, col);
        if (!result.accepted()) {
            sendSecure(Messages.error("INVALID_MOVE", result.message()));
            return;
        }

        GameSnapshot snapshot = result.snapshot();
        broadcastSnapshot(snapshot);
        if (snapshot.status() != GameStatus.IN_PROGRESS) {
            broadcastGameOver(snapshot);
            server.onRoomFinished(activeRoom);
        }
    }

    private void handleChat(NetworkMessage message) throws IOException, GeneralSecurityException {
        GameRoom activeRoom = room;
        if (activeRoom == null) {
            sendSecure(Messages.error("NO_SESSION", "Trò chuyện chỉ khả dụng khi đang trong trận."));
            return;
        }

        String text = message.optional("text", "").trim();
        if (text.isBlank()) {
            return;
        }
        if (text.length() > config.maxChatLength()) {
            sendSecure(Messages.error("CHAT_TOO_LONG", "Tin nhắn vượt quá độ dài cho phép."));
            return;
        }

        String sentAt = Instant.now().toString();
        NetworkMessage chat = Messages.chat(activeRoom.sessionId(), username, text, sentAt);
        sendSecure(chat);
        ClientHandler opponent = activeRoom.opponentOf(this);
        if (opponent != null) {
            opponent.sendSecure(chat);
        }
    }

    private void handleHeartbeat(NetworkMessage message) throws IOException, GeneralSecurityException {
        lastHeartbeatAt = System.currentTimeMillis();
        sendSecure(Messages.heartbeat(message.optionalLong("timestamp", lastHeartbeatAt)));
    }

    private void handleRematch() throws IOException, GeneralSecurityException {
        GameRoom activeRoom = room;
        if (!loggedIn) {
            sendSecure(Messages.rematchResult(false, "Bạn chưa đăng nhập."));
            return;
        }
        if (activeRoom != null && !activeRoom.isFinished()) {
            sendSecure(Messages.rematchResult(false, "Bạn chỉ có thể ghép trận lại sau khi ván đấu kết thúc."));
            return;
        }

        room = null;
        sendSecure(Messages.rematchResult(true, "Đã đưa bạn vào hàng chờ ghép trận lại."));
        server.enqueueForMatchmaking(this);
    }

    private void broadcastSnapshot(GameSnapshot snapshot) throws IOException, GeneralSecurityException {
        GameRoom activeRoom = room;
        if (activeRoom == null) {
            return;
        }
        String nextTurnPlayer = snapshot.nextTurnSymbol() == null ? "" : activeRoom.playerName(snapshot.nextTurnSymbol());
        String winnerPlayer = snapshot.winnerSymbol() == null ? "" : activeRoom.playerName(snapshot.winnerSymbol());
        String statusMessage = buildStatusMessage(snapshot, nextTurnPlayer, winnerPlayer);
        NetworkMessage update = Messages.gameState(snapshot, nextTurnPlayer, winnerPlayer, statusMessage);
        sendSecure(update);

        ClientHandler opponent = activeRoom.opponentOf(this);
        if (opponent != null) {
            opponent.sendSecure(update);
        }
    }

    private void broadcastGameOver(GameSnapshot snapshot) throws IOException, GeneralSecurityException {
        GameRoom activeRoom = room;
        if (activeRoom == null) {
            return;
        }
        String winnerPlayer = snapshot.winnerSymbol() == null ? "" : activeRoom.playerName(snapshot.winnerSymbol());
        String reason = switch (snapshot.status()) {
            case WON -> winnerPlayer + " chiến thắng.";
            case DRAW -> "Ván cờ kết thúc với kết quả hòa.";
            case DISCONNECTED -> "Một người chơi đã ngắt kết nối.";
            default -> snapshot.reason();
        };
        NetworkMessage gameOver = Messages.gameOver(snapshot, winnerPlayer, reason);
        sendSecure(gameOver);
        ClientHandler opponent = activeRoom.opponentOf(this);
        if (opponent != null) {
            opponent.sendSecure(gameOver);
        }
    }

    private String buildStatusMessage(GameSnapshot snapshot, String nextTurnPlayer, String winnerPlayer) {
        return switch (snapshot.status()) {
            case IN_PROGRESS -> "Lượt: " + nextTurnPlayer;
            case WON -> "Người thắng: " + winnerPlayer;
            case DRAW -> "Ván cờ hòa.";
            case DISCONNECTED -> "Đối thủ đã ngắt kết nối.";
            case WAITING -> "Đang chờ người chơi.";
        };
    }

    public void sendSecure(NetworkMessage message) throws IOException, GeneralSecurityException {
        channel.sendSecure(message);
    }

    private void ensureType(NetworkMessage message, MessageType expected) throws ProtocolException {
        if (message.type() != expected) {
            throw new ProtocolException("Expected " + expected + " but received " + message.type());
        }
    }

    private String sanitizeUsername(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (!normalized.matches("[A-Za-z0-9_]{3,16}")) {
            return null;
        }
        return normalized;
    }

    private void cleanup() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }

        server.removeFromMatchmaking(this);
        server.unregisterUsername(username);

        GameRoom activeRoom = room;
        if (activeRoom != null) {
            ClientHandler opponent = activeRoom.opponentOf(this);
            GameSnapshot snapshot = activeRoom.disconnect(this);
            try {
                if (opponent != null && !opponent.closed.get()) {
                    opponent.sendSecure(Messages.gameState(
                            snapshot,
                            "",
                            snapshot.winnerSymbol() == null ? "" : activeRoom.playerName(snapshot.winnerSymbol()),
                            "Đối thủ đã ngắt kết nối."));
                    opponent.sendSecure(Messages.gameOver(snapshot,
                            snapshot.winnerSymbol() == null ? "" : activeRoom.playerName(snapshot.winnerSymbol()),
                            "Đối thủ đã ngắt kết nối."));
                }
            } catch (Exception ignored) {
                // Best effort cleanup.
            } finally {
                server.onRoomFinished(activeRoom);
            }
        }

        closeSilently();
    }

    private void closeSilently() {
        closed.set(true);
        try {
            socket.close();
        } catch (IOException ignored) {
            // Ignore close errors during shutdown.
        }
    }

    private String safeUsername() {
        return username == null ? socket.getRemoteSocketAddress().toString() : username;
    }
}
