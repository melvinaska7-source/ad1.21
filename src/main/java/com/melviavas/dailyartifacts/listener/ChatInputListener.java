package com.melviavas.dailyartifacts.listener;

import com.melviavas.dailyartifacts.DailyArtifactsPlugin;
import com.melviavas.dailyartifacts.manager.ChatInputManager;
import com.melviavas.dailyartifacts.model.ArtifactItem;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class ChatInputListener implements Listener {

    private final DailyArtifactsPlugin plugin;

    public ChatInputListener(DailyArtifactsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getChatInputManager().isAwaiting(player.getUniqueId())) return;

        event.setCancelled(true);
        String raw = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();
        ChatInputManager.Request request = plugin.getChatInputManager().consume(player.getUniqueId());

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (raw.equalsIgnoreCase("cancel")) return;

            try {
                switch (request.type()) {
                    case PRICE -> {
                        double value = Double.parseDouble(raw.replace(",", "."));
                        if (value < 0) throw new NumberFormatException();
                        ArtifactItem item = plugin.getArtifactManager().getById(request.artifactId());
                        if (item == null) return;
                        plugin.getArtifactManager().addOrUpdateItem(
                                request.artifactId(), item.getMaterial(), value, item.getLimit());
                        player.sendMessage(plugin.getConfigManager().msg("value-updated")
                                .replace("{value}", raw));
                    }
                    case UPDATE_MIN, UPDATE_MAX -> {
                        long millis = com.melviavas.dailyartifacts.util.TimeUtil.parseDuration(raw);
                        if (millis <= 0) throw new NumberFormatException();
                        String path = request.type() == ChatInputManager.Type.UPDATE_MIN
                                ? "update-time.min" : "update-time.max";
                        plugin.getConfigManager().getConfig().set(path, raw);
                        plugin.getConfigManager().saveConfig();
                        player.sendMessage(plugin.getConfigManager().msg("value-updated")
                                .replace("{value}", raw));
                    }
                    case COUNT_MIN, COUNT_MAX -> {
                        int value = Integer.parseInt(raw);
                        if (value < 1 || value > 5) throw new NumberFormatException();
                        String path = request.type() == ChatInputManager.Type.COUNT_MIN
                                ? "artifact-count.min" : "artifact-count.max";
                        plugin.getConfigManager().getConfig().set(path, value);
                        plugin.getConfigManager().saveConfig();
                        player.sendMessage(plugin.getConfigManager().msg("value-updated")
                                .replace("{value}", String.valueOf(value)));
                    }
                }
            } catch (NumberFormatException ex) {
                player.sendMessage(plugin.getConfigManager().msg("invalid-number"));
            }
        });
    }
}
