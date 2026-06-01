package com.dmx.caro.common.config;

import com.dmx.caro.common.util.XmlSupport;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public final class AppConfig {
    private final String serverHost;
    private final int serverPort;
    private final int boardSize;
    private final int winLength;
    private final int heartbeatSeconds;
    private final int readTimeoutMillis;
    private final int connectTimeoutMillis;
    private final int maxChatLength;
    private final Path historyFile;

    private AppConfig(
            String serverHost,
            int serverPort,
            int boardSize,
            int winLength,
            int heartbeatSeconds,
            int readTimeoutMillis,
            int connectTimeoutMillis,
            int maxChatLength,
            Path historyFile) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
        this.boardSize = boardSize;
        this.winLength = winLength;
        this.heartbeatSeconds = heartbeatSeconds;
        this.readTimeoutMillis = readTimeoutMillis;
        this.connectTimeoutMillis = connectTimeoutMillis;
        this.maxChatLength = maxChatLength;
        this.historyFile = historyFile;
    }

    public static AppConfig load(Path configPath) throws IOException {
        try {
            Document document = XmlSupport.parse(configPath);
            Element root = document.getDocumentElement();

            Element server = XmlSupport.requiredChild(root, "server");
            Element game = XmlSupport.requiredChild(root, "game");
            Element persistence = XmlSupport.requiredChild(root, "persistence");

            String host = XmlSupport.childText(server, "host", "127.0.0.1");
            int port = XmlSupport.childInt(server, "port", 5050);
            int heartbeat = XmlSupport.childInt(server, "heartbeatSeconds", 4);
            int readTimeout = XmlSupport.childInt(server, "readTimeoutMillis", heartbeat * 4000);
            int connectTimeout = XmlSupport.childInt(server, "connectTimeoutMillis", 5000);
            int boardSize = XmlSupport.childInt(game, "boardSize", 15);
            int winLength = XmlSupport.childInt(game, "winLength", 5);
            int maxChatLength = XmlSupport.childInt(game, "maxChatLength", 220);
            String historyLocation = XmlSupport.childText(
                    persistence, "historyFile", "history/game-history.xml");

            validate(port > 0 && port <= 65535, "Port must be between 1 and 65535.");
            validate(boardSize >= 10 && boardSize <= 25, "Board size must be between 10 and 25.");
            validate(winLength >= 4 && winLength <= boardSize, "Win length must be between 4 and board size.");
            validate(heartbeat >= 2, "Heartbeat must be at least 2 seconds.");
            validate(readTimeout >= heartbeat * 1000, "Read timeout must be >= heartbeat interval.");
            validate(maxChatLength >= 40, "Max chat length must be at least 40 characters.");

            Path historyPath = Paths.get(historyLocation);
            return new AppConfig(
                    host,
                    port,
                    boardSize,
                    winLength,
                    heartbeat,
                    readTimeout,
                    connectTimeout,
                    maxChatLength,
                    historyPath);
        } catch (Exception exception) {
            throw new IOException("Unable to load config from " + configPath + ": " + exception.getMessage(), exception);
        }
    }

    private static void validate(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }

    public String serverHost() {
        return serverHost;
    }

    public int serverPort() {
        return serverPort;
    }

    public int boardSize() {
        return boardSize;
    }

    public int winLength() {
        return winLength;
    }

    public int heartbeatSeconds() {
        return heartbeatSeconds;
    }

    public int readTimeoutMillis() {
        return readTimeoutMillis;
    }

    public int connectTimeoutMillis() {
        return connectTimeoutMillis;
    }

    public int maxChatLength() {
        return maxChatLength;
    }

    public Path historyFile() {
        return historyFile;
    }
}
