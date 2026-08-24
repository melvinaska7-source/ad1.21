package com.melviavas.dailyartifacts.manager;

import com.melviavas.dailyartifacts.DailyArtifactsPlugin;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Хук в Vault. ВАЖНО: сам плагин экономики (например CMI) должен быть
 * зарегистрирован КАК Vault-провайдер — а для этого на сервере должен
 * присутствовать и сам плагин Vault (просто CMI без Vault не подключится).
 */
public class EconomyManager {

    private final DailyArtifactsPlugin plugin;
    private Economy economy;

    public EconomyManager(DailyArtifactsPlugin plugin) {
        this.plugin = plugin;
    }

    /** @return true если экономика успешно найдена */
    public boolean setup() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().warning("Плагин Vault не найден! DailyArtifacts не сможет выдавать деньги.");
            plugin.getLogger().warning("Установи Vault рядом с CMI — CMI сам зарегистрируется как провайдер экономики.");
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = plugin.getServer()
                .getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            plugin.getLogger().warning("Экономика через Vault не найдена (нет зарегистрированного провайдера).");
            return false;
        }
        economy = rsp.getProvider();
        return true;
    }

    public boolean isReady() {
        return economy != null;
    }

    public void deposit(OfflinePlayer player, double amount) {
        if (economy != null) economy.depositPlayer(player, amount);
    }

    public String format(double amount) {
        return economy != null ? economy.format(amount) : String.valueOf(amount);
    }
}
