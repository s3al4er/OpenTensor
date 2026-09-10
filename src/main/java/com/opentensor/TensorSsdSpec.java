package com.opentensor;

import net.minecraft.world.item.Rarity;

/**
 * Tensor SSDs.
 *
 * <p>Labels advertise gigabytes while the actual capacities granted to the
 * machine are megabytes (in OC kilobytes). All sticks report driver tier 2,
 * so they install into Tier 3 (and better) HDD slots.</p>
 */
public enum TensorSsdSpec {
    SSD_512GB("ssd_512gb", "Samsung SSD 840 Pro", "512GB", 524288, Rarity.EPIC);

    private final String id;
    private final String product;
    private final String label;
    private final int kilobytes;
    private final Rarity rarity;

    TensorSsdSpec(String id, String product, String label, int kilobytes, Rarity rarity) {
        this.id = id;
        this.product = product;
        this.label = label;
        this.kilobytes = kilobytes;
        this.rarity = rarity;
    }

    /** Registry path under the {@code opentensor} namespace. */
    public String id() {
        return id;
    }

    /** Branding for names and tooltips. */
    public String product() {
        return product;
    }

    /** Advertised size for names and tooltips (e.g. {@code 512GB}). */
    public String label() {
        return label;
    }

    /** Actual capacity granted to the machine, in OC kilobytes. */
    public int kilobytes() {
        return kilobytes;
    }

    public Rarity rarity() {
        return rarity;
    }

    public static TensorSsdSpec byId(String id) {
        for (TensorSsdSpec spec : values()) {
            if (spec.id.equals(id)) {
                return spec;
            }
        }
        return null;
    }
}
