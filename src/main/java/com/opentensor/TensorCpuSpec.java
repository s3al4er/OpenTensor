package com.opentensor;

import net.minecraft.world.item.Rarity;

/**
 * The two Tensor server CPUs.
 */
public enum TensorCpuSpec {
    EPYC_9354P("epyc_9354p", "AMD EPYC 9354P", 32, 3.0, Rarity.EPIC),
    EPYC_7303P("epyc_7303p", "AMD EPYC 7303P", 24, 2.5, Rarity.RARE);

    private final String id;
    private final String product;
    private final int components;
    private final double callBudget;
    private final Rarity rarity;

    TensorCpuSpec(String id, String product, int components, double callBudget, Rarity rarity) {
        this.id = id;
        this.product = product;
        this.components = components;
        this.callBudget = callBudget;
        this.rarity = rarity;
    }

    /** Registry path under the {@code opentensor} namespace. */
    public String id() {
        return id;
    }

    /** Branding reported via the {@code cpu} device info. */
    public String product() {
        return product;
    }

    /** Number of components supported when installed. */
    public int components() {
        return components;
    }

    /** Call budget (machine speed) granted when installed. */
    public double callBudget() {
        return callBudget;
    }

    public Rarity rarity() {
        return rarity;
    }

    public static TensorCpuSpec byId(String id) {
        for (TensorCpuSpec spec : values()) {
            if (spec.id.equals(id)) {
                return spec;
            }
        }
        return null;
    }
}
