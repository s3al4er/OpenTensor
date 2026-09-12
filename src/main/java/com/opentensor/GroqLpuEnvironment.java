package com.opentensor;

import li.cil.oc.api.Network;
import li.cil.oc.api.UnrecoverablePersistanceException;
import li.cil.oc.api.datacomponents.MutableNbtComponentHolder;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.internal.TextBuffer;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.machine.LimitReachedException;
import li.cil.oc.api.network.ComponentConnector;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.common.component.GpuTextBuffer;
import li.cil.oc.common.component.GpuTextBuffer$;
import li.cil.oc.common.component.traits.VideoRamRasterizer;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.common.MutableDataComponentHolder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Groq LPU component environment, exposed to computers as {@code gpu}.
 *
 * <p>Mirrors the stock OpenComputers APU Tier 2 graphics side (80x25
 * resolution, 4-bit color, Tier-2-class call budget) scaled up 1.5x, i.e. an
 * "APU Tier 2.5": VRAM is 6000 cells (4000 x 1.5) and every budget/energy
 * cost is divided by 1.5. The processor side (CPU slot, 24 components, 2.25
 * call budget) is provided by {@link GroqDriver}.</p>
 *
 * <p>Persistence reuses the same {@link TensorDataComponents} item components
 * as the Tensor GPUs.</p>
 */
public final class GroqLpuEnvironment extends AbstractManagedEnvironment implements DeviceInfo {
    /** Maximum resolution of the Groq LPU (matches OC APU Tier 2 GPU). */
    public static final int MAX_WIDTH = 80;
    /** Maximum resolution of the Groq LPU (matches OC APU Tier 2 GPU). */
    public static final int MAX_HEIGHT = 25;
    /** Resolution cell count, also used as the VRAM accounting unit. */
    public static final int MAX_CELLS = MAX_WIDTH * MAX_HEIGHT; // 2000
    /** Maximum color depth of the Groq LPU (matches OC APU Tier 2 GPU). */
    public static final TextBuffer.ColorDepth MAX_DEPTH = TextBuffer.ColorDepth.FourBit;
    /** Color depth in bits, for tooltips. */
    public static final int MAX_DEPTH_BITS = 4;

    /**
     * Total VRAM in cells: APU Tier 2 stock is 80*25*2 = 4000, times 1.5.
     */
    public static final double TOTAL_VRAM = 6000.0;

    /** CPU-side call budget: APU Tier 2 stock is 1.5, times 1.5. */
    public static final double CPU_CALL_BUDGET = 2.25;

    /** CPU-side supported components: APU Tier 2 stock is 16, times 1.5. */
    public static final int CPU_COMPONENTS = 24;

    // APU-Tier-2-class call budget costs (OC stock Tier 2 values), each
    // divided by 1.5 for the "Tier 2.5" boost.
    private static final double COST_SET_BACKGROUND = (1.0 / 64) / 1.5;
    private static final double COST_SET_FOREGROUND = (1.0 / 64) / 1.5;
    private static final double COST_SET_PALETTE_COLOR = (1.0 / 8) / 1.5;
    private static final double COST_SET = (1.0 / 128) / 1.5;
    private static final double COST_COPY = (1.0 / 32) / 1.5;
    private static final double COST_FILL = (1.0 / 64) / 1.5;
    // OC default bitbltCost (0.5) * 2^1 for Tier-2-class throughput, / 1.5.
    private static final double BITBLT_BASE = 1.0 / 1.5;

    // OC stock energy costs (power.cost.gpuSet/Fill/Clear/Copy), normalized
    // per cell exactly like OC does (divided by basicScreenPixels = 50x16
    // = 800) and divided by 1.5 for the "Tier 2.5" boost. Raw config values
    // must NOT be used directly, see TensorGpuEnvironment.
    private static final double ENERGY_SET = (2.0 / 800) / 1.5;
    private static final double ENERGY_FILL = (1.0 / 800) / 1.5;
    private static final double ENERGY_CLEAR = (0.1 / 800) / 1.5;
    private static final double ENERGY_COPY = (0.25 / 800) / 1.5;

    private static final int RESERVED_SCREEN_INDEX = 0;

    private static final String TAG_SCREEN = "screen";
    private static final String TAG_BUFFER = "buffer";
    private static final String TAG_PAGES = "pages";
    private static final String TAG_PAGE_ID = "id";
    private static final String TAG_PAGE_DATA = "data";

    private final double totalVRAM = TOTAL_VRAM;
    private final ComponentConnector node;

