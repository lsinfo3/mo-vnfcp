package de.uniwue.VNFP.model;

import java.util.Arrays;
import java.util.Objects;

import de.uniwue.VNFP.model.solution.VnfInstances;

/**
 * Objects of this class represent types of network functions.
 * They can be requested by a {@link TrafficRequest}.
 * Instances on nodes are represented by {@link VnfInstances} objects.
 *
 * @author alex
 */
public class VNF {
    /**
     * Name of this VNF type, e.g., "Firewall".
     */
    public final String name;
    /**
     * Number of required resources of every type (defined by VnfLib).
     */
    public final double[] reqResources;
    /**
     * Latency of this VNF. (μs)
     */
    public final double delay;
    /**
     * Capacity of this VNF. (Mbps)
     */
    public final double processingCapacity;
    /**
     * Maximum number of allowed instances. -1 represents unbounded.
     */
    public final long maxInstances;
    /**
     * Relative weight of the penalty for a request's migration between instances of this type.
     */
    public final double flowMigrationPenalty;
    /**
     * Pointer towards the VNF library that this type is a part of.
     */
    public VnfLib vnfLib;

    /**
     * Creates a new instance with the given contents.
     *
     * @param name                 Name of this VNF type, e.g., "Firewall".
     * @param delay                Latency of this VNF. (μs)
     * @param processingCapacity   Capacity of this VNF. (Mbps)
     * @param maxInstances         Maximum number of allowed instances. -1 represents unbounded.
     * @param flowMigrationPenalty Relative weight of the penalty for a request's migration between instances of this type.
     * @param reqResources         Number of required resources of every type (defined by VnfLib).
     */
    public VNF(String name, double delay, double processingCapacity, long maxInstances, double flowMigrationPenalty, double[] reqResources) {
        Objects.requireNonNull(name);
        if (name.trim().isEmpty()) {
            throw new IllegalArgumentException("name is empty");
        }
        if (delay < 0.0) {
            throw new IllegalArgumentException("delay = " + delay);
        }
        if (processingCapacity < 0.0) {
            throw new IllegalArgumentException("processingCapacity = " + processingCapacity);
        }
        if (maxInstances < -1L) {
            throw new IllegalArgumentException("maxInstances = " + maxInstances);
        }
        if (flowMigrationPenalty < 0.0) {
            throw new IllegalArgumentException("flowMigrationPenalty = " + flowMigrationPenalty);
        }
        for (int i = 0; i < reqResources.length; i++) {
            if (reqResources[i] < 0.0) {
                throw new IllegalArgumentException("reqResources["+i+"] = " + reqResources[i]);
            }
        }

        this.name = name;
        this.reqResources = reqResources;
        this.delay = delay;
        this.processingCapacity = processingCapacity;
        this.maxInstances = maxInstances;
        this.flowMigrationPenalty = flowMigrationPenalty;
    }

    /**
     * If the maximum latency between this function and the given VNF is bounded, the corresponding
     * VnfPair object is returned.
     *
     * @param vnf_b The next VNF type of a request.
     * @return VnfPair object containing the maximum allowed intermediate latency, or null, if none is defined.
     */
    public VnfLib.VnfPair getPair(VNF vnf_b) {
        return Objects.requireNonNull(vnfLib).getPair(this, vnf_b);
    }

    @Override
    public String toString() {
        return "VNF{" +
                "name=" + name +
                ", delay=" + delay +
                ", processingCapacity=" + processingCapacity +
                ", maxInstances=" + maxInstances +
                ", flowMigrationPenalty=" + flowMigrationPenalty +
                ", reqResources=" + Arrays.toString(reqResources) +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        VNF vnf = (VNF) o;

        return name.equals(vnf.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
