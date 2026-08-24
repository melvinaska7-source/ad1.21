package com.melviavas.dailyartifacts.gui;

import com.melviavas.dailyartifacts.DailyArtifactsPlugin;
import com.melviavas.dailyartifacts.model.ArtifactItem;
import com.melviavas.dailyartifacts.util.ColorUtil;
import com.melviavas.dailyartifacts.util.ItemBuilder;
import com.melviavas.dailyartifacts.util.TimeUtil;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ArtifactMenuGUI {

    public static final String TITLE_KEY = "menu.title";

    private final DailyArtifactsPlugin plugin;

    public ArtifactMenuGUI(DailyArtifactsPlugin plugin) {
        this.plugin = plugin;
    }

    public Inventory build(Player player) {
        var menuConfig = plugin.getConfigManager().getMenu();
        String title = ColorUtil.colorize(menuConfig.getString("menu.title", "Артефакты дня"));
        int size = menuConfig.getInt("menu.size", 45);

        GuiHolder holder = new GuiHolder(GuiHolder.Type.ARTIFACTS);
        Inventory inv = plugin.getServer().createInventory(holder, size, title);
        holder.setInventory(inv);

        if (menuConfig.getBoolean("menu.border.enabled", true)) {
            fillBorder(inv, menuConfig);
        }

        List<Integer> slots = menuConfig.getIntegerList("menu.artifact-slots");
        List<ArtifactItem> active = plugin.getArtifactManager().getActiveArtifacts();

        // Если по какой-то причине состояние оказалось пустым (например, после
        // ручного удаления state.yml), автоматически создаём новую ротацию.
        if (active.isEmpty() && !plugin.getArtifactManager().getPool().isEmpty()) {
            plugin.getArtifactManager().rotate();
            active = plugin.getArtifactManager().getActiveArtifacts();
        }

        for (int i = 0; i < active.size() && i < slots.size(); i++) {
            ArtifactItem item = active.get(i);
            inv.setItem(slots.get(i), buildArtifactIcon(player, item, menuConfig));
        }

        int clockSlot = menuConfig.getInt("menu.clock-slot", 40);
        inv.setItem(clockSlot, buildClockIcon(menuConfig));

        return inv;
    }

    private void fillBorder(Inventory inv, org.bukkit.configuration.file.FileConfiguration menuConfig) {
        var material = org.bukkit.Material.matchMaterial(
                menuConfig.getString("menu.border.material", "GRAY_STAINED_GLASS_PANE"));
        if (material == null) return;
        String name = menuConfig.getString("menu.border.name", " ");
        ItemStack pane = new ItemBuilder(material).name(name).build();

        List<Integer> artifactSlots = menuConfig.getIntegerList("menu.artifact-slots");
        int clockSlot = menuConfig.getInt("menu.clock-slot", 40);

        for (int i = 0; i < inv.getSize(); i++) {
            if (artifactSlots.contains(i) || i == clockSlot) continue;
            inv.setItem(i, pane);
        }
    }

    private ItemStack buildArtifactIcon(Player player, ArtifactItem item, org.bukkit.configuration.file.FileConfiguration menuConfig) {
        int given = plugin.getDataManager().getProgress(player.getUniqueId(), item.getId());

        String name = menuConfig.getString("item-display.name", "{name}")
                .replace("{name}", prettyName(item));

        List<String> loreLines = menuConfig.getStringList("item-display.lore");
        List<String> lore = new ArrayList<>();
        for (String line : loreLines) {
            lore.add(replacePlaceholders(line, item, given));
        }
        if (given >= item.getLimit()) {
            lore.add(replacePlaceholders(
                    menuConfig.getString("item-display.lore-limit-reached-line", ""), item, given));
        }

        return new ItemBuilder(item.getMaterial())
                .name(name)
                .lore(lore)
                .build();
    }

    private ItemStack buildClockIcon(org.bukkit.configuration.file.FileConfiguration menuConfig) {
        var material = org.bukkit.Material.matchMaterial(menuConfig.getString("menu.clock-item", "CLOCK"));
        if (material == null) material = org.bukkit.Material.CLOCK;

        String time = TimeUtil.formatRemaining(
                plugin.getArtifactManager().getMillisUntilNextUpdate(),
                plugin.getConfigManager().getConfig());

        String name = menuConfig.getString("menu.clock-name", "До обновления").replace("{time}", time);
        List<String> lore = new ArrayList<>();
        for (String line : menuConfig.getStringList("menu.clock-lore")) {
            lore.add(line.replace("{time}", time));
        }

        return new ItemBuilder(material).name(name).lore(lore).build();
    }

    private String replacePlaceholders(String line, ArtifactItem item, int given) {
        int left = Math.max(0, item.getLimit() - given);
        return line
                .replace("{name}", prettyName(item))
                .replace("{price}", trimNumber(item.getPrice()))
                .replace("{limit}", String.valueOf(item.getLimit()))
                .replace("{given}", String.valueOf(given))
                .replace("{left}", String.valueOf(left));
    }

    private String prettyName(ArtifactItem item) {
        String raw = item.getMaterial().name().replace("_", " ").toLowerCase();
        return raw.substring(0, 1).toUpperCase() + raw.substring(1);
    }

    private String trimNumber(double value) {
        if (value == Math.floor(value)) return String.valueOf((long) value);
        return String.valueOf(value);
    }
}