    private String screenAddress;
    private TextBuffer screenInstance;
    private int bufferIndex = RESERVED_SCREEN_INDEX;
    private final Map<Integer, GpuTextBuffer> buffers = new HashMap<>();
    private boolean budgetExhausted;

    public GroqLpuEnvironment() {
        this.node = Network.newNode(this, Visibility.Neighbors)
                .withComponent("gpu")
                .withConnector()
                .create();
    }

    @Override
    public Node node() {
        return node;
    }

    // ------------------------------------------------------------------ //
    // Helpers.
    // ------------------------------------------------------------------ //

    @FunctionalInterface
    private interface BufferCall {
        Object[] call(TextBuffer buffer) throws Exception;
    }

    private static int codePointLength(String value) {
        return value.codePointCount(0, value.length());
    }

    private Object[] onBuffer(int index, BufferCall call) throws Exception {
        TextBuffer buffer = index == RESERVED_SCREEN_INDEX ? screenInstance : buffers.get(index);
        if (buffer == null) {
            return new Object[]{null, index == RESERVED_SCREEN_INDEX ? "no screen" : "invalid buffer index"};
        }
        synchronized (buffer) {
            return call.call(buffer);
        }
    }

    private Object[] onActiveBuffer(BufferCall call) throws Exception {
        return onBuffer(bufferIndex, call);
    }

    private int usedMemory() {
        int sum = 0;
        for (GpuTextBuffer page : buffers.values()) {
            sum += page.data().width() * page.data().height();
        }
        return sum;
    }

    private int nextBufferIndex() {
        int index = RESERVED_SCREEN_INDEX + 1;
        while (buffers.containsKey(index)) {
            index++;
        }
        return index;
    }

    private int removeBuffers(int[] ids) {
        int count = 0;
        for (int id : ids) {
            if (buffers.remove(id) != null) {
                onBufferRamDestroy(id);
                count++;
            }
        }
        return count;
    }

    private int removeAllBuffers() {
        int[] ids = buffers.keySet().stream().sorted().mapToInt(Integer::intValue).toArray();
        return removeBuffers(ids);
    }

    private void onBufferRamDestroy(int id) {
        if (id != RESERVED_SCREEN_INDEX && screenInstance != null
                && screenInstance instanceof VideoRamRasterizer rasterizer) {
            synchronized (screenInstance) {
                rasterizer.removeBuffer(node.address(), id);
            }
        }
        if (id == bufferIndex) {
            bufferIndex = RESERVED_SCREEN_INDEX;
        }
    }

    private boolean resolveInvokeCosts(int index, Context context, double budgetCost, int units, double energyCost) {
        if (index == RESERVED_SCREEN_INDEX) {
            context.consumeCallBudget(budgetCost);
            return node.tryChangeBuffer(-units * energyCost);
        }
        return true;
    }

    private double bitbltBudgetCost(TextBuffer dst, TextBuffer src) {
        if (src instanceof GpuTextBuffer page) {
            if (dst instanceof GpuTextBuffer) {
                return 0.0; // No cost writing to RAM.
            }
            if (page.dirty()) {
                return BITBLT_BASE * (src.getWidth() * src.getHeight()) / (double) MAX_CELLS;
            }
            return 0.001;
        }
        return 0.0;
    }

    private double bitbltEnergyCost(TextBuffer dst) {
        if (dst instanceof GpuTextBuffer) {
            return 0.0;
        }
        return ENERGY_COPY / 15;
    }

    private String clockInfo() {
        return clockPart(COST_SET_BACKGROUND) + "/" + clockPart(COST_SET_FOREGROUND) + "/"
                + clockPart(COST_SET_PALETTE_COLOR) + "/" + clockPart(COST_SET) + "/"
                + clockPart(COST_COPY) + "/" + clockPart(COST_FILL);
    }

    private String clockPart(double cost) {
        return String.valueOf(((long) (2000 / cost)) / 100);
    }

    // ------------------------------------------------------------------ //
    // Device info (APU-style: processor class, CPU clock + GPU clock).
    // ------------------------------------------------------------------ //

    @Override
    public Map<String, String> getDeviceInfo() {
        Map<String, String> info = new HashMap<>();
        info.put(DeviceAttribute.Class, DeviceClass.Processor);
        info.put(DeviceAttribute.Description, "LPU");
        info.put(DeviceAttribute.Vendor, "Groq");
        info.put(DeviceAttribute.Product, "Groq LPU");
        info.put(DeviceAttribute.Capacity, String.valueOf((long) totalVRAM));
        info.put(DeviceAttribute.Width, String.valueOf(OcBuffers.bits(MAX_DEPTH)));
        info.put(DeviceAttribute.Clock, ((long) (CPU_CALL_BUDGET * 1000)) + "+" + clockInfo());
        info.put("memory", "231MB");
        return info;
    }

