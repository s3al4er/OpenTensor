package com.opentensor;

import li.cil.oc.api.driver.EnvironmentProvider;
import li.cil.oc.api.driver.item.Slot;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Item driver exposing Tier 5 Tensor GPU cards to computers as {@code gpu}
 * components. Registered (together with {@link Provider}) via the public
 * {@code li.cil.oc.api.Driver} API only.
 */
public final class TensorTier5Driver implements li.cil.oc.api.driver.DriverItem {
    public static final TensorTier5Driver INSTANCE = new TensorTier5Driver();
    public static final Provider PROVIDER = new Provider();

    private TensorTier5Driver() {
    }

    @Override
    public boolean worksWith(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (!OpenTensor.MODID.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace())) {
            return false;
        }
        return TensorTier5GpuSpec.byId(BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath()) != null;
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
        TensorTier5GpuSpec spec = TensorTier5GpuSpec.byId(BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath());
        if (spec == null) {
            return null;
        }
        return new TensorTier5GpuEnvironment(spec, host);
    }

    @Override
    public String slot(ItemStack stack) {
        return Slot.Card;
    }

    @Override
    public int tier(ItemStack stack) {
        // Like the other Tensor cards: Tier-4-class card slots (creative /
        // high-end servers). See README for the slot-compat note.
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
            return INSTANCE.worksWith(stack) ? TensorTier5GpuEnvironment.class : null;
        }
    }
}
