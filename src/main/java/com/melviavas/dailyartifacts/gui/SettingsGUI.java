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
 * Показывает весь пул возможных предметов (items.yml) для редактирования.
 * Управление (см. help-кнопку в самом меню):
 *   ЛКМ по предмету        — лимит +1
 *   ПКМ по предмету        — лимит -1
 *   Shift+ЛКМ по предмету  — удалить из пула
 *   Shift+ПКМ по предмету  — ввести новую цену в чат
 *   Shift+ЛКМ по предмету в СВОЁМ инвентаре (пока это меню открыто) — добавить в пул
 */
public class SettingsGUI {

    private final DailyArtifactsPlugin plugin;

    // slot -> id предмета, чтобы обработчик клика знал, что нажали
    private final Map<Integer, String> lastSlotMap = new LinkedHashMap<>();

    public SettingsGUI(DailyArtifactsPlugin plugin) {
        this.plugin = plugin;
    }

    public Inventory build(Player player) {
        FileConfiguration menuConfig = plugin.getConfigManager().getMenu();
        String title = ColorUtil.colorize(menuConfig.getString("settings-menu.title", "Настройка артефактов"));
        int size = menuConfig.getInt("settings-menu.size", 54);

        Inventory inv = plugin.getServer().createInventory(null, size, title);
        lastSlotMap.clear();

        int slot = 0;
        int reserved = size - 9; // нижний ряд под кнопки
        for (ArtifactItem item : plugin.getArtifactManager().getPool().values()) {
            if (slot >= reserved) break;
            inv.setItem(slot, buildIcon(item));
            lastSlotMap.put(slot, item.getId());
            slot++;
        }

        placeButton(inv, menuConfig, "save", org.bukkit.Material.LIME_DYE);
        placeButton(inv, menuConfig, "reset", org.bukkit.Material.BARRIER);
        placeButton(inv, menuConfig, "help", org.bukkit.Material.BOOK);

        return inv;
    }

    private void placeButton(Inventory inv, FileConfiguration menuConfig, String key, Material fallback) {
        String base = "settings-menu.buttons." + key;
        int slot = menuConfig.getInt(base + ".slot", -1);
        if (slot < 0) return;
        Material material = Material.matchMaterial(menuConfig.getString(base + ".material", fallback.name()));
        if (material == null) material = fallback;
        String name = menuConfig.getString(base + ".name", key);
        List<String> lore = menuConfig.getStringList(base + ".lore");
        inv.setItem(slot, new ItemBuilder(material).name(name).lore(lore).build());
    }

    private ItemStack buildIcon(ArtifactItem item) {
        String name = "&f" + item.getMaterial().name();
        List<String> lore = new ArrayList<>();
        lore.add("&7Цена: &a" + trim(item.getPrice()) + "$");
        lore.add("&7Лимит: &f" + item.getLimit());
        lore.add("&7Статус: " + (item.isEnabled() ? "&aвключён" : "&cвыключен"));
        return new ItemBuilder(item.getMaterial()).name(name).lore(lore).build();
    }

    public String getIdForSlot(int slot) {
        return lastSlotMap.get(slot);
    }

    private String trim(double v) {
        return v == Math.floor(v) ? String.valueOf((long) v) : String.valueOf(v);
    }
}
