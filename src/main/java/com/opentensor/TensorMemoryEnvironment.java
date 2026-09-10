package com.opentensor;

import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;

import java.util.HashMap;
import java.util.Map;

/**
 * Tensor DDR5 memory bank environment.
 *
 * <p>Exposes no Lua component (like the stock memory bank); its only purpose
 * is device info and holding a network node. The actual RAM amount is granted
 * via {@link TensorMemoryDriver#amount}.</p>
 */
public final class TensorMemoryEnvironment extends AbstractManagedEnvironment implements DeviceInfo {
    private final TensorMemorySpec spec;
    private final Node node;

    public TensorMemoryEnvironment(TensorMemorySpec spec) {
        this.spec = spec;
        this.node = Network.newNode(this, Visibility.Neighbors)
                .create();
    }

    @Override
    public Node node() {
        return node;
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        Map<String, String> info = new HashMap<>();
        info.put(DeviceAttribute.Class, DeviceClass.Memory);
        info.put(DeviceAttribute.Description, "Memory bank");
        info.put(DeviceAttribute.Vendor, "OpenTensor");
        info.put(DeviceAttribute.Product, "DDR5-4800 " + spec.label());
        info.put(DeviceAttribute.Clock, String.valueOf((int) (TensorMemoryDriver.CALL_BUDGET * 1000)));
        return info;
    }
}
