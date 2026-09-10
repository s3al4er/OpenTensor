package com.opentensor;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.core.component.DataComponentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Data components used to persist Tensor GPU state on the card item stacks:
 * bound screen address, active buffer index and off-screen VRAM pages.
 */
public final class TensorDataComponents {
    public static final DeferredRegister<DataComponentType<?>> TYPES =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, OpenTensor.MODID);

    /** Screen address + active buffer index. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CustomData>> GPU_STATE =
            TYPES.register("tensor_gpu_state",
                    () -> DataComponentType.<CustomData>builder().persistent(CustomData.CODEC).build());

    /** Serialized off-screen VRAM buffers. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CustomData>> GPU_VRAM =
            TYPES.register("tensor_gpu_vram",
                    () -> DataComponentType.<CustomData>builder().persistent(CustomData.CODEC).build());

    private TensorDataComponents() {
    }
}
