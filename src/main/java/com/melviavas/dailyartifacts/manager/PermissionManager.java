package com.melviavas.dailyartifacts.manager;

import com.melviavas.dailyartifacts.DailyArtifactsPlugin;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * permission_mode в config.yml:
 *   simple — только OP/не-OP (settings/menu: "op" или "deop")
 *   pex/lp — OP ИЛИ игрок состоит в одной из групп из settings-groups/menu-groups
 *            (проверяется через ноду permission-плагина: dailyartifacts.settings / dailyartifacts.menu,
 *            которую администратор сам выдаёт нужным группам в PermissionsEx/LuckPerms).
 *
 * В любом режиме permission-нода dailyartifacts.settings/menu из plugin.yml тоже уважается —
 * если игроку явно выдали ноду через любой permission-плагин, доступ будет разрешён.
 */
public class PermissionManager {

    private final DailyArtifactsPlugin plugin;

    public PermissionManager(DailyArtifactsPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean canOpenMenu(Player player) {
        return check(player, "menu", "dailyartifacts.menu");
    }

    public boolean canOpenSettings(Player player) {
        return check(player, "settings", "dailyartifacts.settings");
    }

    private boolean check(Player player, String key, String node) {
        if (player.isOp()) return true;
        if (player.hasPermission(node)) return true;

        String mode = plugin.getConfigManager().getConfig()
                .getString("permissions.permission_mode", "simple");

        if (mode.equalsIgnoreCase("simple")) {
            String required = plugin.getConfigManager().getConfig()
                    .getString("permissions." + key, "deop");
            return !required.equalsIgnoreCase("op"); // "deop" -> разрешено всем не-op тоже
        }

        // pex / lp — полагаемся на permission-ноду (проверена выше) и на список групп,
        // просто как информативный список для администратора; фактическая выдача прав
        // происходит через сам permission-плагин.
        List<String> groups = plugin.getConfigManager().getConfig()
                .getStringList("permissions." + key + "-groups");
        return groups.isEmpty();
    }
}
