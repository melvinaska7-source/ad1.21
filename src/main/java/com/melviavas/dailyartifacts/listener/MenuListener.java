package com.melviavas.dailyartifacts.listener;

import com.melviavas.dailyartifacts.DailyArtifactsPlugin;
import com.melviavas.dailyartifacts.manager.ChatInputManager;
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

        var menu = plugin.getConfigManager().getMenu();
        String mainTitle = ColorUtil.colorize(menu.getString("menu.title", ""));
        String settingsTitle = ColorUtil.colorize(menu.getString("settings-menu.title", ""));
        String lootTitle = ColorUtil.colorize(menu.getString("settings-menu.loot-title", ""));
        String updateTitle = ColorUtil.colorize(menu.getString("settings-menu.update-title", ""));
        String countTitle = ColorUtil.colorize(menu.getString("settings-menu.count-title", ""));
        String viewTitle = event.getView().getTitle();

        if (viewTitle.equals(mainTitle)) {
            event.setCancelled(true);
            handleMainMenuClick(player, event);
        } else if (viewTitle.equals(settingsTitle)) {
            event.setCancelled(true);
            handleSettingsRoot(player, event);
        } else if (viewTitle.equals(lootTitle)) {
            handleLootSettings(player, event);
        } else if (viewTitle.equals(updateTitle)) {
            event.setCancelled(true);
            handleUpdateSettings(player, event);
        } else if (viewTitle.equals(countTitle)) {
            event.setCancelled(true);
            handleCountSettings(player, event);
        }
    }

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
        if (toTurnIn <= 0) {
            player.sendMessage(plugin.getConfigManager().msg("not-enough-items"));
            return;
        }

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

    private void handleSettingsRoot(Player player, InventoryClickEvent event) {
        int slot = event.getSlot();
        if (slot == buttonSlot("loot")) {
            player.openInventory(plugin.getSettingsGUI().buildLoot(player));
        } else if (slot == buttonSlot("update")) {
            player.openInventory(plugin.getSettingsGUI().buildUpdate(player));
        } else if (slot == buttonSlot("count")) {
            player.openInventory(plugin.getSettingsGUI().buildCount(player));
        }
    }

    private void handleUpdateSettings(Player player, InventoryClickEvent event) {
        int slot = event.getSlot();
        if (slot == plugin.getConfigManager().getMenu().getInt("settings-menu.update-buttons.min.slot", 11)) {
            plugin.getChatInputManager().request(player.getUniqueId(), ChatInputManager.Type.UPDATE_MIN);
            player.closeInventory();
            player.sendMessage(plugin.getConfigManager().msg("enter-update-time-prompt"));
        } else if (slot == plugin.getConfigManager().getMenu().getInt("settings-menu.update-buttons.max.slot", 15)) {
            plugin.getChatInputManager().request(player.getUniqueId(), ChatInputManager.Type.UPDATE_MAX);
            player.closeInventory();
            player.sendMessage(plugin.getConfigManager().msg("enter-update-time-prompt"));
        }
    }

    private void handleCountSettings(Player player, InventoryClickEvent event) {
        int slot = event.getSlot();
        if (slot == plugin.getConfigManager().getMenu().getInt("settings-menu.count-buttons.min.slot", 11)) {
            plugin.getChatInputManager().request(player.getUniqueId(), ChatInputManager.Type.COUNT_MIN);
            player.closeInventory();
            player.sendMessage(plugin.getConfigManager().msg("enter-count-prompt"));
        } else if (slot == plugin.getConfigManager().getMenu().getInt("settings-menu.count-buttons.max.slot", 15)) {
            plugin.getChatInputManager().request(player.getUniqueId(), ChatInputManager.Type.COUNT_MAX);
            player.closeInventory();
            player.sendMessage(plugin.getConfigManager().msg("enter-count-prompt"));
        }
    }

    private int buttonSlot(String key) {
        return plugin.getConfigManager().getMenu().getInt("settings-menu.buttons." + key + ".slot", -1);
    }

    private void handleLootSettings(Player player, InventoryClickEvent event) {
        boolean topInventory = event.getClickedInventory() != null
                && event.getClickedInventory().equals(event.getView().getTopInventory());

        if (!topInventory) {
            if (event.isShiftClick() && event.getClick() == ClickType.SHIFT_LEFT) {
                ItemStack clicked = event.getCurrentItem();
                if (clicked == null || clicked.getType() == Material.AIR) return;
                event.setCancelled(true);

                int defaultPrice = plugin.getConfigManager().getConfig().getInt("defaults.price", 100);
                int defaultLimit = plugin.getConfigManager().getConfig().getInt("defaults.limit", 64);
                String id = clicked.getType().name().toLowerCase();

                plugin.getArtifactManager().addOrUpdateItem(id, clicked.getType(), defaultPrice, defaultLimit);
                player.sendMessage(plugin.getConfigManager().msg("item-added"));
                player.openInventory(plugin.getSettingsGUI().buildLoot(player));
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
            player.openInventory(plugin.getSettingsGUI().buildLoot(player));
            return;
        }
        if (isButtonSlot(slot, "help")) return;

        String id = plugin.getSettingsGUI().getIdForSlot(slot);
        if (id == null) return;
        ArtifactItem item = plugin.getArtifactManager().getById(id);
        if (item == null) return;

        ClickType click = event.getClick();

        if (click == ClickType.SHIFT_LEFT) {
            plugin.getArtifactManager().removeItem(id);
            player.sendMessage(plugin.getConfigManager().msg("item-removed"));
            player.openInventory(plugin.getSettingsGUI().buildLoot(player));
        } else if (click == ClickType.SHIFT_RIGHT) {
            plugin.getChatInputManager().requestPrice(player.getUniqueId(), id);
            player.closeInventory();
            player.sendMessage(plugin.getConfigManager().msg("enter-price-prompt"));
        } else if (click == ClickType.LEFT) {
            item.setLimit(item.getLimit() + 1);
            plugin.getArtifactManager().addOrUpdateItem(id, item.getMaterial(), item.getPrice(), item.getLimit());
            player.openInventory(plugin.getSettingsGUI().buildLoot(player));
        } else if (click == ClickType.RIGHT) {
            item.setLimit(Math.max(0, item.getLimit() - 1));
            plugin.getArtifactManager().addOrUpdateItem(id, item.getMaterial(), item.getPrice(), item.getLimit());
            player.openInventory(plugin.getSettingsGUI().buildLoot(player));
        }
    }

    private boolean isButtonSlot(int slot, String key) {
        return plugin.getConfigManager().getMenu()
                .getInt("settings-menu.loot-buttons." + key + ".slot", -1) == slot;
    }
}
