package com.opentensor;

import li.cil.oc.api.Machine;
import li.cil.oc.api.driver.EnvironmentProvider;
import li.cil.oc.api.driver.item.CallBudget;
import li.cil.oc.api.driver.item.HostAware;
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
 * Item driver exposing the Groq LPU to computers.
 *
 * <p>The LPU plugs into the CPU slot (like the stock APU) and provides both
 * sides at 1.5x the strength of an OpenComputers APU Tier 2:</p>
 * <ul>
 *   <li>Processor side: 24 supported components (16 x 1.5) and a 2.25 call
 *   budget (1.5 x 1.5), with the standard Lua architecture.</li>
 *   <li>Graphics side: a {@link GroqLpuEnvironment} ({@code gpu} component,
 *   80x25, 4-bit, 6000 VRAM cells, costs / 1.5).</li>
 * </ul>
 *
 * <p>Registered (together with {@link Provider}) via the public
 * {@code li.cil.oc.api.Driver} API only.</p>
 */
public final class GroqDriver implements li.cil.oc.api.driver.DriverItem, MutableProcessor, CallBudget, HostAware {
    public static final GroqDriver INSTANCE = new GroqDriver();
    public static final Provider PROVIDER = new Provider();

    /** Registry path of the Groq LPU item. */
    public static final String ITEM_ID = "groq_lpu";

    /** NBT keys for the selected architecture (mirrors OC's CPU driver). */
    private static final String ARCH_CLASS_KEY = "oc:archClass";
    private static final String ARCH_NAME_KEY = "oc:archName";

    private GroqDriver() {
    }

    private static boolean isGroqLpu(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        var key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return OpenTensor.MODID.equals(key.getNamespace()) && ITEM_ID.equals(key.getPath());
    }

    @Override
    public boolean worksWith(ItemStack stack) {
        return isGroqLpu(stack);
    }

    @Override
    public boolean worksWith(ItemStack stack, Class<? extends EnvironmentHost> host) {
        return isGroqLpu(stack);
    }

    @Override
    public ManagedEnvironment createEnvironment(ItemStack stack, EnvironmentHost host) {
        Level level = host != null ? host.getEnvironmentLevel() : null;
        // Never create server-side environments on the client.
        if (level != null && level.isClientSide()) {
            return null;
        }
        if (!isGroqLpu(stack)) {
            return null;
        }
        return new GroqLpuEnvironment();
    }

    @Override
    public String slot(ItemStack stack) {
        // Like the stock APU: the LPU is a processor with builtin graphics.
        return Slot.CPU;
    }

    @Override
    public int tier(ItemStack stack) {
        // Same install tier as a stock APU Tier 2 (cpuTier 2).
        return 2;
    }

    @Override
    public int supportedComponents(ItemStack stack) {
        return GroqLpuEnvironment.CPU_COMPONENTS;
    }

    @Override
    public double getCallBudget(ItemStack stack) {
        return GroqLpuEnvironment.CPU_CALL_BUDGET;
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
        if (!isGroqLpu(stack)) {
            throw new IllegalArgumentException("Unsupported processor type.");
        }
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putString(ARCH_CLASS_KEY, architecture.getName());
            tag.putString(ARCH_NAME_KEY, Machine.getArchitectureName(architecture));
        });
    }

    /**
     * Maps LPU stacks to their environment class (used for docs/lookups).
     */
    public static final class Provider implements EnvironmentProvider {
        private Provider() {
        }

        @Override
        public Class<?> getEnvironment(ItemStack stack) {
            return INSTANCE.worksWith(stack) ? GroqLpuEnvironment.class : null;
        }
    }
}
