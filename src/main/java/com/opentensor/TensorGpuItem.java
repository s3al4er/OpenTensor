package com.opentensor;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Item form of a Tensor GPU card.
 */
public final class TensorGpuItem extends Item {
    private final TensorGpuSpec spec;

    public TensorGpuItem(TensorGpuSpec spec, Properties properties) {
        super(properties);
        this.spec = spec;
    }

    public TensorGpuSpec spec() {
        return spec;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.opentensor.gpu",
                TensorGpuEnvironment.MAX_WIDTH, TensorGpuEnvironment.MAX_HEIGHT,
                TensorGpuEnvironment.MAX_DEPTH_BITS, spec.memory()));
        tooltip.add(Component.translatable("tooltip.opentensor.desc." + spec.id()));
        TensorTooltips.appendAddress(stack, tooltip);
    }
}
