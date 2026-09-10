package com.opentensor;

import li.cil.oc.api.UnrecoverablePersistanceException;
import li.cil.oc.api.driver.EnvironmentProvider;
import li.cil.oc.api.driver.item.Slot;
import li.cil.oc.api.fs.Label;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.common.datacomponents.OCComponents$;
import li.cil.oc.common.item.data.DriveData;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Item driver exposing Tensor SSDs as {@code filesystem} components.
 *
 * <p>Mirrors the stock SSD path of {@code DriverFileSystem} (save-directory
 * backed storage, SSD energy costs, silent access): the address is stored in
 * {@code OCComponents.ADDRESS}, the label in {@code OCComponents.LABEL} and
 * locking/unmanaged flags are honored via {@code DriveData}. Registered
 * (together with {@link Provider}) via the public
 * {@code li.cil.oc.api.Driver} API only.</p>
 */
public final class TensorSsdDriver implements li.cil.oc.api.driver.DriverItem {
    public static final TensorSsdDriver INSTANCE = new TensorSsdDriver();
    public static final Provider PROVIDER = new Provider();

    /** Driver tier: fits Tier 3+ HDD slots (slots accept tier &lt;= theirs). */
    public static final int TIER = 2;

    /** Filesystem speed, like a stock SSDTier2 (tier + 4). */
    private static final int SPEED = TIER + 4;

    private static final Pattern UUID_VERIFIER =
            Pattern.compile("^([0-9a-f]{8}-(?:[0-9a-f]{4}-){3}[0-9a-f]{12})$");

    private TensorSsdDriver() {
    }

    private static TensorSsdSpec specOf(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        var key = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!OpenTensor.MODID.equals(key.getNamespace())) {
            return null;
        }
        return TensorSsdSpec.byId(key.getPath());
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
        TensorSsdSpec spec = specOf(stack);
        if (spec == null) {
            return null;
        }
        if (ServerLifecycleHooks.getCurrentServer() == null) {
            return null;
        }
        String address = getOrCreateAddress(stack);
        Label label = new SsdLabel(stack);
        DriveData drive = new DriveData(stack);
        long capacity = (long) spec.kilobytes() * 1024L;
        boolean buffered = li.cil.oc.Settings$.MODULE$.get().bufferChanges();
        li.cil.oc.api.fs.FileSystem fs =
                li.cil.oc.api.FileSystem.fromSaveDirectory(address, Math.max(capacity, 0), buffered);
        if (drive.isLocked()) {
            fs = li.cil.oc.api.FileSystem.asReadOnly(fs);
            label = new FixedLabel(label.getLabel(ServerLifecycleHooks.getCurrentServer().registryAccess()));
        }
        String sound = li.cil.oc.Settings$.MODULE$.resourceDomain() + ":ssd_access";
        double readCost = Math.max(li.cil.oc.Settings$.MODULE$.get().ssdReadCost(), 0);
        double writeCost = Math.max(li.cil.oc.Settings$.MODULE$.get().ssdWriteCost(), 0);
        ManagedEnvironment env = li.cil.oc.server.fs.FileSystem$.MODULE$
                .asManagedEnvironment(fs, label, host, sound, SPEED, readCost, writeCost);
        if (env != null && env.node() != null) {
            ((li.cil.oc.server.network.Node) env.node()).address_$eq(address);
        }
        return env;
    }

    @Override
    public String slot(ItemStack stack) {
        return Slot.HDD;
    }

    @Override
    public int tier(ItemStack stack) {
        return TIER;
    }

    private static String getOrCreateAddress(ItemStack stack) {
        String address = stack.get(OCComponents$.MODULE$.ADDRESS().get());
        if (address != null && UUID_VERIFIER.matcher(address).matches()) {
            return address;
        }
        String created = UUID.randomUUID().toString();
        stack.set(OCComponents$.MODULE$.ADDRESS().get(), created);
        return created;
    }

    /**
     * Read-write label backed by the stack's {@code OCComponents.LABEL},
     * mirroring the stock filesystem driver (16 characters max).
     */
    static final class SsdLabel implements Label {
        private final ItemStack stack;
        private String label;

        SsdLabel(ItemStack stack) {
            this.stack = stack;
        }

        @Override
        public String getLabel(HolderLookup.Provider provider) {
            return label;
        }

        @Override
        public void setLabel(String value) {
            label = value != null ? value.substring(0, Math.min(value.length(), 16)) : null;
        }

        @Override
        public void loadData(DataComponentHolder holder) throws UnrecoverablePersistanceException {
            label = holder.get(OCComponents$.MODULE$.LABEL().get());
        }

        @Override
        public void saveData(MutableDataComponentHolder holder) {
            if (label != null) {
                holder.set(OCComponents$.MODULE$.LABEL().get(), label);
            }
        }
    }

    /**
     * Fixed label used when the drive is locked (read-only).
     */
    static final class FixedLabel implements Label {
        private final String label;

        FixedLabel(String label) {
            this.label = label;
        }

        @Override
        public String getLabel(HolderLookup.Provider provider) {
            return label;
        }

        @Override
        public void setLabel(String value) {
            throw new IllegalArgumentException("label is read only");
        }

        @Override
        public void loadData(DataComponentHolder holder) throws UnrecoverablePersistanceException {
            // Fixed at construction time; nothing to load.
        }

        @Override
        public void saveData(MutableDataComponentHolder holder) {
            // Fixed at construction time; nothing to save.
        }
    }

    /**
     * Maps SSD stacks to their environment class (used for docs/lookups).
     */
    public static final class Provider implements EnvironmentProvider {
        private Provider() {
        }

        @Override
        public Class<?> getEnvironment(ItemStack stack) {
            return INSTANCE.worksWith(stack) ? li.cil.oc.server.component.FileSystem.class : null;
        }
    }
}
