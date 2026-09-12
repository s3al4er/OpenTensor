package com.opentensor;

import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ComponentConnector;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;

import java.util.HashMap;
import java.util.Map;

/**
 * Noctua NF-A14 industrialPPC-3000 PWM fan environment.
 *
 * <p>Passive upgrade: it does not need ticking itself. Tensor GPUs count
 * neighboring fan environments each tick (see {@link TensorThermal}) and
 * cool down accordingly. The {@code fan} component only reports its speed
 * so scripts like {@code opentensor-smi} can show it.</p>
 */
public final class NoctuaFanEnvironment extends AbstractManagedEnvironment implements DeviceInfo {
    /** industrialPPC-3000 PWM maximum speed. */
    public static final int MAX_RPM = 3000;

    public static final String ITEM_ID = "noctua_nf_a14";
    /** Card-mounted variant for plain computers (card slot). */
    public static final String CARD_ITEM_ID = "noctua_nf_a14_card";

    private final ComponentConnector node;
    private final EnvironmentHost host;

    public NoctuaFanEnvironment() {
        this(null);
    }

    public NoctuaFanEnvironment(EnvironmentHost host) {
        this.host = host;
        this.node = Network.newNode(this, Visibility.Neighbors)
                .withComponent("fan")
                .withConnector()
                .create();
    }

    /** The machine this fan is installed in (used for thermal matching). */
    public EnvironmentHost host() {
        return host;
    }

    @Override
    public Node node() {
        return node;
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        Map<String, String> info = new HashMap<>();
        info.put(DeviceAttribute.Class, DeviceClass.Generic);
        info.put(DeviceAttribute.Description, "Cooling fan");
        info.put(DeviceAttribute.Vendor, "Noctua");
        info.put(DeviceAttribute.Product, "Noctua NF-A14 industrialPPC-3000 PWM");
        info.put(DeviceAttribute.Capacity, String.valueOf(MAX_RPM));
        return info;
    }

    @Callback(direct = true, doc = "function():number -- Returns the fan speed in RPM.")
    public Object[] getSpeed(Context context, Arguments args) {
        return new Object[]{MAX_RPM};
    }
}
