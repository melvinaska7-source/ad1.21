package com.melviavas.dailyartifacts.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.LinkedHashMap;
import java.util.Map;

/** Надёжно определяет тип GUI без сравнения цветных заголовков. */
public final class GuiHolder implements InventoryHolder {
    public enum Type { ARTIFACTS, SETTINGS, LOOT, UPDATE, COUNT }

    private final Type type;
    private final Map<Integer, String> slotIds = new LinkedHashMap<>();
    private Inventory inventory;

    public GuiHolder(Type type) {
        this.type = type;
    }

    public Type getType() { return type; }

    public Map<Integer, String> getSlotIds() { return slotIds; }

    public void setInventory(Inventory inventory) { this.inventory = inventory; }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
