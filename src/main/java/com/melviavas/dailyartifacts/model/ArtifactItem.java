package com.melviavas.dailyartifacts.model;

import org.bukkit.Material;

/**
 * Один возможный артефакт из items.yml.
 * id — ключ в items.yml (не отображается игроку напрямую).
 */
public class ArtifactItem {

    private final String id;
    private boolean enabled;
    private Material material;
    private double price;
    private int limit;

    public ArtifactItem(String id, boolean enabled, Material material, double price, int limit) {
        this.id = id;
        this.enabled = enabled;
        this.material = material;
        this.price = price;
        this.limit = limit;
    }

    public String getId() { return id; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public Material getMaterial() { return material; }
    public void setMaterial(Material material) { this.material = material; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public int getLimit() { return limit; }
    public void setLimit(int limit) { this.limit = limit; }
}
