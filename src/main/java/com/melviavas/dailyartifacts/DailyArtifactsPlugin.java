package com.melviavas.dailyartifacts;

import com.melviavas.dailyartifacts.command.ADCommand;
import com.melviavas.dailyartifacts.gui.ArtifactMenuGUI;
import com.melviavas.dailyartifacts.gui.SettingsGUI;
import com.melviavas.dailyartifacts.hook.PlaceholderHook;
import com.melviavas.dailyartifacts.listener.ChatInputListener;
import com.melviavas.dailyartifacts.listener.MenuListener;
import com.melviavas.dailyartifacts.manager.ArtifactManager;
import com.melviavas.dailyartifacts.manager.ChatInputManager;
import com.melviavas.dailyartifacts.manager.ConfigManager;
import com.melviavas.dailyartifacts.manager.DataManager;
import com.melviavas.dailyartifacts.manager.EconomyManager;
import com.melviavas.dailyartifacts.manager.PermissionManager;
import org.bukkit.plugin.java.JavaPlugin;

public class DailyArtifactsPlugin extends JavaPlugin {

    private ConfigManager configManager;
    private DataManager dataManager;
    private ArtifactManager artifactManager;
    private EconomyManager economyManager;
    private PermissionManager permissionManager;
    private ChatInputManager chatInputManager;

    private ArtifactMenuGUI artifactMenuGUI;
    private SettingsGUI settingsGUI;

    @Override
    public void onEnable() {
        configManager = new ConfigManager(this);
        configManager.loadAll();

        dataManager = new DataManager(this);
        dataManager.load();

        economyManager = new EconomyManager(this);
        economyManager.setup();

        permissionManager = new PermissionManager(this);
        chatInputManager = new ChatInputManager();

        artifactManager = new ArtifactManager(this);
        artifactManager.init();

        artifactMenuGUI = new ArtifactMenuGUI(this);
        settingsGUI = new SettingsGUI(this);

        getServer().getPluginManager().registerEvents(new MenuListener(this), this);
        getServer().getPluginManager().registerEvents(new ChatInputListener(this), this);

        ADCommand adCommand = new ADCommand(this);
        var pluginCommand = getCommand("dailyartifacts");
        if (pluginCommand != null) {
            pluginCommand.setExecutor(adCommand);
            pluginCommand.setTabCompleter(adCommand);
        }

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PlaceholderHook(this).register();
            getLogger().info("PlaceholderAPI найден, плейсхолдеры %dailyartifacts_...% зарегистрированы.");
        }

        getLogger().info("DailyArtifacts включён. Активные артефакты: "
                + artifactManager.getActiveArtifacts().size());
    }

    @Override
    public void onDisable() {
        getLogger().info("DailyArtifacts выключен.");
    }

    public ConfigManager getConfigManager() { return configManager; }
    public DataManager getDataManager() { return dataManager; }
    public ArtifactManager getArtifactManager() { return artifactManager; }
    public EconomyManager getEconomyManager() { return economyManager; }
    public PermissionManager getPermissionManager() { return permissionManager; }
    public ChatInputManager getChatInputManager() { return chatInputManager; }
    public ArtifactMenuGUI getArtifactMenuGUI() { return artifactMenuGUI; }
    public SettingsGUI getSettingsGUI() { return settingsGUI; }
}
