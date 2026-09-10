package com.opentensor;

import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;

import java.util.HashMap;
import java.util.Map;

/**
 * Tensor server CPU environment.
 *
 * <p>Exposes no Lua component (like the stock CPU); its only purpose is
 * device info and holding a network node. Architecture, component support
 * and call budget are granted via {@link TensorCpuDriver}.</p>
 */
public final class TensorCpuEnvironment extends AbstractManagedEnvironment implements DeviceInfo {
    private final TensorCpuSpec spec;
    private final Node node;

    public TensorCpuEnvironment(TensorCpuSpec spec) {
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
        info.put(DeviceAttribute.Class, DeviceClass.Processor);
        info.put(DeviceAttribute.Description, "CPU");
        info.put(DeviceAttribute.Vendor, "AMD");
        info.put(DeviceAttribute.Product, spec.product());
        info.put(DeviceAttribute.Clock, String.valueOf((int) (spec.callBudget() * 1000)));
        return info;
    }
}
