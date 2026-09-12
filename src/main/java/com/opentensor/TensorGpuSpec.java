package com.opentensor;

import net.minecraft.world.item.Rarity;

/**
 * The twenty-seven Tensor GPUs.
 *
 * <p>All cards share the top-end display specs (190x60, 8-bit, Tier-3-class
 * call budget) and differ in three respects:</p>
 * <ul>
 *   <li>{@code vramScreens}: VRAM multiplier relative to one full screen of
 *   cells. Stock Tier 3 uses 4.0; most Tensor cards use 3.2-28.0 and report
 *   more {@code totalMemory} than a stock Tier 3 card, while the low-end
 *   GT 730 / RX 580 honestly report less.</li>
 *   <li>{@code costScale}: scales call-budget and energy costs, so the
 *   stronger cards also operate faster/cheaper than stock.</li>
 *   <li>{@code vendor}: branding reported via the {@code gpu} component's
   *   device info (NVIDIA, AMD or Google).</li>
 * </ul>
 */
public enum TensorGpuSpec {
    A100_40GB("a100_40gb", "NVIDIA", "NVIDIA A100 40GB", "40GB", 8.0, 1.0, "a100", Rarity.RARE),
    A100_80GB("a100_80gb", "NVIDIA", "NVIDIA A100 80GB", "80GB", 16.0, 0.85, "a100", Rarity.RARE),
    H100_80GB("h100_80gb", "NVIDIA", "NVIDIA H100 80GB", "80GB", 16.0, 0.7, "h100", Rarity.RARE),
    H100_96GB("h100_96gb", "NVIDIA", "NVIDIA H100 96GB", "96GB", 20.0, 0.6, "h100", Rarity.EPIC),
    H200_141GB("h200_141gb", "NVIDIA", "NVIDIA H200 141GB", "141GB", 28.0, 0.5, "h200", Rarity.EPIC),
    RTX_PRO_6000_BLACKWELL_96GB("rtx_pro_6000_blackwell_96gb", "NVIDIA", "NVIDIA RTX PRO 6000 Blackwell 96GB", "96GB", 20.0, 0.55, "rtx_pro_6000_blackwell", Rarity.EPIC),
    RTX_PRO_6000_ADA_48GB("rtx_pro_6000_ada_48gb", "NVIDIA", "NVIDIA RTX PRO 6000 Ada", "48GB", 9.6, 0.6, "rtx_pro_6000_ada", Rarity.EPIC),
    TESLA_P40_24GB("tesla_p40_24gb", "NVIDIA", "NVIDIA Tesla P40", "24GB", 4.8, 1.2, "tesla_p40", Rarity.UNCOMMON),
    TESLA_T4_16GB("tesla_t4_16gb", "NVIDIA", "NVIDIA Tesla T4", "16GB", 3.2, 1.1, "tesla_t4", Rarity.UNCOMMON),
    RTX_3090_24GB("rtx_3090_24gb", "NVIDIA", "NVIDIA GeForce RTX 3090", "24GB", 4.8, 0.9, "rtx_3090", Rarity.RARE),
    GT_730_1GB("gt_730_1gb", "NVIDIA", "NVIDIA GeForce GT 730", "1GB", 0.2, 1.5, "gt730", Rarity.COMMON),
    GT_730_2GB("gt_730_2gb", "NVIDIA", "NVIDIA GeForce GT 730", "2GB", 0.4, 1.5, "gt730", Rarity.COMMON),
    GT_730_4GB("gt_730_4gb", "NVIDIA", "NVIDIA GeForce GT 730", "4GB", 0.8, 1.5, "gt730", Rarity.COMMON),
    TESLA_V100_16GB("tesla_v100_16gb", "NVIDIA", "NVIDIA Tesla V100", "16GB", 3.2, 0.95, "tesla_v100", Rarity.RARE),
    TESLA_V100_32GB("tesla_v100_32gb", "NVIDIA", "NVIDIA Tesla V100", "32GB", 6.4, 0.95, "tesla_v100", Rarity.RARE),
    RX_580_4GB("rx_580_4gb", "AMD", "AMD Radeon RX 580", "4GB", 0.8, 1.0, "rx580", Rarity.UNCOMMON),
    RX_580_8GB("rx_580_8gb", "AMD", "AMD Radeon RX 580", "8GB", 1.6, 1.0, "rx580", Rarity.UNCOMMON),
    TPU_V6E_32GB("tpu_v6e_32gb", "Google", "Google TPU v6e", "32GB", 6.4, 0.65, "tpu_v6e", Rarity.EPIC),
    A10_24GB("a10_24gb", "NVIDIA", "NVIDIA A10 24GB", "24GB", 4.8, 1.0, "a10", Rarity.RARE),
    A30_24GB("a30_24gb", "NVIDIA", "NVIDIA A30 24GB", "24GB", 4.8, 0.9, "a30", Rarity.RARE),
    A40_48GB("a40_48gb", "NVIDIA", "NVIDIA A40 48GB", "48GB", 9.6, 0.75, "a40", Rarity.RARE),
    TESLA_K80_24GB("tesla_k80_24gb", "NVIDIA", "NVIDIA Tesla K80", "24GB", 4.8, 1.4, "tesla_k80", Rarity.COMMON),
    TESLA_P100_16GB("tesla_p100_16gb", "NVIDIA", "NVIDIA Tesla P100", "16GB", 3.2, 1.05, "tesla_p100", Rarity.UNCOMMON),
    INSTINCT_MI50_16GB("instinct_mi50_16gb", "AMD", "AMD Radeon Instinct MI50", "16GB", 3.2, 1.0, "instinct_mi50", Rarity.UNCOMMON),
    INSTINCT_MI100_32GB("instinct_mi100_32gb", "AMD", "AMD Instinct MI100", "32GB", 6.4, 0.8, "instinct_mi100", Rarity.RARE),
    INSTINCT_MI200_64GB("instinct_mi200_64gb", "AMD", "AMD Instinct MI200", "64GB", 12.8, 0.6, "instinct_mi200", Rarity.EPIC),
    INSTINCT_MI250X_128GB("instinct_mi250x_128gb", "AMD", "AMD Instinct MI250X", "128GB", 25.6, 0.55, "instinct_mi250x", Rarity.EPIC);

    private final String id;
    private final String vendor;
    private final String product;
    private final String memory;
    private final double vramScreens;
    private final double costScale;
    private final String texture;
    private final Rarity rarity;

    TensorGpuSpec(String id, String vendor, String product, String memory, double vramScreens, double costScale, String texture, Rarity rarity) {
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

    public static TensorGpuSpec byId(String id) {
        for (TensorGpuSpec spec : values()) {
            if (spec.id.equals(id)) {
                return spec;
            }
        }
        return null;
    }
}
