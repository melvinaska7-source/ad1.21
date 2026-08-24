package com.melviavas.dailyartifacts.manager;

import com.melviavas.dailyartifacts.DailyArtifactsPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

/**
 * Загружает config.yml, items.yml, menu.yml, messages.yml из папки плагина,
 * копируя дефолтные версии из ресурсов jar'а при первом запуске.
 * /ad reload дёргает reloadAll() без рестарта сервера.
 */
public class ConfigManager {

    private final DailyArtifactsPlugin plugin;

    private FileConfiguration config;
    private FileConfiguration items;
    private FileConfiguration menu;
    private FileConfiguration messages;

    private File itemsFile;

    public ConfigManager(DailyArtifactsPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadAll() {
        plugin.saveDefaultConfig();
        config = plugin.getConfig();

        itemsFile = ensureFile("items.yml");
        items = YamlConfiguration.loadConfiguration(itemsFile);

        File menuFile = ensureFile("menu.yml");
        menu = YamlConfiguration.loadConfiguration(menuFile);

        File messagesFile = ensureFile("messages.yml");
        messages = YamlConfiguration.loadConfiguration(messagesFile);
    }

    public void reloadAll() {
        plugin.reloadConfig();
        config = plugin.getConfig();

        items = YamlConfiguration.loadConfiguration(itemsFile);
        menu = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "menu.yml"));
        messages = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "messages.yml"));
    }

    private File ensureFile(String name) {
        File file = new File(plugin.getDataFolder(), name);
        if (!file.exists()) {
            plugin.saveResource(name, false);
        }
        return file;
    }

    public void saveItems() {
        try {
            items.save(itemsFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Не удалось сохранить items.yml", e);
        }
    }

    public FileConfiguration getConfig() { return config; }
    public FileConfiguration getItems() { return items; }
    public FileConfiguration getMenu() { return menu; }
    public FileConfiguration getMessages() { return messages; }

    public String msg(String path) {
        String prefix = messages.getString("prefix", "");
        String raw = messages.getString("messages." + path, path);
        return raw.replace("{prefix}", prefix);
    }
}
