package com.dmx.caro.server;

import com.dmx.caro.common.model.MoveRecord;
import com.dmx.caro.common.model.PlayerSymbol;
import com.dmx.caro.common.util.XmlSupport;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public final class GameHistoryRepository {
    private final Path historyFile;
    private final Object lock = new Object();

    public GameHistoryRepository(Path historyFile) {
        this.historyFile = historyFile;
    }

    public void append(GameRoom room) throws IOException {
        synchronized (lock) {
            Document document = loadOrCreate();
            Element root = document.getDocumentElement();
            root.appendChild(createGameElement(document, room));
            XmlSupport.write(document, historyFile);
        }
    }

    private Document loadOrCreate() throws IOException {
        try {
            if (Files.exists(historyFile)) {
                return XmlSupport.parse(historyFile);
            }

            Document document = XmlSupport.newDocument();
            Element root = document.createElement("games");
            document.appendChild(root);
            return document;
        } catch (Exception exception) {
            throw new IOException("Unable to access game history at " + historyFile + ".", exception);
        }
    }

    private Element createGameElement(Document document, GameRoom room) {
        Element game = document.createElement("game");
        game.setAttribute("sessionId", room.sessionId());
        game.setAttribute("boardSize", String.valueOf(room.boardSize()));
        game.setAttribute("winLength", String.valueOf(room.winLength()));
        game.setAttribute("status", room.status().name());
        game.setAttribute("startedAt", toText(room.startedAt()));
        game.setAttribute("finishedAt", toText(room.finishedAt()));
        game.setAttribute("playerX", room.playerName(PlayerSymbol.X));
        game.setAttribute("playerO", room.playerName(PlayerSymbol.O));
        game.setAttribute("winner", room.winnerSymbol() == null ? "" : room.playerName(room.winnerSymbol()));

        Element moves = document.createElement("moves");
        for (MoveRecord move : room.moves()) {
            Element moveElement = document.createElement("move");
            moveElement.setAttribute("row", String.valueOf(move.row()));
            moveElement.setAttribute("col", String.valueOf(move.col()));
            moveElement.setAttribute("symbol", move.symbol().name());
            moveElement.setAttribute("playedAt", toText(move.playedAt()));
            moves.appendChild(moveElement);
        }
        game.appendChild(moves);
        return game;
    }

    private String toText(Instant instant) {
        return instant == null ? "" : instant.toString();
    }
}
