package com.opentensor;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Item form of a Tensor SSD.
 */
public final class TensorSsdItem extends Item {
    private final TensorSsdSpec spec;

    public TensorSsdItem(TensorSsdSpec spec, Properties properties) {
        super(properties);
        this.spec = spec;
    }

    public TensorSsdSpec spec() {
        return spec;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.opentensor.desc." + spec.id()));
    }
}
