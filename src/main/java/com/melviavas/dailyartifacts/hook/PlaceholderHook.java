package com.melviavas.dailyartifacts.hook;

import com.melviavas.dailyartifacts.DailyArtifactsPlugin;
import com.melviavas.dailyartifacts.model.ArtifactItem;
import com.melviavas.dailyartifacts.util.TimeUtil;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

/**
 * Плейсхолдеры:
 *  %dailyartifacts_time_left%          — сколько осталось до обновления (формат из config.yml)
 *  %dailyartifacts_count%              — сколько артефактов активно сейчас
 *  %dailyartifacts_progress_<id>%      — прогресс игрока по конкретному артефакту (given/limit)
 */
public class PlaceholderHook extends PlaceholderExpansion {

    private final DailyArtifactsPlugin plugin;

    public PlaceholderHook(DailyArtifactsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "dailyartifacts";
    }

    @Override
    public @NotNull String getAuthor() {
        return "melviavas";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (params.equalsIgnoreCase("time_left")) {
            return TimeUtil.formatRemaining(
                    plugin.getArtifactManager().getMillisUntilNextUpdate(),
                    plugin.getConfigManager().getConfig());
        }
        if (params.equalsIgnoreCase("count")) {
            return String.valueOf(plugin.getArtifactManager().getActiveArtifacts().size());
        }
        if (params.startsWith("progress_")) {
            String id = params.substring("progress_".length());
            ArtifactItem item = plugin.getArtifactManager().getById(id);
            if (item == null || player.getUniqueId() == null) return "0/0";
            int given = plugin.getDataManager().getProgress(player.getUniqueId(), id);
            return given + "/" + item.getLimit();
        }
        return null;
    }
}
