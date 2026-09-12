package com.opentensor;

import li.cil.oc.api.internal.TextBuffer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
/**
 * Tier 5 screen block entity: a stock screen pushed beyond the stock tier
 * table (380x120 at 16-bit color).
 *
 * <p>Implemented as a subclass of the stock screen, so rendering
 * ({@code ScreenRenderer}), the baked model ({@code ScreenModel}), the
 * right-click GUI, touch/keyboard input, redstone and analyzer support all
 * keep working. The mechanics tier stays 3 (a valid stock index); only the
 * buffer maximums are raised to Tier 5 values and re-applied after every
 * load path, because the stock code re-applies tier limits from its own
 * table on load.</p>
 *
 * <p>Note: with mechanics tier 3 these screens can multiblock-merge with
 * stock Tier 4 screens. The merge is harmless (same code path), just keep
 * colors apart if you do not want mixed walls.</p>
 */
public class TensorScreenBlockEntity extends li.cil.oc.common.blockentity.Screen {
    /** Maximum resolution of the Tier 5 screen (twice OC Tier 4 each axis). */
    public static final int MAX_WIDTH = 380;    /** Maximum resolution of the Tier 5 screen (twice OC Tier 4 each axis). */
    public static final int MAX_HEIGHT = 120;
    /** Maximum color depth of the Tier 5 screen (matches OC Tier 4). */
    public static final TextBuffer.ColorDepth MAX_DEPTH = TextBuffer.ColorDepth.SixteenBit;
    /** Color depth in bits, for tooltips. */
    public static final int MAX_DEPTH_BITS = 16;

    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();

    private boolean maxApplied = false;

    public TensorScreenBlockEntity(BlockPos pos, BlockState state) {
        super(pos, state, 3);
    }

    @Override
    public BlockEntityType<?> getType() {
        // Must report our own type: vanilla persists the BE id from this and
        // reloads through our factory. The stock id would resurrect a plain
        // Tier 1 screen after a world reload.
        return ModBlockEntities.TIER5_SCREEN.get();
    }

    @Override
    public void loadComponentsCommon(net.minecraft.core.component.DataComponentHolder holder) {
        // Stock resets to the tier color here and then applies the saved
        // color on top. Hook in after it: anything still wearing the tier
        // default (fresh placements AND pre-existing gray screens) becomes
        // OC blue; genuinely dyed screens keep their color. Quirk: a screen
        // dyed exactly the tier-3 gray flips back to blue on reload.
        super.loadComponentsCommon(holder);
        if (getColor() == li.cil.oc.util.Color$.MODULE$.byTier(3)) {
            setColor(0x6666FF); // li.cil.oc.util.Color.rgbValues(DyeColor.BLUE)
        }
    }

    @Override
    public void updateEntity() {
        // Run stock logic first (it re-applies stock tier limits while
        // restoring deferred buffer data), then re-assert ours on top.
        super.updateEntity();
        if (!maxApplied) {
            maxApplied = true;
            buffer().setMaximumResolution(MAX_WIDTH, MAX_HEIGHT);
            buffer().setMaximumColorDepth(MAX_DEPTH);
            LOGGER.info("[OpenTensor] Tier 5 screen initialized at {}: max {}x{}, node {}",
                    getBlockPos(), MAX_WIDTH, MAX_HEIGHT,
                    node() != null ? node().address() : "null");
        }
    }
}
