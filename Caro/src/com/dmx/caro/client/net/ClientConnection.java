package com.dmx.caro.client.net;

import com.dmx.caro.client.controller.ClientEventListener;
import com.dmx.caro.client.model.ChatLine;
import com.dmx.caro.common.config.AppConfig;
import com.dmx.caro.common.crypto.CryptoEngine;
import com.dmx.caro.common.crypto.SessionKeys;
import com.dmx.caro.common.model.GameSnapshot;
import com.dmx.caro.common.model.PlayerSymbol;
import com.dmx.caro.common.net.SecureChannel;
import com.dmx.caro.common.protocol.MessageType;
import com.dmx.caro.common.protocol.Messages;
import com.dmx.caro.common.protocol.NetworkMessage;
import com.dmx.caro.common.protocol.ProtocolException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ClientConnection implements AutoCloseable {
    private final AppConfig config;
    private final ClientEventListener listener;
    private final ExecutorService readerExecutor;
    private final ScheduledExecutorService heartbeatExecutor;
    private final AtomicBoolean closed;

    private Socket socket;
    private SecureChannel channel;
    private volatile int boardSize;
    private volatile int winLength;
    private volatile int heartbeatSeconds;
    private volatile int maxChatLength;

    public ClientConnection(AppConfig config, ClientEventListener listener) {
        this.config = Objects.requireNonNull(config);
        this.listener = Objects.requireNonNull(listener);
        this.readerExecutor = Executors.newSingleThreadExecutor();
        this.heartbeatExecutor = Executors.newSingleThreadScheduledExecutor();
        this.closed = new AtomicBoolean(false);
    }

    public void connectAndLogin(String host, int port, String username)
            throws IOException, GeneralSecurityException, ProtocolException {
        socket = new Socket();
        socket.connect(new InetSocketAddress(host, port), config.connectTimeoutMillis());
        socket.setSoTimeout(config.readTimeoutMillis());
        channel = new SecureChannel(socket);

        channel.sendPlain(Messages.hello("1.0"));
        NetworkMessage serverHello = channel.readMessage();
        ensureType(serverHello, MessageType.SERVER_HELLO);

        boardSize = serverHello.requiredInt("boardSize");
        winLength = serverHello.requiredInt("winLength");
        heartbeatSeconds = serverHello.requiredInt("heartbeatSeconds");
        maxChatLength = serverHello.requiredInt("maxChatLength");

        PublicKey serverPublicKey = CryptoEngine.decodePublicKey(serverHello.required("publicKey"));
        SessionKeys sessionKeys = CryptoEngine.generateSessionKeys();
        channel.sendPlain(Messages.keyExchange(CryptoEngine.encryptSessionKeys(sessionKeys, serverPublicKey)));
        channel.setSessionKeys(sessionKeys);

        NetworkMessage keyAck = channel.readMessage();
        ensureType(keyAck, MessageType.KEY_ACK);
        listener.onConnected(boardSize, winLength, maxChatLength);

        channel.sendSecure(Messages.login(username));
        NetworkMessage loginResult = channel.readMessage();
        ensureType(loginResult, MessageType.LOGIN_RESULT);
        if (!loginResult.requiredBoolean("accepted")) {
            throw new ProtocolException(loginResult.required("message"));
        }
        listener.onLoginAccepted(username, loginResult.required("message"));

        startReaderLoop();
        startHeartbeatLoop();
    }

    public int maxChatLength() {
        return maxChatLength;
    }

    public void sendMove(String sessionId, int row, int col, int moveNumber) throws IOException, GeneralSecurityException {
        channel.sendSecure(Messages.move(sessionId, row, col, moveNumber));
    }

    public void sendChat(String sessionId, String from, String text) throws IOException, GeneralSecurityException {
        channel.sendSecure(Messages.chat(sessionId, from, text, Instant.now().toString()));
    }

    public void requestRematch() throws IOException, GeneralSecurityException {
        channel.sendSecure(Messages.rematch());
    }

    private void startReaderLoop() {
        readerExecutor.submit(() -> {
            try {
                while (!closed.get()) {
                    handleServerMessage(channel.readMessage());
                }
            } catch (Exception exception) {
                if (!closed.get()) {
                    listener.onDisconnected("Connection lost: " + exception.getMessage());
                }
            } finally {
                close();
            }
        });
    }

    private void startHeartbeatLoop() {
        heartbeatExecutor.scheduleAtFixedRate(() -> {
            if (closed.get()) {
                return;
            }
            try {
                channel.sendSecure(Messages.heartbeat(System.currentTimeMillis()));
            } catch (Exception exception) {
                listener.onDisconnected("Heartbeat failed: " + exception.getMessage());
                close();
            }
        }, heartbeatSeconds, heartbeatSeconds, TimeUnit.SECONDS);
    }

    private void handleServerMessage(NetworkMessage message) {
        switch (message.type()) {
            case MATCH_FOUND -> listener.onMatchFound(
                    message.required("opponent"),
                    message.required("sessionId"),
                    PlayerSymbol.fromWireValue(message.required("symbol")),
                    message.requiredInt("boardSize"),
                    message.requiredInt("winLength"));
            case REMATCH_RESULT -> {
                if (message.requiredBoolean("accepted")) {
                    listener.onRematchQueued(message.required("message"));
                } else {
                    listener.onError(message.required("message"));
                }
            }
            case GAME_STATE -> listener.onGameSnapshot(
                    GameSnapshot.fromMessage(message),
                    message.optional("statusMessage", ""));
            case GAME_OVER -> listener.onGameSnapshot(
                    GameSnapshot.fromMessage(message),
                    message.optional("reason", "Game over."));
            case CHAT -> listener.onChat(new ChatLine(
                    message.required("from"),
                    message.required("text"),
                    message.optional("sentAt", "")));
            case ERROR -> listener.onError(message.required("message"));
            case HEARTBEAT -> {
                // Heartbeat ACK, nothing else to do.
            }
            default -> listener.onError("Unexpected message: " + message.type());
        }
    }

    private void ensureType(NetworkMessage message, MessageType expected) throws ProtocolException {
        if (message.type() != expected) {
            throw new ProtocolException("Expected " + expected + " but received " + message.type());
        }
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        heartbeatExecutor.shutdownNow();
        readerExecutor.shutdownNow();
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException ignored) {
                // Ignore close failures during shutdown.
            }
        }
    }
}
