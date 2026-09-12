package com.opentensor;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Shared tooltip bits mirroring stock OpenComputers item tooltips.
 */
public final class TensorTooltips {
    private TensorTooltips() {
    }

    /**
     * Appends the shortened component address, exactly like stock
     * {@code SimpleItem.tooltipCosts} (gray, 13 chars + "...").
     * Shows nothing when the stack carries no address yet (e.g. fresh
     * creative items or RAM, whose address is never persisted).
     */
    public static void appendAddress(ItemStack stack, List<Component> tooltip) {
        String address = stack.get(li.cil.oc.common.datacomponents.OCComponents$.MODULE$.ADDRESS().get());
        if (address != null && !address.isEmpty()) {
            String shortened = address.length() > 13 ? address.substring(0, 13) + "..." : address;
            tooltip.add(Component.literal("§8" + shortened + "§7"));
        }
    }
}
