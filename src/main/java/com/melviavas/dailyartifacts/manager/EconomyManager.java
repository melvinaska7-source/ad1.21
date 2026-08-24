package com.melviavas.dailyartifacts.manager;

import com.melviavas.dailyartifacts.DailyArtifactsPlugin;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.OfflinePlayer;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

/** Экономика: сначала Vault, затем настраиваемый консольный fallback. */
public class EconomyManager {

    private final DailyArtifactsPlugin plugin;
    private Economy economy;

    public EconomyManager(DailyArtifactsPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean setup() {
        return trySetup(true);
    }

    /** Некоторые экономические плагины регистрируют Vault provider позже нашего onEnable. */
    public boolean trySetup(boolean log) {
        if (economy != null) return true;

        boolean vaultEnabled = plugin.getConfigManager().getConfig().getBoolean("economy.vault", true);
        if (vaultEnabled && plugin.getServer().getPluginManager().getPlugin("Vault") != null) {
            RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
            if (rsp != null && rsp.getProvider() != null) {
                economy = rsp.getProvider();
                plugin.getLogger().info("Экономика подключена через Vault: " + economy.getName());
                return true;
            }
        }

        if (log && !hasCommandFallback()) {
            if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
                plugin.getLogger().warning("Vault не найден. Будет использоваться командный fallback, если он включён.");
            } else {
                plugin.getLogger().warning("Vault найден, но Economy-провайдер пока не зарегистрирован. Буду повторять проверку.");
            }
        }
        return hasCommandFallback();
    }

    private boolean hasCommandFallback() {
        return plugin.getConfigManager().getConfig().getBoolean("economy.command-fallback.enabled", true)
                && !plugin.getConfigManager().getConfig().getString("economy.command-fallback.command", "").isBlank();
    }

    public boolean isReady() {
        return economy != null || hasCommandFallback();
    }

    /** Возвращает true только если выбранный способ выплаты доступен. */
    public boolean deposit(OfflinePlayer player, double amount) {
        if (amount < 0) return false;
        if (economy != null) {
            return economy.depositPlayer(player, amount).transactionSuccess();
        }
        if (!hasCommandFallback()) return false;

        String command = plugin.getConfigManager().getConfig()
                .getString("economy.command-fallback.command", "eco give {player} {amount}");
        String amountText = trim(amount);
        command = command.replace("{player}", player.getName() == null ? "" : player.getName())
                .replace("{uuid}", player.getUniqueId().toString())
                .replace("{amount}", amountText);
        if (command.startsWith("/")) command = command.substring(1);
        return Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
    }

    public String format(double amount) {
        return economy != null ? economy.format(amount) : trim(amount);
    }

    private String trim(double value) {
        return value == Math.floor(value) ? String.valueOf((long) value) : String.valueOf(value);
    }
}
