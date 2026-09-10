package com.opentensor;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Item form of the Groq LPU (processor with builtin graphics).
 */
public final class GroqLpuItem extends Item {
    public GroqLpuItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.opentensor.lpu",
                GroqLpuEnvironment.MAX_WIDTH, GroqLpuEnvironment.MAX_HEIGHT,
                GroqLpuEnvironment.MAX_DEPTH_BITS));
        tooltip.add(Component.translatable("tooltip.opentensor.desc.groq_lpu"));
    }
}
