package com.melviavas.dailyartifacts.command;

import com.melviavas.dailyartifacts.DailyArtifactsPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class ADCommand implements CommandExecutor, TabCompleter {

    private final DailyArtifactsPlugin plugin;

    public ADCommand(DailyArtifactsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(plugin.getConfigManager().msg("unknown-command"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "menu" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(plugin.getConfigManager().msg("player-only"));
                    return true;
                }
                if (!plugin.getPermissionManager().canOpenMenu(player)) {
                    player.sendMessage(plugin.getConfigManager().msg("no-permission"));
                    return true;
                }
                player.openInventory(plugin.getArtifactMenuGUI().build(player));
            }
            case "settings" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(plugin.getConfigManager().msg("player-only"));
                    return true;
                }
                if (!plugin.getPermissionManager().canOpenSettings(player)) {
                    player.sendMessage(plugin.getConfigManager().msg("no-permission"));
                    return true;
                }
                player.openInventory(plugin.getSettingsGUI().build(player));
            }
            case "reload" -> {
                if (sender instanceof Player player && !plugin.getPermissionManager().canOpenSettings(player)) {
                    player.sendMessage(plugin.getConfigManager().msg("no-permission"));
                    return true;
                }
                plugin.getConfigManager().reloadAll();
                plugin.getArtifactManager().loadPool();
                sender.sendMessage(plugin.getConfigManager().msg("reload-success"));
            }
            default -> sender.sendMessage(plugin.getConfigManager().msg("unknown-command"));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>(List.of("menu", "settings", "reload"));
            options.removeIf(o -> !o.startsWith(args[0].toLowerCase()));
            return options;
        }
        return List.of();
    }
}
