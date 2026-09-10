package com.opentensor;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The "OpenTensor" creative tab, iconed with the RTX PRO 6000 Blackwell.
 */
public final class OpenTensorTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, OpenTensor.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> OPENTENSOR =
            TABS.register("opentensor", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.opentensor"))
                    .icon(() -> new ItemStack(ModItems.RTX_PRO_6000_BLACKWELL_96GB.get()))
                    .displayItems((params, output) -> {
                        output.accept(ModItems.A100_40GB.get());
                        output.accept(ModItems.A100_80GB.get());
                        output.accept(ModItems.H100_80GB.get());
                        output.accept(ModItems.H100_96GB.get());
                        output.accept(ModItems.H200_141GB.get());
                        output.accept(ModItems.RTX_PRO_6000_BLACKWELL_96GB.get());
                        output.accept(ModItems.TESLA_P40_24GB.get());
                        output.accept(ModItems.TESLA_T4_16GB.get());
                        output.accept(ModItems.RTX_3090_24GB.get());
                        output.accept(ModItems.GT_730_1GB.get());
                        output.accept(ModItems.GT_730_2GB.get());
                        output.accept(ModItems.GT_730_4GB.get());
                        output.accept(ModItems.TESLA_V100_16GB.get());
                        output.accept(ModItems.TESLA_V100_32GB.get());
                        output.accept(ModItems.RX_580_4GB.get());
                        output.accept(ModItems.RX_580_8GB.get());
                        output.accept(ModItems.TPU_V6E_32GB.get());
                        output.accept(ModItems.GROQ_LPU.get());
                        output.accept(ModItems.DDR5_16GB.get());
                        output.accept(ModItems.DDR5_32GB.get());
                        output.accept(ModItems.DDR5_64GB.get());
                        output.accept(ModItems.DDR5_128GB.get());
                        output.accept(ModItems.EPYC_9354P.get());
                        output.accept(ModItems.EPYC_7303P.get());
                        output.accept(ModItems.SSD_512GB.get());
                        output.accept(ModItems.RTX_5090_32GB.get());
                        output.accept(ModItems.TIER5_SCREEN.get());
                    })
                    .build());

    private OpenTensorTabs() {
    }
}
