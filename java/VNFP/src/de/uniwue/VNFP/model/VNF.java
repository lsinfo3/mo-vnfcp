package de.uniwue.VNFP.model;

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
     * Number of required CPU cores for this NF.
     */
    public final double cpuRequired;
    /**
     * Amount of required RAM (Mb).
     */
    public final double ramRequired;
    /**
     * Amount of required HDD capacities (Gb).
     */
    public final double hddRequired;
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
     * Pointer towards the VNF library that this type is a part of.
     */
    public VnfLib vnfLib;

    /**
     * Creates a new instance with the given contents.
     *
     * @param name               Name of this VNF type, e.g., "Firewall".
     * @param cpuRequired        Number of required CPU cores for this NF.
     * @param ramRequired        Amount of required RAM (Mb).
     * @param hddRequired        Amount of required HDD capacities (Gb).
     * @param delay              Latency of this VNF. (μs)
     * @param processingCapacity Capacity of this VNF. (Mbps)
     * @param maxInstances       Maximum number of allowed instances. -1 represents unbounded.
     */
    public VNF(String name, double cpuRequired, double ramRequired, double hddRequired, double delay, double processingCapacity, long maxInstances) {
        Objects.requireNonNull(name);
        if (name.trim().isEmpty()) {
            throw new IllegalArgumentException("name is empty");
        }
        if (cpuRequired < 0.0) {
            throw new IllegalArgumentException("cpuRequired = " + cpuRequired);
        }
        if (ramRequired < 0.0) {
            throw new IllegalArgumentException("ramRequired = " + ramRequired);
        }
        if (hddRequired < 0.0) {
            throw new IllegalArgumentException("hddRequired = " + hddRequired);
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

        this.name = name;
        this.cpuRequired = cpuRequired;
        this.ramRequired = ramRequired;
        this.hddRequired = hddRequired;
        this.delay = delay;
        this.processingCapacity = processingCapacity;
        this.maxInstances = maxInstances;
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
                ", cpuRequired=" + cpuRequired +
                ", ramRequired=" + ramRequired +
                ", hddRequired=" + hddRequired +
                ", delay=" + delay +
                ", processingCapacity=" + processingCapacity +
                ", maxInstances=" + maxInstances +
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