    // ------------------------------------------------------------------ //
    // GPU callbacks (same Lua API as the stock graphics card).
    // ------------------------------------------------------------------ //

    @Callback(direct = true, doc = "function(): number -- returns the index of the currently selected buffer. 0 is reserved for the screen. Can return 0 even when there is no screen")
    public Object[] getActiveBuffer(Context context, Arguments args) {
        return new Object[]{bufferIndex};
    }

    @Callback(direct = true, doc = "function(index: number): number -- Sets the active buffer to `index`. 1 is the first vram buffer and 0 is reserved for the screen. returns nil for invalid index (0 is always valid)")
    public Object[] setActiveBuffer(Context context, Arguments args) {
        int previous = bufferIndex;
        int next = args.checkInteger(0);
        if (next != RESERVED_SCREEN_INDEX && !buffers.containsKey(next)) {
            return new Object[]{null, "invalid buffer index"};
        }
        bufferIndex = next;
        return new Object[]{previous};
    }

    @Callback(direct = true, doc = "function(): number -- Returns an array of indexes of the allocated buffers")
    public Object[] buffers(Context context, Arguments args) {
        return new Object[]{buffers.keySet().stream().sorted().mapToInt(Integer::intValue).toArray()};
    }

    @Callback(direct = true, doc = "function([width: number, height: number]): number -- allocates a new buffer with dimensions width*height (defaults to max resolution) and appends it to the buffer list. Returns the index of the new buffer and returns nil with an error message on failure. A buffer can be allocated even when there is no screen bound to this gpu. Index 0 is always reserved for the screen and thus the lowest index of an allocated buffer is always 1.")
    public Object[] allocateBuffer(Context context, Arguments args) {
        int width = args.optInteger(0, MAX_WIDTH);
        int height = args.optInteger(1, MAX_HEIGHT);
        int size = width * height;
        if (width <= 0 || height <= 0) {
            return new Object[]{null, "invalid page dimensions: must be greater than zero"};
        } else if (size > (totalVRAM - usedMemory())) {
            return new Object[]{null, "not enough video memory"};
        } else if (node == null) {
            return new Object[]{null, "graphics card appears disconnected"};
        }
        li.cil.oc.util.TextBuffer data = OcBuffers.newBuffer(width, height, MAX_DEPTH);
        GpuTextBuffer page = GpuTextBuffer$.MODULE$.wrap(node.address(), nextBufferIndex(), data);
        buffers.put(page.id(), page);
        return new Object[]{page.id()};
    }

    @Callback(direct = true, doc = "function(index: number): boolean -- Closes buffer at `index`. Returns true if a buffer closed. If the current buffer is closed, index moves to 0")
    public Object[] freeBuffer(Context context, Arguments args) {
        int index = args.optInteger(0, bufferIndex);
        if (removeBuffers(new int[]{index}) == 1) {
            return new Object[]{true};
        }
        return new Object[]{null, "no buffer at index"};
    }

    @Callback(direct = true, doc = "function(): number -- Closes all buffers and returns the count. If the active buffer is closed, index moves to 0")
    public Object[] freeAllBuffers(Context context, Arguments args) {
        return new Object[]{removeAllBuffers()};
    }

    @Callback(direct = true, doc = "function(): number -- returns the total memory size of the gpu vram. This does not include the screen.")
    public Object[] totalMemory(Context context, Arguments args) {
        return new Object[]{totalVRAM};
    }

    @Callback(direct = true, doc = "function(): number -- returns the total free memory not allocated to buffers. This does not include the screen.")
    public Object[] freeMemory(Context context, Arguments args) {
        return new Object[]{totalVRAM - usedMemory()};
    }

    @Callback(direct = true, doc = "function(index: number): number, number -- returns the buffer size at index. Returns the screen resolution for index 0. returns nil for invalid indexes")
    public Object[] getBufferSize(Context context, Arguments args) throws Exception {
        int index = args.optInteger(0, bufferIndex);
        return onBuffer(index, buffer -> new Object[]{buffer.getWidth(), buffer.getHeight()});
    }

