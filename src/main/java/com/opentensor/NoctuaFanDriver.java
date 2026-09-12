package com.opentensor;

import li.cil.oc.api.driver.EnvironmentProvider;
import li.cil.oc.api.driver.item.Slot;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Item drivers exposing the Noctua fans as components. Two mount variants
 * share one environment ({@link NoctuaFanEnvironment}):
 * <ul>
 *   <li>{@link #BUS} — {@code noctua_nf_a14} in component-bus slots
 *   (servers and racks; plain computers have no bus slots).</li>
 *   <li>{@link #CARD} — {@code noctua_nf_a14_card} in card slots
 *   (plain computers and servers).</li>
 * </ul>
 * <p>Registered (together with {@link Provider}) via the public
 * {@code li.cil.oc.api.Driver} API only.</p>
 */
public final class NoctuaFanDriver implements li.cil.oc.api.driver.DriverItem {
    /** Bus-mounted fan for servers/racks (component-bus slot, tier 2). */
    public static final NoctuaFanDriver BUS =
            new NoctuaFanDriver(NoctuaFanEnvironment.ITEM_ID, Slot.ComponentBus, 2);
    /** Card-mounted fan for plain computers (card slot, tier 1). */
    public static final NoctuaFanDriver CARD =
            new NoctuaFanDriver(NoctuaFanEnvironment.CARD_ITEM_ID, Slot.Card, 1);
    public static final Provider PROVIDER = new Provider();

    private final String itemId;
    private final String slot;
    private final int tier;

    private NoctuaFanDriver(String itemId, String slot, int tier) {
        this.itemId = itemId;
        this.slot = slot;
        this.tier = tier;
    }

    @Override
    public boolean worksWith(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        var key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return OpenTensor.MODID.equals(key.getNamespace())
                && itemId.equals(key.getPath());
    }

    @Override
    public ManagedEnvironment createEnvironment(ItemStack stack, EnvironmentHost host) {
        Level level = host != null ? host.getEnvironmentLevel() : null;
        // Never create server-side environments on the client.
        if (level != null && level.isClientSide()) {
            return null;
        }
        if (stack.isEmpty()) {
            return null;
        }
        return new NoctuaFanEnvironment(host);
    }

    @Override
    public String slot(ItemStack stack) {
        return slot;
    }

    @Override
    public int tier(ItemStack stack) {
        return tier;
    }

    /**
     * Maps fan stacks to their environment class (used for docs/lookups).
     */
    public static final class Provider implements EnvironmentProvider {
        private Provider() {
        }

        @Override
        public Class<?> getEnvironment(ItemStack stack) {
            if (NoctuaFanDriver.BUS.worksWith(stack) || NoctuaFanDriver.CARD.worksWith(stack)) {
                return NoctuaFanEnvironment.class;
            }
            return null;
        }
    }
}
