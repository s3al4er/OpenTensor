package com.opentensor;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

/**
 * OpenTensor: an OpenComputers (Rebooted) addon adding datacenter-class GPUs,
 * an LPU and DDR5 memory.
 *
 * <p>Installs on top of OpenComputers and only talks to it through the public
 * driver API ({@code li.cil.oc.api.Driver}). No OpenComputers sources are
 * modified; the addon registers its own items, its own environments and its
 * item drivers plus environment providers during common setup.</p>
 */
@Mod(OpenTensor.MODID)
public final class OpenTensor {
    public static final String MODID = "opentensor";

    public OpenTensor(IEventBus modBus) {
        ModItems.ITEMS.register(modBus);
        TensorDataComponents.TYPES.register(modBus);
        OpenTensorTabs.TABS.register(modBus);
        modBus.addListener(this::commonSetup);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // Driver registration must happen at init time or later, never pre-init.
        event.enqueueWork(() -> {
            li.cil.oc.api.Driver.add(TensorDriver.INSTANCE);
            li.cil.oc.api.Driver.add(TensorDriver.PROVIDER);
            li.cil.oc.api.Driver.add(GroqDriver.INSTANCE);
            li.cil.oc.api.Driver.add(GroqDriver.PROVIDER);
            li.cil.oc.api.Driver.add(TensorMemoryDriver.INSTANCE);
            li.cil.oc.api.Driver.add(TensorMemoryDriver.PROVIDER);
            li.cil.oc.api.Driver.add(TensorCpuDriver.INSTANCE);
            li.cil.oc.api.Driver.add(TensorCpuDriver.PROVIDER);
            li.cil.oc.api.Driver.add(TensorSsdDriver.INSTANCE);
            li.cil.oc.api.Driver.add(TensorSsdDriver.PROVIDER);
        });
    }
}
