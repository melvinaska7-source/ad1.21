package com.melviavas.dailyartifacts.manager;

import com.melviavas.dailyartifacts.DailyArtifactsPlugin;
import com.melviavas.dailyartifacts.model.ArtifactItem;
import com.melviavas.dailyartifacts.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Хранит текущий пул возможных предметов (items.yml) и текущие активные артефакты периода.
 * При старте плагина: если сохранённая ротация ещё не истекла — восстанавливает её из data/state.yml.
 * Если истекла или её не было — сразу выбирает новую.
 * Дальше раз в минуту проверяет, не пора ли обновиться (простая и надёжная схема без
 * долгоживущих scheduler-задач на много дней вперёд).
 */
public class ArtifactManager {

    private final DailyArtifactsPlugin plugin;
    private final Random random = new Random();

    private final Map<String, ArtifactItem> pool = new LinkedHashMap<>();
    private List<String> activeIds = new ArrayList<>();
    private long nextUpdateEpoch;

    public ArtifactManager(DailyArtifactsPlugin plugin) {
        this.plugin = plugin;
    }

    public void init() {
        loadPool();

        long savedNext = plugin.getDataManager().getNextUpdateEpoch();
        List<String> savedActive = plugin.getDataManager().getActiveArtifactIds();

        List<String> validSaved = new ArrayList<>();
        for (String id : savedActive) {
            ArtifactItem item = pool.get(id);
            if (item != null && item.isEnabled()) validSaved.add(id);
        }

        if (savedNext > System.currentTimeMillis() && !validSaved.isEmpty()) {
            this.activeIds = validSaved;
            this.nextUpdateEpoch = savedNext;
        } else {
            rotate();
        }

        // Раз в минуту (1200 тиков) проверяем, не истекло ли время.
        Bukkit.getScheduler().runTaskTimer(plugin, this::checkRotation, 1200L, 1200L);
    }

    public void loadPool() {
        pool.clear();
        ConfigurationSection section = plugin.getConfigManager().getItems().getConfigurationSection("items");
        if (section == null) return;

        for (String id : section.getKeys(false)) {
            boolean enabled = section.getBoolean(id + ".enabled", true);
            String materialName = section.getString(id + ".material", "STONE");
            Material material = Material.matchMaterial(materialName);
            if (material == null) {
                plugin.getLogger().warning("Неизвестный Material '" + materialName + "' для предмета '" + id + "', пропускаю.");
                continue;
            }
            double price = section.getDouble(id + ".price", 100);
            int limit = section.getInt(id + ".limit", 64);
            pool.put(id, new ArtifactItem(id, enabled, material, price, limit));
        }
    }

    private void checkRotation() {
        if (System.currentTimeMillis() >= nextUpdateEpoch) {
            rotate();
        }
    }

    /** Принудительно выбирает новый набор артефактов и сбрасывает прогресс всех игроков. */
    public void rotate() {
        List<String> enabledIds = new ArrayList<>();
        for (ArtifactItem item : pool.values()) {
            if (item.isEnabled()) enabledIds.add(item.getId());
        }
        Collections.shuffle(enabledIds, random);

        int min = plugin.getConfigManager().getConfig().getInt("artifact-count.min", 1);
        int max = plugin.getConfigManager().getConfig().getInt("artifact-count.max", 5);
        if (min > max) { int t = min; min = max; max = t; }

        int count = min + (max > min ? random.nextInt(max - min + 1) : 0);
        count = Math.min(count, enabledIds.size());

        if (enabledIds.size() < min) {
            plugin.getLogger().warning("В items.yml включено меньше предметов (" + enabledIds.size()
                    + "), чем artifact-count.min (" + min + "). Будут выбраны все доступные.");
        }

        this.activeIds = new ArrayList<>(enabledIds.subList(0, count));

        String minDur = plugin.getConfigManager().getConfig().getString("update-time.min", "1h");
        String maxDur = plugin.getConfigManager().getConfig().getString("update-time.max", "7d");
        long minMs = TimeUtil.parseDuration(minDur);
        long maxMs = TimeUtil.parseDuration(maxDur);
        if (minMs <= 0) minMs = 3_600_000L;
        if (maxMs < minMs) maxMs = minMs;

        long span = maxMs - minMs;
        long duration = minMs + (span > 0 ? (long) (random.nextDouble() * span) : 0);
        this.nextUpdateEpoch = System.currentTimeMillis() + duration;

        plugin.getDataManager().saveRotation(activeIds, nextUpdateEpoch);
        plugin.getDataManager().resetAllProgress();

        plugin.getLogger().info("DailyArtifacts: новая ротация — " + activeIds
                + " (следующая через " + (duration / 60000) + " мин.)");
    }

    /**
     * Гарантирует, что при наличии активного пула есть хотя бы одна ротация.
     * Используется командами/GUI после reload или ручного изменения файлов.
     */
    public void ensureRotation() {
        if (activeIds.isEmpty() || nextUpdateEpoch <= System.currentTimeMillis()) {
            rotate();
        }
    }

    public List<ArtifactItem> getActiveArtifacts() {
        List<ArtifactItem> result = new ArrayList<>();
        for (String id : activeIds) {
            ArtifactItem item = pool.get(id);
            if (item != null) result.add(item);
        }
        return result;
    }

    public ArtifactItem getById(String id) {
        return pool.get(id);
    }

    public Map<String, ArtifactItem> getPool() {
        return pool;
    }

    public long getMillisUntilNextUpdate() {
        return nextUpdateEpoch - System.currentTimeMillis();
    }

    public void addOrUpdateItem(String id, Material material, double price, int limit) {
        ArtifactItem existing = pool.get(id);
        if (existing != null) {
            existing.setMaterial(material);
            existing.setPrice(price);
            existing.setLimit(limit);
        } else {
            pool.put(id, new ArtifactItem(id, true, material, price, limit));
        }
        persistPool();
    }

    public void removeItem(String id) {
        pool.remove(id);
        activeIds.remove(id);
        persistPool();
    }

    private void persistPool() {
        ConfigurationSection section = plugin.getConfigManager().getItems().createSection("items");
        for (ArtifactItem item : pool.values()) {
            section.set(item.getId() + ".enabled", item.isEnabled());
            section.set(item.getId() + ".material", item.getMaterial().name());
            section.set(item.getId() + ".price", item.getPrice());
            section.set(item.getId() + ".limit", item.getLimit());
        }
        plugin.getConfigManager().saveItems();
    }
}
