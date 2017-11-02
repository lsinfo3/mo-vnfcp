package de.uniwue.VNFP.model.solution;

import de.uniwue.VNFP.model.Node;
import de.uniwue.VNFP.model.TrafficRequest;
import de.uniwue.VNFP.model.VNF;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

/**
 * This data class is used to store information on how many
 * VNF instances of which type are stored on which node.
 *
 * @author alex
 */
public class VnfInstances {
    /**
     * The node on which the VNF instances are instantiated.
     */
    public final Node node;
    /**
     * The type of VNF instance.
     */
    public final VNF type;
    /**
     * This array stores the load-information on every instance of the specified VNF-type on this node.
     * The length of the array equals the number of instances.
     * The loads themselves are given in Mb/s, not percentage.
     */
    public final double[] loads;
    /**
     * This array stores individual traffic requests whose flows are utilizing this VNF-type on this node.
     * Length and indices of the outer array should match <code>loads</code>.
     * Can be null, if the mapping is not required.
     */
    public final TrafficRequest[][] flows;

    /**
     * Creates a new VnfInstances to store assignment information.
     *
     * @param node  The node on which the VNF instances are instantiated.
     * @param type  The type of VNF instance.
     * @param loads This array stores the loads-information on every instance of the specified VNF-type on this node.
     *              The length of the array equals the number of instances.
     *              The loads themselves are given in Mbps, not percentage.
     * @param flows This array stores individual traffic requests whose flows are utilizing this VNF-type on this node.
     *              Length and indices of the outer array should match <code>loads</code>.
     *              Can be null, if the mapping is not required.
     */
    public VnfInstances(Node node, VNF type, double[] loads, TrafficRequest[][] flows) {
        this.node = Objects.requireNonNull(node);
        this.type = Objects.requireNonNull(type);
        this.loads = Objects.requireNonNull(loads);
        this.flows = flows;

        if (flows != null && flows.length != loads.length) {
            throw new IllegalArgumentException("flows.length=" + flows.length + ", loads.length=" + loads.length);
        }
    }

    /**
     * Creates a new VnfInstances to store assignment information.
     *
     * @param node  The node on which the VNF instances are instantiated.
     * @param type  The type of VNF instance.
     * @param loads This list stores the load-information on every instance of the specified VNF-type on this node.
     *              The size of the list equals the number of instances.
     *              The loads themselves are given in Mbps, not percentage.
     * @param flows This list stores individual traffic requests whose flows are utilizing this VNF-type on this node.
     *              Length and indices of the outer list should match <code>loads</code>.
     *              Can be null, if the mapping is not required.
     */
    public VnfInstances(Node node, VNF type, ArrayList<Double> loads, ArrayList<ArrayList<TrafficRequest>> flows) {
        this.node = Objects.requireNonNull(node);
        this.type = Objects.requireNonNull(type);
        this.loads = new double[Objects.requireNonNull(loads).size()];

        if (flows == null) {
            this.flows = null;
        }
        else {
            if (flows.size() != loads.size()) {
                throw new IllegalArgumentException("flows.size()=" + flows.size() + ", loads.size()=" + loads.size());
            }
            this.flows = flows.stream()
                    .map(l -> l.stream().toArray(TrafficRequest[]::new))
                    .toArray(TrafficRequest[][]::new);
        }

        for (int i = 0; i < loads.size(); i++) {
            this.loads[i] = loads.get(i);
        }
    }

    /**
     * @return load-Array normalized with the vnf's capacity
     */
    public double[] getLoadPercentage() {
        return Arrays.stream(loads).map(d -> d / type.processingCapacity).toArray();
    }

    @Override
    public String toString() {
        return "VnfInstances{"+type.name+" on "+node.name+": "+Arrays.toString(loads)+"}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        VnfInstances that = (VnfInstances) o;

        if (!node.equals(that.node)) return false;
        return type.equals(that.type);
    }

    @Override
    public int hashCode() {
        int result = node.hashCode();
        result = 31 * result + type.hashCode();
        return result;
    }

    /**
     * @return A new VnfInstance-Object with the same data.
     */
    public VnfInstances copy() {
        return new VnfInstances(node, type, Arrays.copyOf(loads, loads.length),
                flows == null ? null : Arrays.stream(flows).map(a -> Arrays.copyOf(a, a.length)).toArray(TrafficRequest[][]::new));
    }
}