    @Callback(direct = true, doc = "function([dst: number, col: number, row: number, width: number, height: number, src: number, fromCol: number, fromRow: number]):boolean -- bitblt from buffer to screen. All parameters are optional. Writes to `dst` page in rectangle `x, y, width, height`, defaults to the bound screen and its viewport. Reads data from `src` page at `fx, fy`, default is the active page from position 1, 1")
    public Object[] bitblt(Context context, Arguments args) throws Exception {
        int dstIndex = args.optInteger(0, RESERVED_SCREEN_INDEX);
        TextBuffer dst = dstIndex == RESERVED_SCREEN_INDEX ? screenInstance : buffers.get(dstIndex);
        if (dst == null) {
            return new Object[]{null, dstIndex == RESERVED_SCREEN_INDEX ? "no screen" : "invalid buffer index"};
        }
        synchronized (dst) {
            int col = args.optInteger(1, 1);
            int row = args.optInteger(2, 1);
            int w = args.optInteger(3, dst.getWidth());
            int h = args.optInteger(4, dst.getHeight());
            int srcIndex = args.optInteger(5, bufferIndex);
            TextBuffer src = srcIndex == RESERVED_SCREEN_INDEX ? screenInstance : buffers.get(srcIndex);
            if (src == null) {
                return new Object[]{null, srcIndex == RESERVED_SCREEN_INDEX ? "no screen" : "invalid buffer index"};
            }
            synchronized (src) {
                int fromCol = args.optInteger(6, 1);
                int fromRow = args.optInteger(7, 1);

                double budgetCost = bitbltBudgetCost(dst, src);
                double energyCost = bitbltEnergyCost(dst);
                double tierCredit = 1.5; // Tier-2-class (tier + 1) * 0.5 = 1.0, times 1.5.
                double overBudget = budgetCost - tierCredit;

                if (overBudget > 0) {
                    if (budgetExhausted) {
                        if (overBudget > tierCredit) {
                            double pauseNeeded = overBudget - tierCredit;
                            context.pause((pauseNeeded / tierCredit) / 20);
                        }
                        budgetCost = 0;
                    } else {
                        budgetExhausted = true;
                        throw new LimitReachedException();
                    }
                }
                budgetExhausted = false;

                if (resolveInvokeCosts(dstIndex, context, budgetCost, w * h, energyCost)) {
                    if (dstIndex == srcIndex) {
                        int tx = col - fromCol;
                        int ty = row - fromRow;
                        dst.copy(fromCol - 1, fromRow - 1, w, h, tx, ty);
                        return new Object[]{true};
                    }
                    GpuTextBuffer$.MODULE$.bitblt(dst, col, row, w, h, src, fromCol, fromRow);
                    return new Object[]{true};
                }
                return new Object[]{null, "not enough energy"};
            }
        }
    }

    @Callback(doc = "function(address:string[, reset:boolean=true]):boolean -- Binds the GPU to the screen with the specified address and resets screen settings if `reset` is true.")
    public Object[] bind(Context context, Arguments args) {
        String address = args.checkString(0);
        boolean reset = args.optBoolean(1, true);
        Node target = node.network() != null ? node.network().node(address) : null;
        if (target == null) {
            return new Object[]{null, "invalid address"};
        }
        if (target.host() instanceof TextBuffer screen) {
            screenAddress = address;
            screenInstance = screen;
            synchronized (screen) {
                if (reset) {
                    screen.setResolution(Math.min(MAX_WIDTH, screen.getMaximumWidth()),
                            Math.min(MAX_HEIGHT, screen.getMaximumHeight()));
                    TextBuffer.ColorDepth[] depths = TextBuffer.ColorDepth.values();
                    screen.setColorDepth(depths[Math.min(MAX_DEPTH.ordinal(), screen.getMaximumColorDepth().ordinal())]);
                    screen.setForegroundColor(0xFFFFFF);
                    screen.setBackgroundColor(0x000000);
                    if (screen instanceof VideoRamRasterizer rasterizer) {
                        rasterizer.removeAllBuffers();
                    }
                } else {
                    context.pause(0); // Discourage realtime output to multiple screens with one GPU.
                }
                return new Object[]{true};
            }
        }
        return new Object[]{null, "not a screen"};
    }

    @Callback(direct = true, doc = "function():string -- Get the address of the screen the GPU is currently bound to.")
    public Object[] getScreen(Context context, Arguments args) throws Exception {
        return onBuffer(RESERVED_SCREEN_INDEX, screen -> new Object[]{screen.node().address()});
    }

