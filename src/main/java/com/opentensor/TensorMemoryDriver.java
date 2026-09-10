package com.opentensor;

import li.cil.oc.api.driver.EnvironmentProvider;
import li.cil.oc.api.driver.item.CallBudget;
import li.cil.oc.api.driver.item.Memory;
import li.cil.oc.api.driver.item.Slot;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Item driver exposing the four Tensor DDR5 sticks as computer memory.
 *
 * <p>Reports driver tier 2 (installs into Tier 3 and better memory slots)
 * and a 1.5 call budget, mirroring high-end stock RAM. Registered (together
 * with {@link Provider}) via the public {@code li.cil.oc.api.Driver} API
 * only.</p>
 */
public final class TensorMemoryDriver implements li.cil.oc.api.driver.DriverItem, Memory, CallBudget {
    public static final TensorMemoryDriver INSTANCE = new TensorMemoryDriver();
    public static final Provider PROVIDER = new Provider();

    /** Driver tier: fits Tier 3+ memory slots (slots accept tier &lt;= theirs). */
    public static final int TIER = 2;

    /** Call budget granted by every stick (matches stock top RAM class). */
    public static final double CALL_BUDGET = 1.5;

    private TensorMemoryDriver() {
    }

    private static TensorMemorySpec specOf(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        var key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!OpenTensor.MODID.equals(key.getNamespace())) {
            return null;
        }
        return TensorMemorySpec.byId(key.getPath());
    }

    @Override
    public boolean worksWith(ItemStack stack) {
        return specOf(stack) != null;
    }

    @Override
    public double amount(ItemStack stack) {
        TensorMemorySpec spec = specOf(stack);
        return spec != null ? spec.kilobytes() : 0.0;
    }

    @Override
    public ManagedEnvironment createEnvironment(ItemStack stack, EnvironmentHost host) {
        Level level = host != null ? host.getEnvironmentLevel() : null;
        // Never create server-side environments on the client.
        if (level != null && level.isClientSide()) {
            return null;
        }
        TensorMemorySpec spec = specOf(stack);
        if (spec == null) {
            return null;
        }
        return new TensorMemoryEnvironment(spec);
    }

    @Override
    public String slot(ItemStack stack) {
        return Slot.Memory;
    }

    @Override
    public int tier(ItemStack stack) {
        return TIER;
    }

    @Override
    public double getCallBudget(ItemStack stack) {
        return CALL_BUDGET;
    }

    /**
     * Maps memory stacks to their environment class (used for docs/lookups).
     */
    public static final class Provider implements EnvironmentProvider {
        private Provider() {
        }

        @Override
        public Class<?> getEnvironment(ItemStack stack) {
            return INSTANCE.worksWith(stack) ? TensorMemoryEnvironment.class : null;
        }
    }
}
