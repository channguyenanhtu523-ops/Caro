package com.dmx.caro.server;

import com.dmx.caro.common.config.AppConfig;
import com.dmx.caro.common.crypto.CryptoEngine;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.time.Instant;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public final class GameServer implements AutoCloseable {
    private final AppConfig config;
    private final GameHistoryRepository historyRepository;
    private final KeyPair serverKeyPair;
    private final ExecutorService clientExecutor;
    private final Queue<ClientHandler> waitingPlayers;
    private final Set<String> activeUsernames;
    private final Set<GameRoom> activeRooms;
    private final AtomicBoolean running;

    private ServerSocket serverSocket;

    public GameServer(AppConfig config) throws GeneralSecurityException {
        this.config = Objects.requireNonNull(config);
        this.historyRepository = new GameHistoryRepository(config.historyFile());
        this.serverKeyPair = CryptoEngine.generateRsaKeyPair();
        this.clientExecutor = Executors.newCachedThreadPool();
        this.waitingPlayers = new ConcurrentLinkedQueue<>();
        this.activeUsernames = ConcurrentHashMap.newKeySet();
        this.activeRooms = ConcurrentHashMap.newKeySet();
        this.running = new AtomicBoolean(false);
    }

    public void start() throws IOException {
        if (!running.compareAndSet(false, true)) {
            return;
        }

        serverSocket = new ServerSocket(config.serverPort());
        System.out.printf("[%s] Caro server listening on %d%n", Instant.now(), config.serverPort());

        while (running.get()) {
            try {
                Socket socket = serverSocket.accept();
                socket.setSoTimeout(config.readTimeoutMillis());
                ClientHandler handler = new ClientHandler(socket, this, config, serverKeyPair);
                clientExecutor.submit(handler);
            } catch (SocketException exception) {
                if (running.get()) {
                    System.err.println("Server socket error: " + exception.getMessage());
                }
            }
        }
    }

    public boolean registerUsername(String username) {
        return activeUsernames.add(username.toLowerCase());
    }

    public void unregisterUsername(String username) {
        if (username != null) {
            activeUsernames.remove(username.toLowerCase());
        }
    }

    public void enqueueForMatchmaking(ClientHandler handler) {
        ClientHandler opponent = null;

        synchronized (waitingPlayers) {
            waitingPlayers.remove(handler);

            while ((opponent = waitingPlayers.poll()) != null) {
                if (opponent.isReadyForMatch() && opponent != handler) {
                    break;
                }
                opponent = null;
            }

            if (opponent == null) {
                waitingPlayers.offer(handler);
                return;
            }
        }

        startRoom(opponent, handler);
    }

    public void removeFromMatchmaking(ClientHandler handler) {
        waitingPlayers.remove(handler);
    }

    public void onRoomFinished(GameRoom room) {
        if (room == null) {
            return;
        }
        if (activeRooms.remove(room)) {
            room.playerX().attachRoom(null);
            room.playerO().attachRoom(null);
            try {
                historyRepository.append(room);
            } catch (IOException exception) {
                System.err.println("Unable to persist game history for session " + room.sessionId() + ": " + exception.getMessage());
            }
        }
    }

    public void shutdownRoom(GameRoom room) {
        if (room != null) {
            activeRooms.remove(room);
        }
    }

    private void startRoom(ClientHandler first, ClientHandler second) {
        GameRoom room = new GameRoom(config.boardSize(), config.winLength(), first, second);
        activeRooms.add(room);
        first.attachRoom(room);
        second.attachRoom(room);
        first.onMatchFound();
        second.onMatchFound();
        first.broadcastInitialSnapshot();
    }

    @Override
    public void close() throws IOException {
        running.set(false);
        if (serverSocket != null && !serverSocket.isClosed()) {
            serverSocket.close();
        }
        clientExecutor.shutdownNow();
    }

    public static void main(String[] args) {
        try {
            AppConfig config = AppConfig.load(java.nio.file.Path.of("config", "app-config.xml"));
            try (GameServer server = new GameServer(config)) {
                server.start();
            }
        } catch (Exception exception) {
            System.err.println("Fatal server error: " + exception.getMessage());
            exception.printStackTrace(System.err);
        }
    }
}