    @Callback(direct = true, doc = "function():number, boolean -- Get the current background color and whether it's from the palette or not.")
    public Object[] getBackground(Context context, Arguments args) throws Exception {
        return onActiveBuffer(screen -> new Object[]{screen.getBackgroundColor(), screen.isBackgroundFromPalette()});
    }

    @Callback(direct = true, doc = "function(value:number[, palette:boolean]):number, number or nil -- Sets the background color to the specified value. Optionally takes an explicit palette index. Returns the old value and if it was from the palette its palette index.")
    public Object[] setBackground(Context context, Arguments args) throws Exception {
        int color = args.checkInteger(0);
        if (bufferIndex == RESERVED_SCREEN_INDEX) {
            context.consumeCallBudget(COST_SET_BACKGROUND);
        }
        return onActiveBuffer(screen -> {
            int oldValue = screen.getBackgroundColor();
            Object oldColor;
            Object oldIndex;
            if (screen.isBackgroundFromPalette()) {
                oldColor = screen.getPaletteColor(oldValue);
                oldIndex = oldValue;
            } else {
                oldColor = oldValue;
                oldIndex = null;
            }
            screen.setBackgroundColor(color, args.optBoolean(1, false));
            return new Object[]{oldColor, oldIndex};
        });
    }

    @Callback(direct = true, doc = "function():number, boolean -- Get the current foreground color and whether it's from the palette or not.")
    public Object[] getForeground(Context context, Arguments args) throws Exception {
        return onActiveBuffer(screen -> new Object[]{screen.getForegroundColor(), screen.isForegroundFromPalette()});
    }

    @Callback(direct = true, doc = "function(value:number[, palette:boolean]):number, number or nil -- Sets the foreground color to the specified value. Optionally takes an explicit palette index. Returns the old value and if it was from the palette its palette index.")
    public Object[] setForeground(Context context, Arguments args) throws Exception {
        int color = args.checkInteger(0);
        if (bufferIndex == RESERVED_SCREEN_INDEX) {
            context.consumeCallBudget(COST_SET_FOREGROUND);
        }
        return onActiveBuffer(screen -> {
            int oldValue = screen.getForegroundColor();
            Object oldColor;
            Object oldIndex;
            if (screen.isForegroundFromPalette()) {
                oldColor = screen.getPaletteColor(oldValue);
                oldIndex = oldValue;
            } else {
                oldColor = oldValue;
                oldIndex = null;
            }
            screen.setForegroundColor(color, args.optBoolean(1, false));
            return new Object[]{oldColor, oldIndex};
        });
    }

    @Callback(direct = true, doc = "function(index:number):number -- Get the palette color at the specified palette index.")
    public Object[] getPaletteColor(Context context, Arguments args) throws Exception {
        int index = args.checkInteger(0);
        return onActiveBuffer(screen -> {
            try {
                return new Object[]{screen.getPaletteColor(index)};
            } catch (ArrayIndexOutOfBoundsException e) {
                throw new IllegalArgumentException("invalid palette index");
            }
        });
    }

    @Callback(direct = true, doc = "function(index:number, color:number):number -- Set the palette color at the specified palette index. Returns the previous value.")
    public Object[] setPaletteColor(Context context, Arguments args) throws Exception {
        int index = args.checkInteger(0);
        int color = args.checkInteger(1);
        if (bufferIndex == RESERVED_SCREEN_INDEX) {
            context.consumeCallBudget(COST_SET_PALETTE_COLOR);
            context.pause(0.1);
        }
        return onActiveBuffer(screen -> {
            try {
                int oldColor = screen.getPaletteColor(index);
                screen.setPaletteColor(index, color);
                return new Object[]{oldColor};
            } catch (ArrayIndexOutOfBoundsException e) {
                throw new IllegalArgumentException("invalid palette index");
            }
        });
    }

    @Callback(direct = true, doc = "function():number -- Returns the currently set color depth.")
    public Object[] getDepth(Context context, Arguments args) throws Exception {
        return onActiveBuffer(screen -> new Object[]{OcBuffers.bits(screen.getColorDepth())});
    }

