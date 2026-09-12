package com.opentensor;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Item form of a Tensor DDR5 memory stick.
 */
public final class TensorMemoryItem extends Item {
    private final TensorMemorySpec spec;

    public TensorMemoryItem(TensorMemorySpec spec, Properties properties) {
        super(properties);
        this.spec = spec;
    }

    public TensorMemorySpec spec() {
        return spec;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.opentensor.ram", spec.label()));
        tooltip.add(Component.translatable("tooltip.opentensor.desc." + spec.id()));
        TensorTooltips.appendAddress(stack, tooltip);
    }
}
