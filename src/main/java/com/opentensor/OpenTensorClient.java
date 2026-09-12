package com.opentensor;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * Client-only setup (never loaded on a dedicated server).
 *
 * <p>Reuses the stock screen rendering wholesale: the baked block model is
 * swapped for OC's {@code ScreenModel} and our block entity type is bound
 * to OC's {@code ScreenRenderer} (legal because our entity extends the
 * stock screen class).</p>
 */
public final class OpenTensorClient {
    private OpenTensorClient() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(OpenTensorClient::onModifyBakingResult);
        modBus.addListener(OpenTensorClient::onRegisterRenderers);
        modBus.addListener(OpenTensorClient::onRegisterItemColors);
    }

    private static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        var models = event.getModels();
        var targets = new java.util.ArrayList<net.minecraft.client.resources.model.ModelResourceLocation>();
        for (var location : models.keySet()) {
            if (location instanceof net.minecraft.client.resources.model.ModelResourceLocation modelLocation
                    && modelLocation.toString().matches("^opentensor:tier5_screen#pitch=.*")) {
                targets.add(modelLocation);
            }
        }
        for (var location : targets) {
            models.put(location, li.cil.oc.client.renderer.block.ScreenModel$.MODULE$);
        }
    }

    private static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.TIER5_SCREEN.get(),
                context -> new li.cil.oc.client.renderer.tileentity.ScreenRenderer());
    }

    /**
     * Tints the Tier 5 screen item icon OC blue (the item model carries
     * {@code tintindex 0}; placed blocks are colored by the renderer from
     * the block entity instead and are unaffected).
     */
    private static void onRegisterItemColors(
            net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) -> 0x6666FF, ModItems.TIER5_SCREEN.get());
    }
}
