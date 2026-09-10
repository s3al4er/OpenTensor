package com.opentensor;

import li.cil.oc.api.internal.TextBuffer;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Access to OC's packed-color formats and text buffers.
 *
 * <p>These live in Scala objects ({@code PackedColor.Depth}), whose
 * {@code $}-mangled binary names {@code javac} cannot resolve directly
 * (it mistakes them for nested classes of the companion mirror). All access
 * therefore goes through cached reflection looked up by binary name, which
 * {@link Class#forName} handles fine. Lookups happen once per class load;
 * buffer allocation itself is unaffected at runtime.</p>
 */
final class OcBuffers {
    private static final Object DEPTH_MODULE;
    private static final Method DEPTH_FORMAT;
    private static final Constructor<li.cil.oc.util.TextBuffer> TEXT_BUFFER_CTOR;

    static {
        try {
            Class<?> depthClass = Class.forName("li.cil.oc.util.PackedColor$Depth$");
            DEPTH_MODULE = depthClass.getField("MODULE$").get(null);
            DEPTH_FORMAT = depthClass.getMethod("format", TextBuffer.ColorDepth.class);
            @SuppressWarnings("unchecked")
            Constructor<li.cil.oc.util.TextBuffer> ctor = (Constructor<li.cil.oc.util.TextBuffer>)
                    li.cil.oc.util.TextBuffer.class.getConstructor(int.class, int.class,
                            Class.forName("li.cil.oc.util.PackedColor$ColorFormat"));
            TEXT_BUFFER_CTOR = ctor;
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private OcBuffers() {
    }

    /** Bit count for a color depth (mirrors OC's {@code PackedColor.Depth.bits}). */
    static int bits(TextBuffer.ColorDepth depth) {
        switch (depth) {
            case OneBit:
                return 1;
            case FourBit:
                return 4;
            case EightBit:
                return 8;
            case SixteenBit:
                return 16;
            default:
                throw new IllegalArgumentException("Unknown color depth: " + depth);
        }
    }

    /** Creates an OC text buffer with the format for the given depth. */
    static li.cil.oc.util.TextBuffer newBuffer(int width, int height, TextBuffer.ColorDepth depth) {
        try {
            Object format = DEPTH_FORMAT.invoke(DEPTH_MODULE, depth);
            return TEXT_BUFFER_CTOR.newInstance(width, height, format);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to allocate OC text buffer", e);
        }
    }
}
