package com.melviavas.dailyartifacts.gui;

import com.melviavas.dailyartifacts.DailyArtifactsPlugin;
import com.melviavas.dailyartifacts.model.ArtifactItem;
import com.melviavas.dailyartifacts.util.ColorUtil;
import com.melviavas.dailyartifacts.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Два уровня настроек:
 * 1) корневое /ad settings с тремя разделами;
 * 2) отдельное меню настройки лута.
 *
 * enabled намеренно не редактируется через GUI — только items.yml.
 */
public class SettingsGUI {

    private final DailyArtifactsPlugin plugin;
    private final Map<Integer, String> lastSlotMap = new LinkedHashMap<>();

    public SettingsGUI(DailyArtifactsPlugin plugin) {
        this.plugin = plugin;
    }

    public Inventory build(Player player) {
        FileConfiguration c = plugin.getConfigManager().getMenu();
        String title = ColorUtil.colorize(c.getString("settings-menu.title", "Настройки Daily Artifacts"));
        int size = c.getInt("settings-menu.size", 27);
        Inventory inv = plugin.getServer().createInventory(null, size, title);

        placeButton(inv, c, "loot", Material.CHEST);
        placeButton(inv, c, "update", Material.CLOCK);
        placeButton(inv, c, "count", Material.NETHER_STAR);
        return inv;
    }

    public Inventory buildLoot(Player player) {
        FileConfiguration c = plugin.getConfigManager().getMenu();
        String title = ColorUtil.colorize(c.getString("settings-menu.loot-title", "Настройка лута"));
        int size = c.getInt("settings-menu.loot-size", 54);
        Inventory inv = plugin.getServer().createInventory(null, size, title);
        lastSlotMap.clear();

        int reserved = size - 9;
        int slot = 0;
        for (ArtifactItem item : plugin.getArtifactManager().getPool().values()) {
            if (slot >= reserved) break;
            inv.setItem(slot, buildIcon(item));
            lastSlotMap.put(slot, item.getId());
            slot++;
        }

        placeLootButton(inv, c, "save", Material.LIME_DYE);
        placeLootButton(inv, c, "reset", Material.BARRIER);
        placeLootButton(inv, c, "help", Material.BOOK);
        return inv;
    }

    public Inventory buildUpdate(Player player) {
        FileConfiguration c = plugin.getConfigManager().getMenu();
        String title = ColorUtil.colorize(c.getString("settings-menu.update-title", "Настройка обновления"));
        int size = c.getInt("settings-menu.update-size", 27);
        Inventory inv = plugin.getServer().createInventory(null, size, title);

        int minSlot = c.getInt("settings-menu.update-buttons.min.slot", 11);
        int maxSlot = c.getInt("settings-menu.update-buttons.max.slot", 15);

        inv.setItem(minSlot, new ItemBuilder(Material.CLOCK)
                .name(c.getString("settings-menu.update-buttons.min.name", "&eМинимальное обновление"))
                .lore(List.of(
                        "&7Сейчас: &f" + plugin.getConfigManager().getConfig().getString("update-time.min", "1h"),
                        "&8ЛКМ — изменить через чат"
                )).build());

        inv.setItem(maxSlot, new ItemBuilder(Material.CLOCK)
                .name(c.getString("settings-menu.update-buttons.max.name", "&eМаксимальное обновление"))
                .lore(List.of(
                        "&7Сейчас: &f" + plugin.getConfigManager().getConfig().getString("update-time.max", "7d"),
                        "&8ЛКМ — изменить через чат"
                )).build());
        return inv;
    }

    public Inventory buildCount(Player player) {
        FileConfiguration c = plugin.getConfigManager().getMenu();
        String title = ColorUtil.colorize(c.getString("settings-menu.count-title", "Количество артефактов"));
        int size = c.getInt("settings-menu.count-size", 27);
        Inventory inv = plugin.getServer().createInventory(null, size, title);

        int minSlot = c.getInt("settings-menu.count-buttons.min.slot", 11);
        int maxSlot = c.getInt("settings-menu.count-buttons.max.slot", 15);

        inv.setItem(minSlot, new ItemBuilder(Material.CHEST)
                .name(c.getString("settings-menu.count-buttons.min.name", "&eМинимальное количество"))
                .lore(List.of(
                        "&7Сейчас: &f" + plugin.getConfigManager().getConfig().getInt("artifact-count.min", 1),
                        "&8ЛКМ — изменить через чат"
                )).build());

        inv.setItem(maxSlot, new ItemBuilder(Material.CHEST)
                .name(c.getString("settings-menu.count-buttons.max.name", "&eМаксимальное количество"))
                .lore(List.of(
                        "&7Сейчас: &f" + plugin.getConfigManager().getConfig().getInt("artifact-count.max", 5),
                        "&8ЛКМ — изменить через чат"
                )).build());
        return inv;
    }

    private void placeLootButton(Inventory inv, FileConfiguration c, String key, Material fallback) {
        String base = "settings-menu.loot-buttons." + key;
        int slot = c.getInt(base + ".slot", -1);
        if (slot < 0 || slot >= inv.getSize()) return;
        Material material = Material.matchMaterial(c.getString(base + ".material", fallback.name()));
        if (material == null) material = fallback;
        String name = c.getString(base + ".name", key);
        List<String> lore = c.getStringList(base + ".lore");
        inv.setItem(slot, new ItemBuilder(material).name(name).lore(lore).build());
    }

    private void placeButton(Inventory inv, FileConfiguration c, String key, Material fallback) {
        String base = "settings-menu.buttons." + key;
        int slot = c.getInt(base + ".slot", -1);
        if (slot < 0 || slot >= inv.getSize()) return;
        Material material = Material.matchMaterial(c.getString(base + ".material", fallback.name()));
        if (material == null) material = fallback;
        String name = c.getString(base + ".name", key);
        List<String> lore = c.getStringList(base + ".lore");
        inv.setItem(slot, new ItemBuilder(material).name(name).lore(lore).build());
    }

    private ItemStack buildIcon(ArtifactItem item) {
        String name = "&f" + item.getMaterial().name().replace("_", " ");
        List<String> lore = new ArrayList<>();
        lore.add("&7Цена: &a" + trim(item.getPrice()) + "$");
        lore.add("&7Лимит: &f" + item.getLimit());
        lore.add("&8");
        lore.add("&7Shift+ЛКМ — удалить");
        lore.add("&7Shift+ПКМ — изменить цену");
        lore.add("&7Колёсико — изменить лимит");
        return new ItemBuilder(item.getMaterial()).name(name).lore(lore).build();
    }

    public String getIdForSlot(int slot) {
        return lastSlotMap.get(slot);
    }

    private String trim(double v) {
        return v == Math.floor(v) ? String.valueOf((long) v) : String.valueOf(v);
    }
}
