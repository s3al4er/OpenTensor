package com.opentensor;

import net.minecraft.world.level.block.Block;

/**
 * Block item for the Tier 5 screen.
 *
 * <p>Extends the stock block item (placement rotation handling) with a
 * fixed description id pointing at our own lang entry.</p>
 */
public class TensorScreenBlockItem extends li.cil.oc.common.block.Item {
    public TensorScreenBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public String getDescriptionId() {
        return "block.opentensor.tier5_screen";
    }
}
