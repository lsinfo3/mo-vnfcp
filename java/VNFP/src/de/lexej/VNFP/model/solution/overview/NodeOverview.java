package de.lexej.VNFP.model.solution.overview;

import de.lexej.VNFP.model.Node;
import de.lexej.VNFP.model.TrafficRequest;
import de.lexej.VNFP.model.VNF;
import de.lexej.VNFP.model.solution.NodeAssignment;
import de.lexej.VNFP.model.solution.TrafficAssignment;
import de.lexej.VNFP.model.solution.VnfInstances;

import java.util.*;

/**
 * This class stores placement information for nodes, which can be added
 * with the {@code addAssignment()} method.
 *
 * @author alex
 */
public class NodeOverview {
    /**
     * The corresponding node.
     */
    public final Node node;

    private HashSet<NodeAssignment> assignments;
    private HashMap<VNF, VnfInstances> vnfInstances = null;
    private double remainingCpu;
    private double remainingRam;
    private double remainingHdd;

    /**
     * Creates a new instance.
     *
     * @param node The corresponding node.
     */
    public NodeOverview(Node node) {
        this.node = Objects.requireNonNull(node);
        this.assignments = new HashSet<>();
    }

    /**
     * Adds a new a {@code (Node, VNF)} assignment.
     * (NodeAssignments can be acquired from {@link TrafficAssignment} objects.)
     *
     * @param assignment NodeAssignment, representing a {@code (Node, VNF)} tuple
     */
    public void addAssignment(NodeAssignment assignment) {
        if (!node.equals(assignment.node)) {
            throw new IllegalArgumentException("assignment-node " + assignment.node + " does not match node " + node);
        }
        if (assignment.vnf != null) {
            if (assignments.add(Objects.requireNonNull(assignment))) {
                vnfInstances = null;
            }
        }
    }

    /**
     * Removes an assignment from this object, if present.
     *
     * @param assignment NodeAssignment, representing a {@code (Node, VNF)} tuple
     */
    public void removeAssignment(NodeAssignment assignment) {
        if (!node.equals(assignment.node)) {
            throw new IllegalArgumentException("assignment-node " + assignment.node + " does not match node " + node);
        }
        if (assignment.vnf != null) {
            if (assignments.remove(Objects.requireNonNull(assignment))) {
                vnfInstances = null;
            }
        }
    }

    /**
     * Subtracts the used CPU cores from the available resources and returns the difference.
     * Can be negative, which indicates that constraints are not satisfied.
     *
     * @return Remaining CPU resources for this node.
     */
    public double remainingCpu() {
        if (vnfInstances == null) {
            // This method also calculates remainingCpu:
            getVnfInstances();
        }
        return remainingCpu;
    }

    /**
     * Subtracts the used RAM cores from the available resources and returns the difference.
     * Can be negative, which indicates that constraints are not satisfied.
     *
     * @return Remaining RAM resources for this node.
     */
    public double remainingRam() {
        if (vnfInstances == null) {
            // This method also calculates remainingCpu:
            getVnfInstances();
        }
        return remainingRam;
    }

    /**
     * Subtracts the used HDD cores from the available resources and returns the difference.
     * Can be negative, which indicates that constraints are not satisfied.
     *
     * @return Remaining HDD resources for this node.
     */
    public double remainingHdd() {
        if (vnfInstances == null) {
            // This method also calculates remainingCpu:
            getVnfInstances();
        }
        return remainingHdd;
    }

    /**
     * Returns all previously added NodeAssignments that apply a NF on this node.
     *
     * @return An array containing all added NodeAssignments with <tt>assig.vnf != null</tt>.
     */
    public NodeAssignment[] getAssignments() {
        return assignments.toArray(new NodeAssignment[assignments.size()]);
    }

