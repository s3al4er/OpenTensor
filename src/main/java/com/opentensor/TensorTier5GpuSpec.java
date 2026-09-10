package com.opentensor;

import net.minecraft.world.item.Rarity;

/**
 * Tier 5 Tensor GPUs (380x120, 16-bit).
 */
public enum TensorTier5GpuSpec {
    RTX_5090_32GB("rtx_5090_32gb", "NVIDIA", "NVIDIA GeForce RTX 5090", "32GB", 6.4, 0.45, "rtx_5090", Rarity.EPIC);

    private final String id;
    private final String vendor;
    private final String product;
    private final String memory;
    private final double vramScreens;
    private final double costScale;
    private final String texture;
    private final Rarity rarity;

    TensorTier5GpuSpec(String id, String vendor, String product, String memory, double vramScreens, double costScale, String texture, Rarity rarity) {
        this.id = id;
        this.vendor = vendor;
        this.product = product;
        this.memory = memory;
        this.vramScreens = vramScreens;
        this.costScale = costScale;
        this.texture = texture;
        this.rarity = rarity;
    }

    /** Registry path under the {@code opentensor} namespace. */
    public String id() {
        return id;
    }

    /** Vendor branding reported via the {@code gpu} component's device info. */
    public String vendor() {
        return vendor;
    }

    /** Branding reported via the {@code gpu} component's device info. */
    public String product() {
        return product;
    }

    /** Human-readable memory size for tooltips. */
    public String memory() {
        return memory;
    }

    public double vramScreens() {
        return vramScreens;
    }

    public double costScale() {
        return costScale;
    }

    public String texture() {
        return texture;
    }

    public Rarity rarity() {
        return rarity;
    }

    public static TensorTier5GpuSpec byId(String id) {
        for (TensorTier5GpuSpec spec : values()) {
            if (spec.id.equals(id)) {
                return spec;
            }
        }
        return null;
    }
}
