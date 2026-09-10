package com.opentensor;

import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Tensor blocks.
 */
public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(OpenTensor.MODID);

    public static final DeferredBlock<TensorScreenBlock> TIER5_SCREEN = BLOCKS.register("tier5_screen",
            () -> new TensorScreenBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2, 5)));

    private ModBlocks() {
    }
}