    /**
     * Returns the corresponding VnfInstances object to the given VNF.
     *
     * @param vnf VNF type of interest.
     * @return VnfInstances object with used capacities.
     */
    public VnfInstances getVnfCapacities(VNF vnf) {
        getVnfInstances();
        VnfInstances vnfInst = vnfInstances.get(vnf);
        return (vnfInst != null ? vnfInst : new VnfInstances(node, vnf, new double[0], new TrafficRequest[0][]));
    }

    /**
     * This method solves the bin packing problem by means of a first fit approximation
     * to assess the number of required VNF instances for all assigned demands.
     *
     * @return A map containing workloads for instances.
     */
    public HashMap<VNF, VnfInstances> getVnfInstances() {
        if (vnfInstances != null) {
            return vnfInstances;
        }
        vnfInstances = new HashMap<>();

        // Gather requests for different instance types:
        HashMap<VNF, ArrayList<TrafficRequest>> allRequests = new HashMap<>();
        for (NodeAssignment assignment : assignments) {
            if (assignment.vnf != null) {
                ArrayList<TrafficRequest> requestList = allRequests.get(assignment.vnf);
                if (requestList == null) {
                    requestList = new ArrayList<>();
                    allRequests.put(assignment.vnf, requestList);
                }
                requestList.add(assignment.traffReq);
            }
        }

        // Solve bin packing for each VNF type:
        for (Map.Entry<VNF, ArrayList<TrafficRequest>> entry : allRequests.entrySet()) {
            double binSize = entry.getKey().processingCapacity;
            ArrayList<Double> bins = new ArrayList<>();
            ArrayList<ArrayList<TrafficRequest>> mappedRequests = new ArrayList<>();
            // Sort bandwidth demands (desc):
            entry.getValue().sort(Comparator.reverseOrder());

            for (TrafficRequest req : entry.getValue()) {
                boolean platzGefunden = false;
                int aktuellerBin = 0;
                while (!platzGefunden) {
                    // Current bin does not exist? -> Create new one, add request:
                    if (aktuellerBin == bins.size()) {
                        bins.add(req.bandwidthDemand);

                        ArrayList<TrafficRequest> l = new ArrayList<>();
                        l.add(req);
                        mappedRequests.add(l);

                        platzGefunden = true;
                    }
                    // Request fits into current bin:
                    else if (bins.get(aktuellerBin) + req.bandwidthDemand <= binSize) {
                        bins.set(aktuellerBin, bins.get(aktuellerBin) + req.bandwidthDemand);
                        mappedRequests.get(aktuellerBin).add(req);
                        platzGefunden = true;
                    }
                    // Request does not fit, but more bins exist:
                    else {
                        aktuellerBin++;
                    }
                }
            }
            // bins.size() equals the number of required instances.
            vnfInstances.put(entry.getKey(), new VnfInstances(node, entry.getKey(), bins, mappedRequests));
        }
        // Also set remainingCpu & friends:
        remainingCpu = node.cpuCapacity - vnfInstances.values().stream().mapToDouble(e -> e.loads.length * e.type.cpuRequired).sum();
        remainingRam = node.ramCapacity - vnfInstances.values().stream().mapToDouble(e -> e.loads.length * e.type.ramRequired).sum();
        remainingHdd = node.hddCapacity - vnfInstances.values().stream().mapToDouble(e -> e.loads.length * e.type.hddRequired).sum();
        return vnfInstances;
    }

    /**
     * Creates a copy of this object & contents.
     *
     * @return A new NodeOverview object with the same contents.
     */
    public NodeOverview copy() {
        NodeOverview nOver = new NodeOverview(node);

        nOver.assignments.addAll(assignments);
        if (vnfInstances != null) {
            nOver.vnfInstances = new HashMap<>(vnfInstances);
        }
        nOver.remainingCpu = remainingCpu;
        nOver.remainingRam = remainingRam;
        nOver.remainingHdd = remainingHdd;

        return nOver;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NodeOverview that = (NodeOverview) o;

        return node.equals(that.node);
    }

    @Override
    public int hashCode() {
        return node.hashCode();
    }
}