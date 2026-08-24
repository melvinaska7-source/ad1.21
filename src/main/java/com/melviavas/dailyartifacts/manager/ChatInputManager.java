package com.melviavas.dailyartifacts.manager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Хранит, каких игроков плагин ждёт с вводом цены в чат (после Shift+ПКМ в /ad settings). */
public class ChatInputManager {

    private final Map<UUID, String> pendingPriceInput = new HashMap<>();

    public void requestPrice(UUID playerId, String artifactId) {
        pendingPriceInput.put(playerId, artifactId);
    }

    public boolean isAwaiting(UUID playerId) {
        return pendingPriceInput.containsKey(playerId);
    }

    public String consume(UUID playerId) {
        return pendingPriceInput.remove(playerId);
    }

    public void cancel(UUID playerId) {
        pendingPriceInput.remove(playerId);
    }
}
