package com.opentensor;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Tensor block entity types.
 */
public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, OpenTensor.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TensorScreenBlockEntity>> TIER5_SCREEN =
            TYPES.register("tier5_screen", () -> BlockEntityType.Builder
                    .of(TensorScreenBlockEntity::new, ModBlocks.TIER5_SCREEN.get())
                    .build(null));

    private ModBlockEntities() {
    }
}
