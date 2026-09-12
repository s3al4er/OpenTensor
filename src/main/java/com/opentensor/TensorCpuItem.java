package com.opentensor;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Item form of a Tensor server CPU.
 */
public final class TensorCpuItem extends Item {
    private final TensorCpuSpec spec;

    public TensorCpuItem(TensorCpuSpec spec, Properties properties) {
        super(properties);
        this.spec = spec;
    }

    public TensorCpuSpec spec() {
        return spec;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.opentensor.cpu", spec.components(), spec.callBudget()));
        tooltip.add(Component.translatable("tooltip.opentensor.desc." + spec.id()));
        TensorTooltips.appendAddress(stack, tooltip);
    }
}
