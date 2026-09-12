package com.opentensor;

import li.cil.oc.api.driver.EnvironmentProvider;
import li.cil.oc.api.driver.item.Slot;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Item driver exposing the twenty-seven Tensor GPU cards to computers as {@code gpu}
 * components. Registered (together with {@link Provider}) via the public
 * {@code li.cil.oc.api.Driver} API only.
 */
public final class TensorDriver implements li.cil.oc.api.driver.DriverItem {
    public static final TensorDriver INSTANCE = new TensorDriver();
    public static final Provider PROVIDER = new Provider();

    private TensorDriver() {
    }

    @Override
    public boolean worksWith(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (!OpenTensor.MODID.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace())) {
            return false;
        }
        return TensorGpuSpec.byId(BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath()) != null;
    }

    @Override
    public ManagedEnvironment createEnvironment(ItemStack stack, EnvironmentHost host) {
        Level level = host != null ? host.getEnvironmentLevel() : null;
        // Never create server-side environments on the client.
        if (level != null && level.isClientSide()) {
            return null;
        }
        if (stack.isEmpty()) {
            return null;
        }
        TensorGpuSpec spec = TensorGpuSpec.byId(BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath());
        if (spec == null) {
            return null;
        }
        return new TensorGpuEnvironment(spec, host);
    }

    @Override
    public String slot(ItemStack stack) {
        return Slot.Card;
    }

    @Override
    public int tier(ItemStack stack) {
        // Tensor cards plug into Tier 3 card slots.
        return 3;
    }

    /**
     * Maps card stacks to their environment class (used for docs/lookups).
     */
    public static final class Provider implements EnvironmentProvider {
        private Provider() {
        }

        @Override
        public Class<?> getEnvironment(ItemStack stack) {
            return INSTANCE.worksWith(stack) ? TensorGpuEnvironment.class : null;
        }
    }
}
