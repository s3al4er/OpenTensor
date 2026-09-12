package com.opentensor;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Item form of the Noctua NF-A14 industrialPPC-3000 PWM fan.
 */
public final class NoctuaFanItem extends Item {
    public NoctuaFanItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.opentensor.fan",
                NoctuaFanEnvironment.MAX_RPM));
        tooltip.add(Component.translatable("tooltip.opentensor.desc." + NoctuaFanEnvironment.ITEM_ID));
        TensorTooltips.appendAddress(stack, tooltip);
    }
}
