package com.melviavas.dailyartifacts.listener;

import com.melviavas.dailyartifacts.DailyArtifactsPlugin;
import com.melviavas.dailyartifacts.model.ArtifactItem;
import com.melviavas.dailyartifacts.util.ColorUtil;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class MenuListener implements Listener {

    private final DailyArtifactsPlugin plugin;

    public MenuListener(DailyArtifactsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String menuTitle = ColorUtil.colorize(plugin.getConfigManager().getMenu().getString("menu.title", ""));
        String settingsTitle = ColorUtil.colorize(plugin.getConfigManager().getMenu().getString("settings-menu.title", ""));
        String viewTitle = event.getView().getTitle();

        if (viewTitle.equals(menuTitle)) {
            event.setCancelled(true);
            handleMainMenuClick(player, event);
        } else if (viewTitle.equals(settingsTitle)) {
            handleSettingsClick(player, event);
        }
    }

    // ---------- главное меню сдачи предметов ----------

    private void handleMainMenuClick(Player player, InventoryClickEvent event) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        ArtifactItem match = null;
        for (ArtifactItem item : plugin.getArtifactManager().getActiveArtifacts()) {
            if (item.getMaterial() == clicked.getType()) {
                match = item;
                break;
            }
        }
        if (match == null) return;

        int toTurnIn = event.isShiftClick() ? countInInventory(player, match.getMaterial()) : 1;
        if (toTurnIn <= 0) return;

        turnIn(player, match, toTurnIn);
    }

    private void turnIn(Player player, ArtifactItem item, int requested) {
        if (!plugin.getEconomyManager().isReady()) {
            player.sendMessage(plugin.getConfigManager().msg("no-economy"));
            return;
        }

        int given = plugin.getDataManager().getProgress(player.getUniqueId(), item.getId());
        int room = item.getLimit() - given;
        if (room <= 0) {
            player.sendMessage(plugin.getConfigManager().msg("limit-reached")
                    .replace("{given}", String.valueOf(given))
                    .replace("{limit}", String.valueOf(item.getLimit())));
            playSound(player, "limit-reached");
            return;
        }

        int amount = Math.min(requested, room);
        amount = Math.min(amount, countInInventory(player, item.getMaterial()));
        if (amount <= 0) {
            player.sendMessage(plugin.getConfigManager().msg("not-enough-items"));
            return;
        }

        removeFromInventory(player, item.getMaterial(), amount);
        double total = item.getPrice() * amount;
        plugin.getEconomyManager().deposit(player, total);
        plugin.getDataManager().setProgress(player.getUniqueId(), item.getId(), given + amount);

        player.sendMessage(plugin.getConfigManager().msg("turn-in-success")
                .replace("{amount}", String.valueOf(amount))
                .replace("{item}", prettyName(item))
                .replace("{price}", trim(total)));
        playSound(player, "success");

        player.closeInventory();
        player.openInventory(plugin.getArtifactMenuGUI().build(player));
    }

    private String prettyName(ArtifactItem item) {
        String raw = item.getMaterial().name().replace("_", " ").toLowerCase();
        return raw.substring(0, 1).toUpperCase() + raw.substring(1);
    }

    private void playSound(Player player, String key) {
        if (!plugin.getConfigManager().getConfig().getBoolean("sounds.enabled", true)) return;
        String soundName = plugin.getConfigManager().getConfig().getString("sounds." + key, "");
        try {
            Sound sound = Sound.valueOf(soundName);
            float volume = (float) plugin.getConfigManager().getConfig().getDouble("sounds.volume", 1.0);
            float pitch = (float) plugin.getConfigManager().getConfig().getDouble("sounds.pitch", 1.0);
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (IllegalArgumentException ignored) {
        }
    }

    private int countInInventory(Player player, Material material) {
        int count = 0;
        for (ItemStack stack : player.getInventory().getStorageContents()) {
            if (stack != null && stack.getType() == material) count += stack.getAmount();
        }
        return count;
    }

    private void removeFromInventory(Player player, Material material, int amount) {
        int remaining = amount;
        ItemStack[] contents = player.getInventory().getStorageContents();
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack stack = contents[i];
            if (stack == null || stack.getType() != material) continue;
            int take = Math.min(remaining, stack.getAmount());
            stack.setAmount(stack.getAmount() - take);
            remaining -= take;
            contents[i] = stack.getAmount() <= 0 ? null : stack;
        }
        player.getInventory().setStorageContents(contents);
    }

    private String trim(double v) {
        return v == Math.floor(v) ? String.valueOf((long) v) : String.valueOf(v);
    }

    // ---------- меню настроек ----------

    private void handleSettingsClick(Player player, InventoryClickEvent event) {
        boolean topInventory = event.getClickedInventory() != null
                && event.getClickedInventory().equals(event.getView().getTopInventory());

        if (!topInventory) {
            // клик по СВОЕМУ инвентарю, пока открыто /ad settings
            if (event.isShiftClick() && event.getClick() == ClickType.SHIFT_LEFT) {
                ItemStack clicked = event.getCurrentItem();
                if (clicked == null || clicked.getType() == Material.AIR) return;
                event.setCancelled(true);

                int defaultPrice = plugin.getConfigManager().getConfig().getInt("defaults.price", 100);
                int defaultLimit = plugin.getConfigManager().getConfig().getInt("defaults.limit", 64);
                String id = clicked.getType().name().toLowerCase();

                plugin.getArtifactManager().addOrUpdateItem(id, clicked.getType(), defaultPrice, defaultLimit);
                player.sendMessage(plugin.getConfigManager().msg("item-added"));
                player.openInventory(plugin.getSettingsGUI().build(player));
            }
            return;
        }

        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        int slot = event.getSlot();

        if (isButtonSlot(slot, "save")) {
            player.sendMessage(plugin.getConfigManager().msg("settings-saved"));
            player.closeInventory();
            return;
        }
        if (isButtonSlot(slot, "reset")) {
            for (String id : new java.util.ArrayList<>(plugin.getArtifactManager().getPool().keySet())) {
                plugin.getArtifactManager().removeItem(id);
            }
            player.sendMessage(plugin.getConfigManager().msg("settings-reset"));
            player.openInventory(plugin.getSettingsGUI().build(player));
            return;
        }
        if (isButtonSlot(slot, "help")) {
            return;
        }

        String id = plugin.getSettingsGUI().getIdForSlot(slot);
        if (id == null) return;
        ArtifactItem item = plugin.getArtifactManager().getById(id);
        if (item == null) return;

        ClickType click = event.getClick();

        if (click == ClickType.SHIFT_LEFT) {
            plugin.getArtifactManager().removeItem(id);
            player.sendMessage(plugin.getConfigManager().msg("item-removed"));
            player.openInventory(plugin.getSettingsGUI().build(player));
        } else if (click == ClickType.SHIFT_RIGHT) {
            plugin.getChatInputManager().requestPrice(player.getUniqueId(), id);
            player.closeInventory();
            player.sendMessage(plugin.getConfigManager().msg("enter-price-prompt"));
        } else if (click == ClickType.LEFT) {
            item.setLimit(item.getLimit() + 1);
            plugin.getArtifactManager().addOrUpdateItem(id, item.getMaterial(), item.getPrice(), item.getLimit());
            player.openInventory(plugin.getSettingsGUI().build(player));
        } else if (click == ClickType.RIGHT) {
            item.setLimit(Math.max(0, item.getLimit() - 1));
            plugin.getArtifactManager().addOrUpdateItem(id, item.getMaterial(), item.getPrice(), item.getLimit());
            player.openInventory(plugin.getSettingsGUI().build(player));
        }
    }

    private boolean isButtonSlot(int slot, String key) {
        int configured = plugin.getConfigManager().getMenu().getInt("settings-menu.buttons." + key + ".slot", -1);
        return configured == slot;
    }
}
