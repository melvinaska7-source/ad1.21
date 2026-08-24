package com.melviavas.dailyartifacts.util;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class ItemBuilder {

    private final ItemStack stack;
    private final ItemMeta meta;

    public ItemBuilder(Material material) {
        this.stack = new ItemStack(material);
        this.meta = stack.getItemMeta();
    }

    public ItemBuilder(ItemStack base) {
        this.stack = base.clone();
        this.meta = stack.getItemMeta();
    }

    public ItemBuilder name(String rawName) {
        if (meta != null) meta.setDisplayName(ColorUtil.colorize(rawName));
        return this;
    }

    public ItemBuilder lore(List<String> rawLore) {
        if (meta != null) meta.setLore(ColorUtil.colorize(rawLore));
        return this;
    }

    public ItemBuilder lore(String... rawLore) {
        List<String> list = new ArrayList<>();
        for (String s : rawLore) list.add(s);
        return lore(list);
    }

    public ItemBuilder amount(int amount) {
        stack.setAmount(Math.max(1, amount));
        return this;
    }

    public ItemStack build() {
        stack.setItemMeta(meta);
        return stack;
    }
}
