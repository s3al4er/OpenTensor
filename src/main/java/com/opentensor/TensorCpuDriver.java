package com.opentensor;

import li.cil.oc.api.Machine;
import li.cil.oc.api.driver.EnvironmentProvider;
import li.cil.oc.api.driver.item.CallBudget;
import li.cil.oc.api.driver.item.MutableProcessor;
import li.cil.oc.api.driver.item.Slot;
import li.cil.oc.api.machine.Architecture;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import java.util.Collection;

/**
 * Item driver exposing the Tensor server CPUs to computers.
 *
 * <p>The CPUs plug into the CPU slot (like stock CPUs) and grant their
 * component support and call budget. Registered (together with
 * {@link Provider}) via the public {@code li.cil.oc.api.Driver} API
 * only.</p>
 */
public final class TensorCpuDriver implements li.cil.oc.api.driver.DriverItem, MutableProcessor, CallBudget {
    public static final TensorCpuDriver INSTANCE = new TensorCpuDriver();
    public static final Provider PROVIDER = new Provider();

    /** Driver tier: fits Tier 3+ CPU slots (slots accept tier &lt;= theirs). */
    public static final int TIER = 2;

    /** NBT keys for the selected architecture (mirrors OC's CPU driver). */
    private static final String ARCH_CLASS_KEY = "oc:archClass";
    private static final String ARCH_NAME_KEY = "oc:archName";

    private TensorCpuDriver() {
    }

    private static TensorCpuSpec specOf(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        var key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!OpenTensor.MODID.equals(key.getNamespace())) {
            return null;
        }
        return TensorCpuSpec.byId(key.getPath());
    }

    @Override
    public boolean worksWith(ItemStack stack) {
        return specOf(stack) != null;
    }

    @Override
    public ManagedEnvironment createEnvironment(ItemStack stack, EnvironmentHost host) {
        Level level = host != null ? host.getEnvironmentLevel() : null;
        // Never create server-side environments on the client.
        if (level != null && level.isClientSide()) {
            return null;
        }
        TensorCpuSpec spec = specOf(stack);
        if (spec == null) {
            return null;
        }
        return new TensorCpuEnvironment(spec);
    }

    @Override
    public String slot(ItemStack stack) {
        return Slot.CPU;
    }

    @Override
    public int tier(ItemStack stack) {
        return TIER;
    }

    @Override
    public int supportedComponents(ItemStack stack) {
        TensorCpuSpec spec = specOf(stack);
        return spec != null ? spec.components() : 0;
    }

    @Override
    public double getCallBudget(ItemStack stack) {
        TensorCpuSpec spec = specOf(stack);
        return spec != null ? spec.callBudget() : 1.0;
    }

    @Override
    public Class<? extends Architecture> architecture(ItemStack stack) {
        CustomData custom = stack.get(DataComponents.CUSTOM_DATA);
        if (custom != null) {
            String clazz = custom.copyTag().getString(ARCH_CLASS_KEY);
            if (clazz != null && !clazz.isEmpty()) {
                try {
                    return Class.forName(clazz).asSubclass(Architecture.class);
                } catch (ReflectiveOperationException | ClassCastException ignored) {
                    // Fall through to the default architecture below.
                }
            }
        }
        Collection<Class<? extends Architecture>> architectures = Machine.architectures();
        if (architectures == null || architectures.isEmpty()) {
            return null;
        }
        return architectures.iterator().next();
    }

    @Override
    public Collection<Class<? extends Architecture>> allArchitectures() {
        return Machine.architectures();
    }

    @Override
    public void setArchitecture(ItemStack stack, Class<? extends Architecture> architecture) {
        if (specOf(stack) == null) {
            throw new IllegalArgumentException("Unsupported processor type.");
        }
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putString(ARCH_CLASS_KEY, architecture.getName());
            tag.putString(ARCH_NAME_KEY, Machine.getArchitectureName(architecture));
        });
    }

    /**
     * Maps CPU stacks to their environment class (used for docs/lookups).
     */
    public static final class Provider implements EnvironmentProvider {
        private Provider() {
        }

        @Override
        public Class<?> getEnvironment(ItemStack stack) {
            return INSTANCE.worksWith(stack) ? TensorCpuEnvironment.class : null;
        }
    }
}
