package com.dmx.caro.client.controller;

import com.dmx.caro.client.model.ChatLine;
import com.dmx.caro.common.model.GameSnapshot;
import com.dmx.caro.common.model.PlayerSymbol;

public interface ClientEventListener {
    void onConnected(int boardSize, int winLength, int maxChatLength);

    void onLoginAccepted(String username, String message);

    void onRematchQueued(String message);

    void onMatchFound(String opponent, String sessionId, PlayerSymbol mySymbol, int boardSize, int winLength);

    void onGameSnapshot(GameSnapshot snapshot, String statusMessage);

    void onChat(ChatLine line);

    void onError(String message);

    void onDisconnected(String message);
}
