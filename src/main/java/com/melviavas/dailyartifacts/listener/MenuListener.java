package com.melviavas.dailyartifacts.listener;

import com.melviavas.dailyartifacts.DailyArtifactsPlugin;
import com.melviavas.dailyartifacts.gui.GuiHolder;
import com.melviavas.dailyartifacts.manager.ChatInputManager;
import com.melviavas.dailyartifacts.model.ArtifactItem;
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

    public MenuListener(DailyArtifactsPlugin plugin) { this.plugin = plugin; }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getView().getTopInventory().getHolder() instanceof GuiHolder holder)) return;

        switch (holder.getType()) {
            case ARTIFACTS -> {
                event.setCancelled(true);
                if (event.getClickedInventory() != event.getView().getTopInventory()) return;
                handleMainMenuClick(player, event);
            }
            case SETTINGS -> {
                event.setCancelled(true);
                if (event.getClickedInventory() != event.getView().getTopInventory()) return;
                handleSettingsRoot(player, event);
            }
            case LOOT -> handleLootSettings(player, event, holder);
            case UPDATE -> {
                event.setCancelled(true);
                if (event.getClickedInventory() != event.getView().getTopInventory()) return;
                handleUpdateSettings(player, event);
            }
            case COUNT -> {
                event.setCancelled(true);
                if (event.getClickedInventory() != event.getView().getTopInventory()) return;
                handleCountSettings(player, event);
            }
        }
    }

    private void handleMainMenuClick(Player player, InventoryClickEvent event) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        ArtifactItem match = null;
        for (ArtifactItem item : plugin.getArtifactManager().getActiveArtifacts()) {
            if (item.getMaterial() == clicked.getType()) { match = item; break; }
        }
        if (match == null) return;

        int requested = event.isShiftClick() ? countInInventory(player, match.getMaterial()) : 1;
        if (requested <= 0) {
            player.sendMessage(plugin.getConfigManager().msg("not-enough-items"));
            return;
        }
        turnIn(player, match, requested);
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

        int amount = Math.min(Math.min(requested, room), countInInventory(player, item.getMaterial()));
        if (amount <= 0) {
            player.sendMessage(plugin.getConfigManager().msg("not-enough-items"));
            return;
        }

        double total = item.getPrice() * amount;

        // Сначала убеждаемся, что способ выплаты реально доступен.
        // Только после успешной выплаты удаляем предметы и записываем прогресс.
        if (!plugin.getEconomyManager().deposit(player, total)) {
            player.sendMessage(plugin.getConfigManager().msg("no-economy"));
            return;
        }

        removeFromInventory(player, item.getMaterial(), amount);
        plugin.getDataManager().setProgress(player.getUniqueId(), item.getId(), given + amount);

        player.sendMessage(plugin.getConfigManager().msg("turn-in-success")
                .replace("{amount}", String.valueOf(amount))
                .replace("{item}", prettyName(item))
                .replace("{price}", trim(total)));
        playSound(player, "success");
        player.openInventory(plugin.getArtifactMenuGUI().build(player));
    }

    private void handleSettingsRoot(Player player, InventoryClickEvent event) {
        int slot = event.getSlot();
        if (slot == buttonSlot("loot")) player.openInventory(plugin.getSettingsGUI().buildLoot(player));
        else if (slot == buttonSlot("update")) player.openInventory(plugin.getSettingsGUI().buildUpdate(player));
        else if (slot == buttonSlot("count")) player.openInventory(plugin.getSettingsGUI().buildCount(player));
        else if (slot == buttonSlot("refresh")) {
            plugin.getArtifactManager().loadPool();
            plugin.getArtifactManager().rotate();
            player.sendMessage(plugin.getConfigManager().msg("artifacts-refreshed"));
            player.openInventory(plugin.getSettingsGUI().build(player));
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

    private void handleLootSettings(Player player, InventoryClickEvent event, GuiHolder holder) {
        boolean top = event.getClickedInventory() == event.getView().getTopInventory();

        if (!top) {
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

        if (isButtonSlot(slot, "save")) { player.sendMessage(plugin.getConfigManager().msg("settings-saved")); player.closeInventory(); return; }
        if (isButtonSlot(slot, "reset")) {
            for (String id : new java.util.ArrayList<>(plugin.getArtifactManager().getPool().keySet())) plugin.getArtifactManager().removeItem(id);
            player.sendMessage(plugin.getConfigManager().msg("settings-reset"));
            player.openInventory(plugin.getSettingsGUI().buildLoot(player));
            return;
        }
        if (isButtonSlot(slot, "help")) return;

        String id = holder.getSlotIds().get(slot);
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
        } else if (click == ClickType.MIDDLE) {
            plugin.getChatInputManager().requestLimit(player.getUniqueId(), id);
            player.closeInventory();
            player.sendMessage(plugin.getConfigManager().msg("enter-limit-prompt"));
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

    private int buttonSlot(String key) { return plugin.getConfigManager().getMenu().getInt("settings-menu.buttons." + key + ".slot", -1); }
    private boolean isButtonSlot(int slot, String key) { return plugin.getConfigManager().getMenu().getInt("settings-menu.loot-buttons." + key + ".slot", -1) == slot; }

    private int countInInventory(Player player, Material material) {
        int count = 0;
        for (ItemStack stack : player.getInventory().getStorageContents()) if (stack != null && stack.getType() == material) count += stack.getAmount();
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

    private void playSound(Player player, String key) {
        if (!plugin.getConfigManager().getConfig().getBoolean("sounds.enabled", true)) return;
        String soundName = plugin.getConfigManager().getConfig().getString("sounds." + key, "");
        try {
            Sound sound = Sound.valueOf(soundName);
            player.playSound(player.getLocation(), sound, (float)plugin.getConfigManager().getConfig().getDouble("sounds.volume", 1.0), (float)plugin.getConfigManager().getConfig().getDouble("sounds.pitch", 1.0));
        } catch (IllegalArgumentException ignored) { }
    }

    private String prettyName(ArtifactItem item) { String raw=item.getMaterial().name().replace("_"," ").toLowerCase(); return raw.substring(0,1).toUpperCase()+raw.substring(1); }
    private String trim(double v) { return v == Math.floor(v) ? String.valueOf((long)v) : String.valueOf(v); }
}
