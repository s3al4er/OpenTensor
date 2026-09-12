package com.opentensor;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Item form of a Tier 5 Tensor GPU card.
 */
public final class TensorTier5GpuItem extends Item {
    private final TensorTier5GpuSpec spec;

    public TensorTier5GpuItem(TensorTier5GpuSpec spec, Properties properties) {
        super(properties);
        this.spec = spec;
    }

    public TensorTier5GpuSpec spec() {
        return spec;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.opentensor.gpu",
                TensorTier5GpuEnvironment.MAX_WIDTH, TensorTier5GpuEnvironment.MAX_HEIGHT,
                TensorTier5GpuEnvironment.MAX_DEPTH_BITS, spec.memory()));
        tooltip.add(Component.translatable("tooltip.opentensor.desc." + spec.id()));
        TensorTooltips.appendAddress(stack, tooltip);
    }
}
