package com.opentensor;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.List;

/**
 * Tier 5 screen block (380x120, 16-bit).
 */
public class TensorScreenBlock extends li.cil.oc.common.block.Screen {
    public TensorScreenBlock(BlockBehaviour.Properties properties) {
        super(properties, 3);
    }

    @Override
    public li.cil.oc.common.blockentity.Screen newBlockEntity(BlockPos pos, BlockState state) {
        return new TensorScreenBlockEntity(pos, state);
    }

    @Override
    public BlockEntityType<?> getBlockEntityType() {
        return ModBlockEntities.TIER5_SCREEN.get();
    }

    @Override
    public void tooltipBody(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.opentensor.screen",
                TensorScreenBlockEntity.MAX_WIDTH, TensorScreenBlockEntity.MAX_HEIGHT,
                TensorScreenBlockEntity.MAX_DEPTH_BITS));
        tooltip.add(Component.translatable("tooltip.opentensor.desc.screen_tier5"));
    }
}
