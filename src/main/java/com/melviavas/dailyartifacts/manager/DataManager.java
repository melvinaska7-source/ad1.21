package com.melviavas.dailyartifacts.manager;

import com.melviavas.dailyartifacts.DailyArtifactsPlugin;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * data/state.yml — текущие активные артефакты + время следующего обновления (epoch millis).
 * data/players.yml — индивидуальный прогресс каждого игрока по каждому артефакту периода.
 *
 * Файлы не пишутся каждый тик — только при изменениях (сдача предмета, ротация, /ad settings).
 */
public class DataManager {

    private final DailyArtifactsPlugin plugin;
    private final File dataFolder;
    private final File stateFile;
    private final File playersFile;

    private YamlConfiguration state;
    private YamlConfiguration players;

    public DataManager(DailyArtifactsPlugin plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "data");
        this.stateFile = new File(dataFolder, "state.yml");
        this.playersFile = new File(dataFolder, "players.yml");
    }

    public void load() {
        if (!dataFolder.exists()) dataFolder.mkdirs();
        state = YamlConfiguration.loadConfiguration(stateFile);
        players = YamlConfiguration.loadConfiguration(playersFile);
    }

    // ---------- state.yml ----------

    public List<String> getActiveArtifactIds() {
        return state.getStringList("active");
    }

    public long getNextUpdateEpoch() {
        return state.getLong("next-update", 0L);
    }

    public void saveRotation(List<String> activeIds, long nextUpdateEpoch) {
        state.set("active", activeIds);
        state.set("next-update", nextUpdateEpoch);
        saveState();
    }

    private void saveState() {
        try {
            state.save(stateFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Не удалось сохранить data/state.yml", e);
        }
    }

    // ---------- players.yml ----------
    // Путь: players.<uuid>.<artifactId> = сколько сдано

    public int getProgress(UUID playerId, String artifactId) {
        return players.getInt("players." + playerId + "." + artifactId, 0);
    }

    public void setProgress(UUID playerId, String artifactId, int amount) {
        players.set("players." + playerId + "." + artifactId, amount);
        savePlayers();
    }

    /** Сбрасывает прогресс всех игроков (вызывается при ротации артефактов). */
    public void resetAllProgress() {
        players.set("players", new HashMap<String, Object>());
        savePlayers();
    }

    private void savePlayers() {
        try {
            players.save(playersFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Не удалось сохранить data/players.yml", e);
        }
    }
}
