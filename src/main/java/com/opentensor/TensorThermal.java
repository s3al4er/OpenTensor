package com.opentensor;

import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Node;

/**
 * Shared thermal model for Tensor GPUs.
 *
 * <p>Simple model (v1.0.9): cards start at {@link #AMBIENT_C}, heat up when
 * doing work (set/fill/copy/bitblt) plus idle heat while the machine runs,
 * cool with Newton-style passive cooling toward ambient, and cool much
 * faster for each Noctua fan in the same machine. If any card exceeds
 * {@link #OVERHEAT_C}, its host machine is stopped.</p>
 */
public final class TensorThermal {
    /** Server log for throttled thermal diagnostics. */
    public static final org.slf4j.Logger LOG = com.mojang.logging.LogUtils.getLogger();

    /** Idle/cooled resting point. */
    public static final double AMBIENT_C = 30.0;
    /** Overheat threshold: machine stops above this. */
    public static final double OVERHEAT_C = 100.0;

    /** Heat per single-cell plot. */
    public static final double HEAT_SET = 0.02;
    /** Heat per cell touched by fill/copy/bitblt. */
    public static final double HEAT_PER_CELL = 0.00015;
    /**
     * Idle heat per tick while the host machine is running.
     * With {@link #PASSIVE_K} below this settles at ~70 C with no fans.
     */
    public static final double IDLE_HEAT = 0.06;
    /**
     * Passive (Newton) cooling factor per tick, proportional to
     * {@code temperature - AMBIENT_C}. Gives stable equilibria instead of
     * runaway heating/cooling: ~70 C idle with no fans, ~77 C under light
     * load, ambient floor with even one fan.
     */
    public static final double PASSIVE_K = 0.0015;
    /** Extra cooling per tick per Noctua fan in the same machine. */
    public static final double FAN_COOL = 0.5;

    /** NBT key for the temperature inside GPU_STATE. */
    public static final String TAG_TEMP = "temp";

    private TensorThermal() {
    }

    public static double clamp(double value) {
        if (value < AMBIENT_C) {
            return AMBIENT_C;
        }
        if (value > 150.0) {
            return 150.0;
        }
        return value;
    }

    /**
     * Count Noctua fans installed in the same machine.
     *
     * <p>Iterates the full network member list ({@code network.nodes()}),
     * <em>not</em> {@code reachableNodes()}: the latter hides
     * {@code Neighbors}-visibility nodes (like our fans) that are not
     * directly connected to the querying node — and components in one
     * computer hang off the machine node, never directly off each other.
     * Matches by host identity, with a host-position fallback, so fans on
     * other networked machines are not counted.</p>
     */
    public static int countFans(Node node, EnvironmentHost host) {
        if (node == null || node.network() == null) {
            return 0;
        }
        int fans = 0;
        try {
            for (Node other : node.network().nodes()) {
                if (other == node) {
                    continue;
                }
                Environment env;
                try {
                    env = other.host();
                } catch (Throwable ignored) {
                    continue;
                }
                if (env instanceof NoctuaFanEnvironment fan && sameMachine(fan.host(), host)) {
                    fans++;
                }
            }
        } catch (Throwable ignored) {
            // Network iteration must never break the tick.
        }
        return fans;
    }

    /** Same machine by host reference, falling back to host position. */
    private static boolean sameMachine(EnvironmentHost a, EnvironmentHost b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        try {
            return a.getEnvironmentLevel() == b.getEnvironmentLevel()
                    && a.xPosition() == b.xPosition()
                    && a.yPosition() == b.yPosition()
                    && a.zPosition() == b.zPosition();
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * One tick of cooling/heating. Returns the new temperature.
     *
     * @param running whether the host machine is running (a stopped machine
     *                produces no idle heat and just cools down).
     */
    public static double tick(double current, int fans, boolean running) {
        double next = current - (current - AMBIENT_C) * PASSIVE_K - fans * FAN_COOL;
        if (running) {
            next += IDLE_HEAT;
        }
        return clamp(next);
    }
}