    @Callback(doc = "function(depth:number):number -- Set the color depth. Returns the previous value.")
    public Object[] setDepth(Context context, Arguments args) throws Exception {
        int depth = args.checkInteger(0);
        return onActiveBuffer(screen -> {
            TextBuffer.ColorDepth oldDepth = screen.getColorDepth();
            switch (depth) {
                case 1 -> screen.setColorDepth(TextBuffer.ColorDepth.OneBit);
                case 4 -> {
                    if (MAX_DEPTH.ordinal() >= TextBuffer.ColorDepth.FourBit.ordinal()) {
                        screen.setColorDepth(TextBuffer.ColorDepth.FourBit);
                    } else {
                        throw new IllegalArgumentException("unsupported depth");
                    }
                }
                case 8 -> {
                    if (MAX_DEPTH.ordinal() >= TextBuffer.ColorDepth.EightBit.ordinal()) {
                        screen.setColorDepth(TextBuffer.ColorDepth.EightBit);
                    } else {
                        throw new IllegalArgumentException("unsupported depth");
                    }
                }
                default -> throw new IllegalArgumentException("unsupported depth");
            }
            return new Object[]{oldDepth};
        });
    }

    @Callback(direct = true, doc = "function():number -- Get the maximum supported color depth by the current GPU+screen combo.")
    public Object[] maxDepth(Context context, Arguments args) throws Exception {
        return onActiveBuffer(screen -> {
            TextBuffer.ColorDepth[] depths = TextBuffer.ColorDepth.values();
            TextBuffer.ColorDepth combined = depths[Math.min(MAX_DEPTH.ordinal(), screen.getMaximumColorDepth().ordinal())];
            return new Object[]{OcBuffers.bits(combined)};
        });
    }

    @Callback(direct = true, doc = "function():number -- Get the maximum color depth supported by the GPU.")
    public Object[] hardwareDepth(Context context, Arguments args) {
        return new Object[]{OcBuffers.bits(MAX_DEPTH)};
    }

    @Callback(direct = true, doc = "function():number, number -- Get the current screen resolution.")
    public Object[] getResolution(Context context, Arguments args) throws Exception {
        return onActiveBuffer(screen -> new Object[]{screen.getWidth(), screen.getHeight()});
    }

    @Callback(doc = "function(width:number, height:number):boolean -- Set the screen resolution. Returns true if the resolution changed.")
    public Object[] setResolution(Context context, Arguments args) throws Exception {
        int w = args.checkInteger(0);
        int h = args.checkInteger(1);
        if (w < 1 || h < 1 || w > MAX_WIDTH || h > MAX_HEIGHT || h * w > MAX_WIDTH * MAX_HEIGHT) {
            throw new IllegalArgumentException("unsupported resolution");
        }
        return onActiveBuffer(screen -> new Object[]{screen.setResolution(w, h)});
    }

    @Callback(direct = true, doc = "function():number, number -- Get the maximum screen resolution supported by the current GPU+screen combo.")
    public Object[] maxResolution(Context context, Arguments args) throws Exception {
        return onActiveBuffer(screen -> new Object[]{
                Math.min(MAX_WIDTH, screen.getMaximumWidth()),
                Math.min(MAX_HEIGHT, screen.getMaximumHeight())});
    }

    @Callback(direct = true, doc = "function():number, number -- Get the default screen resolution.")
    public Object[] getDefaultResolution(Context context, Arguments args) throws Exception {
        return onActiveBuffer(screen -> new Object[]{
                Math.min(MAX_WIDTH, screen.getMaximumWidth()),
                Math.min(MAX_HEIGHT, screen.getMaximumHeight())});
    }

    @Callback(direct = true, doc = "function():number, number -- Get the maximum screen resolution supported by the GPU.")
    public Object[] hardwareResolution(Context context, Arguments args) {
        return new Object[]{MAX_WIDTH, MAX_HEIGHT};
    }

    @Callback(direct = true, doc = "function():number, number -- Get the current viewport resolution.")
    public Object[] getViewport(Context context, Arguments args) throws Exception {
        return onActiveBuffer(screen -> new Object[]{screen.getViewportWidth(), screen.getViewportHeight()});
    }

    @Callback(doc = "function(width:number, height:number):boolean -- Set the viewport resolution. Cannot exceed the screen resolution. Returns true if the resolution changed.")
    public Object[] setViewport(Context context, Arguments args) throws Exception {
        int w = args.checkInteger(0);
        int h = args.checkInteger(1);
        if (w < 1 || h < 1 || w > MAX_WIDTH || h > MAX_HEIGHT || h * w > MAX_WIDTH * MAX_HEIGHT) {
            throw new IllegalArgumentException("unsupported viewport size");
        }
        return onActiveBuffer(screen -> {
            if (w > screen.getWidth() || h > screen.getHeight()) {
                throw new IllegalArgumentException("unsupported viewport size");
            }
            return new Object[]{screen.setViewport(w, h)};
        });
    }

