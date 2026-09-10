package com.opentensor;

import net.minecraft.world.item.Rarity;

/**
 * The four Tensor DDR5 memory sticks.
 *
 * <p>Labels advertise 16/32/64/128 GB while the actual amounts granted to
 * the machine are 32/64/128/256 MB (in OC kilobytes). All sticks report
 * driver tier 2, so they install into Tier 3 (and better) memory slots,
 * exactly like high-end stock RAM.</p>
 */
public enum TensorMemorySpec {
    DDR5_16GB("ddr5_16gb", "16GB", 32768, Rarity.RARE),
    DDR5_32GB("ddr5_32gb", "32GB", 65536, Rarity.RARE),
    DDR5_64GB("ddr5_64gb", "64GB", 131072, Rarity.EPIC),
    DDR5_128GB("ddr5_128gb", "128GB", 262144, Rarity.EPIC);

    private final String id;
    private final String label;
    private final double kilobytes;
    private final Rarity rarity;

    TensorMemorySpec(String id, String label, double kilobytes, Rarity rarity) {
        this.id = id;
        this.label = label;
        this.kilobytes = kilobytes;
        this.rarity = rarity;
    }

    /** Registry path under the {@code opentensor} namespace. */
    public String id() {
        return id;
    }

    /** Advertised size for names and tooltips (e.g. {@code 16GB}). */
    public String label() {
        return label;
    }

    /** Actual RAM granted to the machine, in OC kilobytes. */
    public double kilobytes() {
        return kilobytes;
    }

    public Rarity rarity() {
        return rarity;
    }

    public static TensorMemorySpec byId(String id) {
        for (TensorMemorySpec spec : values()) {
            if (spec.id.equals(id)) {
                return spec;
            }
        }
        return null;
    }
}
