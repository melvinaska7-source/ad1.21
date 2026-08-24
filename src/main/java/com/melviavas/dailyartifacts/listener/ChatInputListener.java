package com.melviavas.dailyartifacts.listener;

import com.melviavas.dailyartifacts.DailyArtifactsPlugin;
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
        String raw = PlainTextComponentSerializer.plainText().serialize(event.message());
        String artifactId = plugin.getChatInputManager().consume(player.getUniqueId());

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (raw.equalsIgnoreCase("cancel")) {
                return;
            }
            try {
                double value = Double.parseDouble(raw.trim().replace(",", "."));
                ArtifactItem item = plugin.getArtifactManager().getById(artifactId);
                if (item == null) return;
                plugin.getArtifactManager().addOrUpdateItem(
                        artifactId, item.getMaterial(), value, item.getLimit());
                player.sendMessage(plugin.getConfigManager().msg("value-updated")
                        .replace("{value}", raw.trim()));
            } catch (NumberFormatException e) {
                player.sendMessage(plugin.getConfigManager().msg("invalid-number"));
            }
        });
    }
}