    @Callback(direct = true, doc = "function(x:number, y:number):string, number, number, number or nil, number or nil -- Get the value displayed on the screen at the specified index, as well as the foreground and background color. If the foreground or background is from the palette, returns the palette indices as fourth and fifth results, else nil, respectively.")
    public Object[] get(Context context, Arguments args) throws Exception {
        int x = args.checkInteger(0) - 1;
        int y = args.checkInteger(1) - 1;
        return onActiveBuffer(screen -> {
            int fgValue = screen.getForegroundColor(x, y);
            Object fgColor;
            Object fgIndex;
            if (screen.isForegroundFromPalette(x, y)) {
                fgColor = screen.getPaletteColor(fgValue);
                fgIndex = fgValue;
            } else {
                fgColor = fgValue;
                fgIndex = null;
            }
            int bgValue = screen.getBackgroundColor(x, y);
            Object bgColor;
            Object bgIndex;
            if (screen.isBackgroundFromPalette(x, y)) {
                bgColor = screen.getPaletteColor(bgValue);
                bgIndex = bgValue;
            } else {
                bgColor = bgValue;
                bgIndex = null;
            }
            String ch = new StringBuilder().appendCodePoint(screen.getCodePoint(x, y)).toString();
            return new Object[]{ch, fgColor, bgColor, fgIndex, bgIndex};
        });
    }

    @Callback(direct = true, doc = "function(x:number, y:number, value:string[, vertical:boolean]):boolean -- Plots a string value to the screen at the specified position. Optionally writes the string vertically.")
    public Object[] set(Context context, Arguments args) throws Exception {
        int x = args.checkInteger(0) - 1;
        int y = args.checkInteger(1) - 1;
        String value = args.checkString(2);
        boolean vertical = args.optBoolean(3, false);
        return onActiveBuffer(screen -> {
            if (resolveInvokeCosts(bufferIndex, context, COST_SET, codePointLength(value), ENERGY_SET)) {
                screen.set(x, y, value, vertical);
                return new Object[]{true};
            }
            return new Object[]{null, "not enough energy"};
        });
    }

    @Callback(direct = true, doc = "function(x:number, y:number, width:number, height:number, tx:number, ty:number):boolean -- Copies a portion of the screen from the specified location with the specified size by the specified translation.")
    public Object[] copy(Context context, Arguments args) throws Exception {
        int x = args.checkInteger(0) - 1;
        int y = args.checkInteger(1) - 1;
        int w = Math.max(0, args.checkInteger(2));
        int h = Math.max(0, args.checkInteger(3));
        int tx = args.checkInteger(4);
        int ty = args.checkInteger(5);
        return onActiveBuffer(screen -> {
            if (resolveInvokeCosts(bufferIndex, context, COST_COPY, w * h, ENERGY_COPY)) {
                screen.copy(x, y, w, h, tx, ty);
                return new Object[]{true};
            }
            return new Object[]{null, "not enough energy"};
        });
    }

    @Callback(direct = true, doc = "function(x:number, y:number, width:number, height:number, char:string):boolean -- Fills a portion of the screen at the specified position with the specified size with the specified character.")
    public Object[] fill(Context context, Arguments args) throws Exception {
        int x = args.checkInteger(0) - 1;
        int y = args.checkInteger(1) - 1;
        int w = Math.max(0, args.checkInteger(2));
        int h = Math.max(0, args.checkInteger(3));
        String value = args.checkString(4);
        if (codePointLength(value) == 1) {
            return onActiveBuffer(screen -> {
                int c = value.codePointAt(0);
                double cost = c == ' ' ? ENERGY_CLEAR : ENERGY_FILL;
                if (resolveInvokeCosts(bufferIndex, context, COST_FILL, w * h, cost)) {
                    screen.fill(x, y, w, h, c);
                    return new Object[]{true};
                }
                return new Object[]{null, "not enough energy"};
            });
        }
        throw new Exception("invalid fill value");
    }

    // ------------------------------------------------------------------ //
    // Network events.
    // ------------------------------------------------------------------ //

    @Override
    public void onConnect(Node node) {
        super.onConnect(node);
        if (screenInstance == null && screenAddress != null && screenAddress.equals(node.address())
                && node.host() instanceof TextBuffer buffer) {
            screenInstance = buffer;
        }
    }

    @Override
    public void onDisconnect(Node node) {
        super.onDisconnect(node);
        if (node == this.node || (screenAddress != null && screenAddress.equals(node.address()))) {
            screenAddress = null;
            screenInstance = null;
        }
    }

