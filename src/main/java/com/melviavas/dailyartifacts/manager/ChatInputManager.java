package com.melviavas.dailyartifacts.manager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Хранит ожидаемый ввод значения из чата. */
public class ChatInputManager {

    public enum Type { PRICE, UPDATE_MIN, UPDATE_MAX, COUNT_MIN, COUNT_MAX }

    public record Request(Type type, String artifactId) {}

    private final Map<UUID, Request> pending = new HashMap<>();

    public void requestPrice(UUID playerId, String artifactId) {
        pending.put(playerId, new Request(Type.PRICE, artifactId));
    }

    public void request(UUID playerId, Type type) {
        pending.put(playerId, new Request(type, null));
    }

    public boolean isAwaiting(UUID playerId) {
        return pending.containsKey(playerId);
    }

    public Request consume(UUID playerId) {
        return pending.remove(playerId);
    }

    public void cancel(UUID playerId) {
        pending.remove(playerId);
    }
}
