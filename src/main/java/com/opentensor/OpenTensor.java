package com.opentensor;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/**
 * OpenTensor: an OpenComputers (Rebooted) addon adding datacenter-class GPUs,
 * an LPU, DDR5 memory, server CPUs, an SSD and a Tier 5 screen.
 *
 * <p>Installs on top of OpenComputers and only talks to it through the public
 * driver API ({@code li.cil.oc.api.Driver}). No OpenComputers sources are
 * modified; the addon registers its own items, blocks, environments and its
 * item drivers plus environment providers during common setup.</p>
 */
@Mod(OpenTensor.MODID)
public final class OpenTensor {
    public static final String MODID = "opentensor";

    public OpenTensor(IEventBus modBus) {
        ModItems.ITEMS.register(modBus);
        ModBlocks.BLOCKS.register(modBus);
        ModBlockEntities.TYPES.register(modBus);
        TensorDataComponents.TYPES.register(modBus);
        OpenTensorTabs.TABS.register(modBus);
        modBus.addListener(this::commonSetup);
        // NeoForge 1.21 no longer discovers capabilities implemented directly
        // by block entities: without these providers the network cannot see
        // the Tier 5 screen's node at all (no join, no bind, no sync).
        // Runs on both sides, like OC's own registration.
        modBus.addListener(this::registerCapabilities);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            OpenTensorClient.register(modBus);
        }
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        var type = ModBlockEntities.TIER5_SCREEN.get();
        var caps = li.cil.oc.common.Capabilities$.MODULE$;
        event.registerBlockEntity(caps.EnvironmentCapability(), type,
                (be, side) -> be instanceof li.cil.oc.api.network.Environment environment ? environment : null);
        event.registerBlockEntity(caps.SidedEnvironmentCapability(), type,
                (be, side) -> be instanceof li.cil.oc.api.network.SidedEnvironment environment ? environment : null);
        event.registerBlockEntity(caps.ColoredCapability(), type,
                (be, side) -> be instanceof li.cil.oc.api.internal.Colored colored ? colored : null);
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
            li.cil.oc.api.Driver.add(TensorTier5Driver.INSTANCE);
            li.cil.oc.api.Driver.add(TensorTier5Driver.PROVIDER);
        });
    }
}