    @Override
    public void onMessage(Message message) {
        super.onMessage(message);
        if (message.source() == null) {
            return;
        }
        boolean stopped = "computer.stopped".equals(message.name());
        boolean started = "computer.started".equals(message.name());
        if ((stopped || started) && node.isNeighborOf(message.source())) {
            bufferIndex = RESERVED_SCREEN_INDEX;
            removeAllBuffers();
        }
        if (stopped && node.isNeighborOf(message.source()) && screenInstance != null) {
            TextBuffer screen = screenInstance;
            synchronized (screen) {
                screen.setResolution(Math.min(MAX_WIDTH, screen.getMaximumWidth()),
                        Math.min(MAX_HEIGHT, screen.getMaximumHeight()));
                TextBuffer.ColorDepth[] depths = TextBuffer.ColorDepth.values();
                screen.setColorDepth(depths[Math.min(MAX_DEPTH.ordinal(), screen.getMaximumColorDepth().ordinal())]);
                screen.setForegroundColor(0xFFFFFF);
                screen.setBackgroundColor(0x000000);
                screen.fill(0, 0, screen.getWidth(), screen.getHeight(), 0x20);
            }
        }
    }

    // ------------------------------------------------------------------ //
    // Persistence (screen binding, active buffer, VRAM pages).
    // ------------------------------------------------------------------ //

    @Override
    public void loadData(DataComponentHolder holder) throws UnrecoverablePersistanceException {
        super.loadData(holder);
        CustomData state = holder.get(TensorDataComponents.GPU_STATE.get());
        if (state != null) {
            CompoundTag tag = state.copyTag();
            screenAddress = tag.contains(TAG_SCREEN) ? tag.getString(TAG_SCREEN) : null;
            screenInstance = null;
            bufferIndex = tag.contains(TAG_BUFFER) ? tag.getInt(TAG_BUFFER) : RESERVED_SCREEN_INDEX;
        }
        removeAllBuffers();
        CustomData vram = holder.get(TensorDataComponents.GPU_VRAM.get());
        if (vram != null && node.address() != null) {
            ListTag pages = vram.copyTag().getList(TAG_PAGES, Tag.TAG_COMPOUND);
            for (int i = 0; i < pages.size(); i++) {
                try {
                    CompoundTag page = pages.getCompound(i);
                    int id = page.getInt(TAG_PAGE_ID);
                    TensorGpuEnvironment.TensorPageHolder pageHolder =
                            TensorGpuEnvironment.TensorPageHolder.fromTag(page.getCompound(TAG_PAGE_DATA));
                    if (pageHolder == null) {
                        continue;
                    }
                    li.cil.oc.util.TextBuffer data = OcBuffers.newBuffer(1, 1, TextBuffer.ColorDepth.OneBit);
                    data.loadData(pageHolder);
                    buffers.put(id, GpuTextBuffer$.MODULE$.wrap(node.address(), id, data));
                } catch (Exception ignored) {
                    // A corrupt page must never break world loading; the
                    // buffer simply starts out empty.
                }
            }
        }
    }

    @Override
    public void saveData(MutableDataComponentHolder holder) {
        super.saveData(holder);
        CompoundTag tag = new CompoundTag();
        if (screenAddress != null) {
            tag.putString(TAG_SCREEN, screenAddress);
        }
        tag.putInt(TAG_BUFFER, bufferIndex);
        holder.set(TensorDataComponents.GPU_STATE.get(), CustomData.of(tag));

        CompoundTag vram = new CompoundTag();
        ListTag pages = new ListTag();
        List<Integer> ids = new ArrayList<>(buffers.keySet());
        Collections.sort(ids);
        for (int id : ids) {
            try {
                GpuTextBuffer page = buffers.get(id);
                if (page == null) {
                    continue;
                }
                TensorGpuEnvironment.TensorPageHolder pageHolder = new TensorGpuEnvironment.TensorPageHolder();
                page.data().saveData(pageHolder);
                CompoundTag dataTag = pageHolder.toTag();
                if (dataTag == null) {
                    continue;
                }
                CompoundTag pageTag = new CompoundTag();
                pageTag.putInt(TAG_PAGE_ID, id);
                pageTag.put(TAG_PAGE_DATA, dataTag);
                pages.add(pageTag);
            } catch (Exception ignored) {
                // Persistence is best-effort per page; gameplay is unaffected.
            }
        }
        vram.put(TAG_PAGES, pages);
        holder.set(TensorDataComponents.GPU_VRAM.get(), CustomData.of(vram));
    }
}
