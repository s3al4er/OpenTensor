package com.opentensor;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The seventeen Tensor GPU items, plus memory and CPUs.
 */
public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(OpenTensor.MODID);

    public static final DeferredItem<TensorGpuItem> A100_40GB = register(TensorGpuSpec.A100_40GB);
    public static final DeferredItem<TensorGpuItem> A100_80GB = register(TensorGpuSpec.A100_80GB);
    public static final DeferredItem<TensorGpuItem> H100_80GB = register(TensorGpuSpec.H100_80GB);
    public static final DeferredItem<TensorGpuItem> H100_96GB = register(TensorGpuSpec.H100_96GB);
    public static final DeferredItem<TensorGpuItem> H200_141GB = register(TensorGpuSpec.H200_141GB);
    public static final DeferredItem<TensorGpuItem> RTX_PRO_6000_BLACKWELL_96GB = register(TensorGpuSpec.RTX_PRO_6000_BLACKWELL_96GB);
    public static final DeferredItem<TensorGpuItem> TESLA_P40_24GB = register(TensorGpuSpec.TESLA_P40_24GB);
    public static final DeferredItem<TensorGpuItem> TESLA_T4_16GB = register(TensorGpuSpec.TESLA_T4_16GB);
    public static final DeferredItem<TensorGpuItem> RTX_3090_24GB = register(TensorGpuSpec.RTX_3090_24GB);
    public static final DeferredItem<TensorGpuItem> GT_730_1GB = register(TensorGpuSpec.GT_730_1GB);
    public static final DeferredItem<TensorGpuItem> GT_730_2GB = register(TensorGpuSpec.GT_730_2GB);
    public static final DeferredItem<TensorGpuItem> GT_730_4GB = register(TensorGpuSpec.GT_730_4GB);
    public static final DeferredItem<TensorGpuItem> TESLA_V100_16GB = register(TensorGpuSpec.TESLA_V100_16GB);
    public static final DeferredItem<TensorGpuItem> TESLA_V100_32GB = register(TensorGpuSpec.TESLA_V100_32GB);
    public static final DeferredItem<TensorGpuItem> RX_580_4GB = register(TensorGpuSpec.RX_580_4GB);
    public static final DeferredItem<TensorGpuItem> RX_580_8GB = register(TensorGpuSpec.RX_580_8GB);
    public static final DeferredItem<TensorGpuItem> TPU_V6E_32GB = register(TensorGpuSpec.TPU_V6E_32GB);

    public static final DeferredItem<TensorMemoryItem> DDR5_16GB = registerMemory(TensorMemorySpec.DDR5_16GB);
    public static final DeferredItem<TensorMemoryItem> DDR5_32GB = registerMemory(TensorMemorySpec.DDR5_32GB);
    public static final DeferredItem<TensorMemoryItem> DDR5_64GB = registerMemory(TensorMemorySpec.DDR5_64GB);
    public static final DeferredItem<TensorMemoryItem> DDR5_128GB = registerMemory(TensorMemorySpec.DDR5_128GB);

    public static final DeferredItem<TensorCpuItem> EPYC_9354P = registerCpu(TensorCpuSpec.EPYC_9354P);
    public static final DeferredItem<TensorCpuItem> EPYC_7303P = registerCpu(TensorCpuSpec.EPYC_7303P);

    public static final DeferredItem<TensorSsdItem> SSD_512GB =
            ITEMS.register(TensorSsdSpec.SSD_512GB.id(),
                    () -> new TensorSsdItem(TensorSsdSpec.SSD_512GB,
                            new Item.Properties().rarity(TensorSsdSpec.SSD_512GB.rarity())));

    public static final DeferredItem<GroqLpuItem> GROQ_LPU =
            ITEMS.register(GroqDriver.ITEM_ID, () -> new GroqLpuItem(new Item.Properties().rarity(Rarity.EPIC)));

    private ModItems() {
    }

    private static DeferredItem<TensorGpuItem> register(TensorGpuSpec spec) {
        return ITEMS.register(spec.id(), () -> new TensorGpuItem(spec, new Item.Properties().rarity(spec.rarity())));
    }

    private static DeferredItem<TensorMemoryItem> registerMemory(TensorMemorySpec spec) {
        return ITEMS.register(spec.id(), () -> new TensorMemoryItem(spec, new Item.Properties().rarity(spec.rarity())));
    }

    private static DeferredItem<TensorCpuItem> registerCpu(TensorCpuSpec spec) {
        return ITEMS.register(spec.id(), () -> new TensorCpuItem(spec, new Item.Properties().rarity(spec.rarity())));
    }
}
