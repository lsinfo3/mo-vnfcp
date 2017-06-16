package de.lexej.VNFP.model.solution;

import de.lexej.VNFP.model.*;
import de.lexej.VNFP.model.solution.overview.LinkOverview;
import de.lexej.VNFP.model.solution.overview.NodeOverview;
import de.lexej.VNFP.model.solution.overview.VnfTypeOverview;
import de.lexej.VNFP.util.Config;
import de.lexej.VNFP.util.Median;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static de.lexej.VNFP.model.solution.Solution.Vals.*;

/**
 * Objects of this class represent a placement that consists of {@link TrafficAssignment}s.
 *
 * @author alex
 */
public class Solution implements Comparable<Solution> {
    /**
     * The network graph with available resources.
     */
    public final NetworkGraph graph;
    /**
     * All network demands. The order of requests and assignments must match.
     */
    public final TrafficRequest[] requests;
    /**
     * Contains one TrafficAssignment for each TrafficRequest. The order of requests and assignments must match.
     */
    public final TrafficAssignment[] assignments;
    /**
     * Contains an overview for each node.
     */
    public HashMap<Node, NodeOverview> nodeMap;
    /**
     * Contains an overview for each link.
     */
    public HashMap<Link, LinkOverview> linkMap;
    /**
     * Contains an overview for each VNF.
     */
    public HashMap<VNF, VnfTypeOverview> vnfMap;

    public double creationTemperature;
    public int creationIteration;

    /**
     * This Enum lists all possible objective functions for the objective vectors.
     * All values are to be minimized.
     *
     * Prefix TOTAL indicates a sum of all respective values.
     * Prefix MEAN indicates the average of all respective values.
     * Suffix INDEX indicates the ratio (value) / (minimum possible value).
     * Infix ROOTED indicates that, before summing up the values, their square root has been taken.
     */
    public enum Vals {
        UNFEASIBLE,

        // Link resources:
        MEAN_DELAY_INDEX,
        MEDIAN_DELAY_INDEX,
        TOTAL_DELAY,
        MAX_DELAY_INDEX,
        MEAN_HOPS_INDEX,
        MEDIAN_HOPS_INDEX,
        TOTAL_NUMBER_OF_HOPS,
        MAX_HOPS_INDEX,

        // Node resources:
        TOTAL_USED_CPU,
        TOTAL_USED_RAM,
        TOTAL_USED_HDD,

        // VNF instance resources:
        MEAN_INVERSE_LOAD_INDEX,
        MEDIAN_INVERSE_LOAD_INDEX,
        NUMBER_OF_VNF_INSTANCES,
        TOTAL_ROOTED_VNF_LOADS,

        // Indicators of unfeasibility:
        // OVERLOADED refers to VNF instances on nodes with exhausted resources.
        // EXCESSIVE refers to VNF types with more instances than permitted.
        NUMBER_OF_DELAY_VIOLATIONS,
        NUMBER_OF_CPU_VIOLATIONS,
        NUMBER_OF_RAM_VIOLATIONS,
        NUMBER_OF_HDD_VIOLATIONS,
        NUMBER_OF_EXCESSIVE_VNFS,
        TOTAL_OVERLOADED_VNF_CAPACITY,
        TOTAL_ROOTED_EXCESSIVE_VNF_CAPACITY;

        public final int i = ordinal();
    }

    /**
     * This Array contains values for every possible objective function
     * defined by the <tt>Vals</tt> Enum.
     */
    public final double[] vals;

    private double[] objectiveVector;
    private double[] unfeasibleVector;

    /**
     * Creates a new Solution instance with the given content and calculates objective values for it.
     *
     * @param graph       The network graph with available resources.
     * @param requests    All network demands. The order of requests and assignments must match.
     * @param assignments Contains one TrafficAssignment for each TrafficRequest. The order of requests and assignments must match.
     * @param calcStats   True indicates that the objective vector should be calculated right away.
     */
    private Solution(NetworkGraph graph, TrafficRequest[] requests, TrafficAssignment[] assignments, boolean calcStats) {
        this.graph = Objects.requireNonNull(graph);
        this.requests = Objects.requireNonNull(requests);
        this.assignments = Objects.requireNonNull(assignments);

        vals = new double[Vals.values().length];

        // Sanity-Check:
        for (TrafficRequest req : requests) {
            if (req == null) {
                throw new NullPointerException("requests contains null");
            }
        }
        for (TrafficAssignment traffAss : assignments) {
            if (traffAss == null) {
                throw new NullPointerException("assignments contains null");
            }
        }

        // Checks whether an assignment exists for each request.
        if (requests.length != assignments.length) {
            throw new IllegalArgumentException("requests and assignments have different length");
        }
        HashSet<TrafficRequest> assignmentSet = Arrays.stream(assignments)
                .map(a -> a.request)
                .collect(Collectors.toCollection(HashSet<TrafficRequest>::new));
        for (TrafficRequest req : requests) {
            if (!assignmentSet.contains(req)) {
                throw new IllegalArgumentException("no assignment found for request '"+req+"'");
            }
        }

        if (calcStats) {
            calcStats();
        }
    }

    /**
     * Creates a new Solution instance with the given content and calculates objective values for it.
     *
     * @param graph       The network graph with available resources.
     * @param requests    All network demands. The order of requests and assignments must match.
     * @param assignments Contains one TrafficAssignment for each TrafficRequest. The order of requests and assignments must match.
     * @return A Solution object with the given content.
     */
    public static Solution getInstance(NetworkGraph graph, TrafficRequest[] requests, TrafficAssignment[] assignments) {
        return new Solution(graph, requests, assignments, true);
    }

    /**
     * Creates a new Solution-instance without requests or assignments.
     * May be used for neighbour selection algorithms in order to create entirely new solutions.
     *
     * @param graph The corresponding network, with all its nodes and links.
     * @return A new solution without flows.
     */
    public static Solution createEmptyInstance(NetworkGraph graph) {
        return new Solution(graph, new TrafficRequest[0], new TrafficAssignment[0], true);
    }

    /**
     * This method may be used to create a new Solution, removing the given assignments
     * from the current one. Statistics (VNF loads, resource demands, ...) will
     * be recalculated accordingly. May be used during neighbour selection.
     *
     * @param old            Solution to be altered.
     * @param requests       A copy of the old solution's request array, possibly in a different order.
     * @param assignments    A copy of the old solution's assignment array, in the same order as the requests.
     * @param lastValidIndex The last index of the remaining flows' arrays (TrafficRequests and TrafficAssignments).
     *                       Elements [0..lastValidIndex] will be kept, [lastValidIndex+1..length-1] will be removed.
     * @return A new Solution without the given TrafficAssignments, only containing information about the remaining flows.
     */
    public static Solution removeAssignmentsFromSolution(Solution old, TrafficRequest[] requests, TrafficAssignment[] assignments, int lastValidIndex) {
        TrafficRequest[] reqs = new TrafficRequest[lastValidIndex+1];
        TrafficAssignment[] assigs = new TrafficAssignment[lastValidIndex+1];
        System.arraycopy(requests, 0, reqs, 0, lastValidIndex+1);
        System.arraycopy(assignments, 0, assigs, 0, lastValidIndex+1);

        Solution s2 = new Solution(old.graph, reqs, assigs, false);

        // Calc the stats based on the original solution
        s2.nodeMap = new HashMap<>(old.nodeMap);
        s2.linkMap = new HashMap<>(old.linkMap);
        s2.vnfMap = new HashMap<>(old.vnfMap);
        HashMap<Object, Boolean> copied = new HashMap<>();

        for (int i = lastValidIndex+1; i < assignments.length; i++) {
            TrafficAssignment assig = assignments[i];

            for (NodeAssignment nassig : assig.path) {
                if (nassig.prev != null) {
                    LinkOverview lOv = s2.linkMap.get(nassig.prev);
                    if (copied.get(nassig.prev) == null) {
                        lOv = lOv.copy();
                        s2.linkMap.put(nassig.prev, lOv);
                        copied.put(nassig.prev, true);
                    }
                    lOv.removeRequest(nassig.traffReq);
                }
                if (nassig.vnf != null) {
                    NodeOverview nOv = s2.nodeMap.get(nassig.node);
                    if (copied.get(nassig.node) == null) {
                        nOv = nOv.copy();
                        s2.nodeMap.put(nassig.node, nOv);
                        copied.put(nassig.node, true);
                    }
                    nOv.removeAssignment(nassig);

                    VnfTypeOverview vnfOv = s2.vnfMap.get(nassig.vnf);
                    if (copied.get(nassig.vnf) == null) {
                        vnfOv = vnfOv.copy();
                        s2.vnfMap.put(nassig.vnf, vnfOv);
                        copied.put(nassig.vnf, true);
                    }
                    vnfOv.removeLocation(nassig.node);
                    vnfOv.addLocation(nassig.node, nOv.getVnfCapacities(nassig.vnf));
                }
            }
        }

        s2.checkConstraints();

        return s2;
    }

    /**
     * This method adds the given assignments to the given solution.
     * Statistics (VNF loads, resource demands, ...) will also be updated accordingly.
     *
     * @param old       Solution to be altered.
     * @param newAssigs New TrafficAssignments.
     * @return A new Solution including the given TrafficAssignments in its statistics
     */
    public static Solution addAssignmentsToSolution(Solution old, TrafficAssignment... newAssigs) {
        TrafficRequest[] reqs = new TrafficRequest[old.requests.length + newAssigs.length];
        TrafficAssignment[] assigs = new TrafficAssignment[old.assignments.length + newAssigs.length];
        System.arraycopy(old.requests, 0, reqs, 0, old.requests.length);
        System.arraycopy(old.assignments, 0, assigs, 0, old.assignments.length);

        for (int i = 0; i < newAssigs.length; i++) {
            reqs[old.requests.length + i] = newAssigs[i].request;
            assigs[old.assignments.length + i] = newAssigs[i];
        }

        Solution s2 = new Solution(old.graph, reqs, assigs, false);

        // Calc the stats based on the original solution
        s2.nodeMap = new HashMap<>(old.nodeMap);
        s2.linkMap = new HashMap<>(old.linkMap);
        s2.vnfMap = new HashMap<>(old.vnfMap);
        HashMap<Object, Boolean> copied = new HashMap<>();

        for (TrafficAssignment assig : newAssigs) {
            for (NodeAssignment nassig : assig.path) {
                if (nassig.prev != null) {
                    LinkOverview lOv = s2.linkMap.get(nassig.prev);
                    if (copied.get(nassig.prev) == null) {
                        lOv = lOv.copy();
                        s2.linkMap.put(nassig.prev, lOv);
                        copied.put(nassig.prev, true);
                    }
                    lOv.addRequest(nassig.traffReq);
                }
                if (nassig.vnf != null) {
                    NodeOverview nOv = s2.nodeMap.get(nassig.node);
                    if (copied.get(nassig.node) == null) {
                        nOv = nOv.copy();
                        s2.nodeMap.put(nassig.node, nOv);
                        copied.put(nassig.node, true);
                    }
                    nOv.addAssignment(nassig);

                    VnfTypeOverview vnfOv = s2.vnfMap.get(nassig.vnf);
                    if (copied.get(nassig.vnf) == null) {
                        vnfOv = (vnfOv == null ? new VnfTypeOverview(nassig.vnf) : vnfOv.copy());
                        s2.vnfMap.put(nassig.vnf, vnfOv);
                        copied.put(nassig.vnf, true);
                    }
                    vnfOv.removeLocation(nassig.node);
                    vnfOv.addLocation(nassig.node, nOv.getVnfCapacities(nassig.vnf));
                }
            }
        }

        s2.checkConstraints();

        return s2;
    }

    /**
     * Writes various information into {@code System.out}, including:
     * <ul>
     *     <li>Node resource overview</li>
     *     <li>VNF assignments on every node</li>
     *     <li>VNF overview</li>
     *     <li>Flows (Ingress-Egress) which traverse nodes</li>
     *     <li>Remaining bandwidth on all links</li>
     *     <li>Delay of all TrafficAssignments</li>
     * </ul>
     * Some values need to be calculated first, this method is mostly suited for debugging.
     */
    public void printDebugOutput() {
        try {
            printDebugOutput(new OutputStreamWriter(System.out));
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Writes various information into the given writer, including:
     * <ul>
     *     <li>Node resource overview</li>
     *     <li>VNF assignments on every node</li>
     *     <li>VNF overview</li>
     *     <li>Flows (Ingress-Egress) which traverse nodes</li>
     *     <li>Remaining bandwidth on all links</li>
     *     <li>Delay of all TrafficAssignments</li>
     * </ul>
     * Some values need to be calculated first, this method is mostly suited for debugging.
     *
     * @param w Destination of the output.
     */
    public void printDebugOutput(Writer w) throws IOException {
        // Nodes:
        for (NodeOverview nodeOv : nodeMap.values()) {
            if (nodeOv.remainingCpu() < 0.0 || nodeOv.remainingRam() < 0.0 || nodeOv.remainingHdd() < 0.0) {
                w.write("* ");
            }
            else {
                w.write("  ");
            }

            w.write("Node " + nodeOv.node.name +
                    " | CPU left: " + nodeOv.remainingCpu() +
                    " | RAM left: " + nodeOv.remainingRam() +
                    " | HDD left: " + nodeOv.remainingHdd() +
                    " | VNFS:");
            w.write(nodeOv.getVnfInstances().values().stream()
                    .map(v -> " " + v.type.name + "=" + v.loads.length)
                    .collect(Collectors.joining()));
            w.write(" | Flows:");
            w.write(Arrays.stream(nodeOv.getAssignments())
                    .map(n -> " "+n.traffReq.ingress.name+"-"+n.traffReq.egress.name)
                    .collect(Collectors.joining()));
            w.write("\n");
        }

        // VNFs:
        for (VnfTypeOverview vnfOv : vnfMap.values()) {
            w.write("  VNF " + vnfOv.vnf.name + " (" + vnfOv.getTotal() + " total):");
            for (Map.Entry<Node, VnfInstances> e : vnfOv.locations.entrySet()) {
                w.write(" Node{" + e.getKey().name + "}(x" + e.getValue().loads.length + ")");
            }
            w.write("\n");
        }

        // Links:
        for (LinkOverview linkOv : linkMap.values()) {
            if (linkOv.remainingBandwidth() < 0.0) w.write("* ");
            else w.write("  ");

            w.write("Link " + linkOv.link.node1 + "-" + linkOv.link.node2 + " Bandwidth left: "
                    + linkOv.remainingBandwidth());
            w.write("\n");
        }

        // Assignments:
        for (TrafficAssignment assig : assignments) {
            double delay = 0.0;
            String[] path = new String[assig.path.length];
            for (int i = 0; i < assig.path.length; i++) {
                NodeAssignment nAssig = assig.path[i];

                if (nAssig.vnf != null) delay += nAssig.vnf.delay;
                if (nAssig.prev != null) delay += nAssig.prev.delay;

                path[i] = nAssig.node.name;
                if (nAssig.vnf != null) path[i] = "(" + path[i] + ")";
            }

            if (delay > assig.request.expectedDelay) w.write("* ");
            else w.write("  ");

            w.write("delay=" + delay + " hops=" + assig.path.length + " for " + Arrays.toString(path) + "   " + assig);
            w.write("\n");
        }
    }

    /**
     * Calculates all statistics (including objectives) for later.
     */
    private void calcStats() {
        if (nodeMap == null) {
            // Create overview objects for each node and link:
            nodeMap = new HashMap<>();
            linkMap = new HashMap<>();
            vnfMap = new HashMap<>();
            for (Node n : graph.getNodes().values()) {
                nodeMap.put(n, new NodeOverview(n));
                n.getNeighbours().forEach(l -> {
                    if (!linkMap.containsKey(l)) linkMap.put(l, new LinkOverview(l));
                });
            }

            // Fill overview objects with the placement's assignments:
            for (TrafficAssignment assig : assignments) {
                for (NodeAssignment nAssig : assig.path) {
                    nodeMap.get(nAssig.node).addAssignment(nAssig);
                    if (nAssig.prev != null) {
                        linkMap.get(nAssig.prev).addRequest(assig.request);
                    }
                }
            }

            // Save VNF -> Node assignment:
            for (NodeOverview nodeOv : nodeMap.values()) {
                for (VnfInstances inst : nodeOv.getVnfInstances().values()) {
                    VnfTypeOverview vnfOv = vnfMap.get(inst.type);
                    if (vnfOv == null) {
                        vnfOv = new VnfTypeOverview(inst.type);
                        vnfMap.put(inst.type, vnfOv);
                    }
                    vnfOv.addLocation(nodeOv.node, inst);
                }
            }

            checkConstraints();
        }
    }

    /**
     * Uses previously generated overview objects and calculates objective values.
     */
    private void checkConstraints() {
        // Double[0] = Number of VNFs, Double[1] = sum(sqrt(capacity)
        HashMap<VNF, Double[]> numberOfVnfsPerType = new HashMap<>();
        ArrayList<Double> loads = new ArrayList<>();

        for (NodeOverview nodeOv : nodeMap.values()) {
            vals[TOTAL_USED_CPU.i] += nodeOv.node.cpuCapacity - nodeOv.remainingCpu();
            vals[TOTAL_USED_RAM.i] += nodeOv.node.ramCapacity - nodeOv.remainingRam();
            vals[TOTAL_USED_HDD.i] += nodeOv.node.hddCapacity - nodeOv.remainingHdd();

            // Check node resources
            if (nodeOv.remainingCpu() < 0.0) {
                vals[UNFEASIBLE.i] = 1.0;
                vals[NUMBER_OF_CPU_VIOLATIONS.i]++;
            }
            if (nodeOv.remainingRam() < 0.0) {
                vals[UNFEASIBLE.i] = 1.0;
                vals[NUMBER_OF_RAM_VIOLATIONS.i]++;
            }
            if (nodeOv.remainingHdd() < 0.0) {
                vals[UNFEASIBLE.i] = 1.0;
                vals[NUMBER_OF_HDD_VIOLATIONS.i]++;
            }

            for (VnfInstances inst : nodeOv.getVnfInstances().values()) {
                // Check capacities
                for (double d : inst.loads) {
                    vals[TOTAL_ROOTED_VNF_LOADS.i] += Math.sqrt(d);
                    if (nodeOv.remainingCpu() < 0.0 || nodeOv.remainingRam() < 0.0 || nodeOv.remainingHdd() < 0.0) {
                        vals[TOTAL_OVERLOADED_VNF_CAPACITY.i] += d;
                    }

                    loads.add(inst.type.processingCapacity / d);
                }

                // Save number & capacities of instances
                Double[] current = numberOfVnfsPerType.get(inst.type);
                if (current == null) current = new Double[]{0.0, 0.0};
                current[0] += inst.loads.length;
                current[1] += Arrays.stream(inst.loads).map(Math::sqrt).sum();
                numberOfVnfsPerType.put(inst.type, current);
            }
        }
        vals[MEAN_INVERSE_LOAD_INDEX.i] = loads.stream().mapToDouble(Double::doubleValue).sum() / loads.size();
        vals[MEDIAN_INVERSE_LOAD_INDEX.i] = Median.median(loads.stream().mapToDouble(Double::doubleValue).toArray());

        // Check number of instances
        for (Map.Entry<VNF, Double[]> e : numberOfVnfsPerType.entrySet()) {
            if (e.getKey().maxInstances > -1L && Math.round(e.getValue()[0]) > e.getKey().maxInstances) {
                vals[NUMBER_OF_EXCESSIVE_VNFS.i] += (e.getValue()[0] - e.getKey().maxInstances);
                vals[TOTAL_ROOTED_EXCESSIVE_VNF_CAPACITY.i] += e.getValue()[1];
                vals[UNFEASIBLE.i] = 1.0;
            }
            vals[NUMBER_OF_VNF_INSTANCES.i] += e.getValue()[0];
        }

        // Check link bandwidth
        for (LinkOverview linkOv : linkMap.values()) {
            if (linkOv.remainingBandwidth() < 0.0) {
                vals[UNFEASIBLE.i] = 1.0;
            }
        }

        // Check delay
        double delayIndex = 0.0;
        double hopsIndex = 0.0;
        for (TrafficAssignment assig : assignments) {
            vals[TOTAL_DELAY.i] += assig.delay;
            vals[TOTAL_NUMBER_OF_HOPS.i] += assig.numberOfHops;
            delayIndex += assig.delayIndex;
            hopsIndex += assig.hopsIndex;
            if (assig.delayIndex > vals[MAX_DELAY_INDEX.i]) vals[MAX_DELAY_INDEX.i] = assig.delayIndex;
            if (assig.hopsIndex > vals[MAX_HOPS_INDEX.i]) vals[MAX_HOPS_INDEX.i] = assig.hopsIndex;

            if (assig.delay > assig.request.expectedDelay) {
                vals[UNFEASIBLE.i] = 1.0;
                vals[NUMBER_OF_DELAY_VIOLATIONS.i]++;
            }
        }
        vals[MEAN_DELAY_INDEX.i] = delayIndex / (double) assignments.length;
        vals[MEAN_HOPS_INDEX.i] = hopsIndex / (double) assignments.length;

        vals[MEDIAN_DELAY_INDEX.i] = Median.median(Arrays.stream(assignments).mapToDouble(a -> a.delayIndex).toArray());
        vals[MEDIAN_HOPS_INDEX.i] = Median.median(Arrays.stream(assignments).mapToDouble(a -> a.hopsIndex).toArray());
    }

    /**
     * Returns a new array that equals a point in the objective space.
     *
     * @return <tt>new double[]</tt> with selected objective values.
     */
    public double[] getObjectiveVector() {
        if (objectiveVector == null) {
            objectiveVector = Config.getInstance().objectiveVector(vals);
        }
        return objectiveVector;
    }

    /**
     * Returns a new array that equals a point in the objective space.
     * Contains special objective values for unfeasible solutions.
     * If the solution is feasible, this array is supposed to contain zeros only.
     *
     * @return <tt>new double[]</tt> with selected objective values.
     */
    public double[] getUnfeasibleVector() {
        if (unfeasibleVector == null) {
            unfeasibleVector = Config.getInstance().unfeasibleVector(vals);
        }
        return unfeasibleVector;
    }

    /**
     * @return true, if this Solution is feasible; otherwise: false.
     */
    public boolean isFeasible() {
        return vals[UNFEASIBLE.i] == 0.0;
    }

    @Override
    public String toString() {
        return "Solution{" + Arrays.stream(Vals.values())
                .map(v -> v.name() + "=" + formatDouble(vals[v.i]))
                .collect(Collectors.joining(", ")) + "}";
    }

    /**
     * This method omits positions after the decimal point
     * if the first 10 digits are all 0 or 9.
     *
     * @param d Value to be formatted as String.
     * @return A String representation of d with either 0 or 3 digits after comma.
     */
    private String formatDouble(double d) {
        double r = d % 1.0;
        if (r < 0.0000000001 || r > 0.9999999999) {
            return String.format("%.0f", d);
        }
        return String.format("%.3f", d);
    }

    /**
     * Returns a CSV-formatted String representation of value-names in [0]
     * and the corresponding values in [1].
     *
     * @return A semicolon-separated value-list with header.
     */
    public String[] toStringCsv() {
        String header = Arrays.stream(Vals.values()).map(Enum::name).collect(Collectors.joining(";"))
                + ";" + IntStream.range(0, getObjectiveVector().length).mapToObj(i -> "obj"+i).collect(Collectors.joining(";"));
        String values = Arrays.stream(Vals.values()).map(v -> String.valueOf(vals[v.i])).collect(Collectors.joining(";"))
                + ";" + Arrays.stream(getObjectiveVector()).mapToObj(String::valueOf).collect(Collectors.joining(";"));
        return new String[]{header, values};
    }

    @Override
    public int compareTo(Solution o) {
        for (int i = 0; i < vals.length; i++) {
            if (vals[i] < o.vals[i]) return -1;
            if (vals[i] > o.vals[i]) return +1;
        }

        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Solution solution = (Solution) o;

        if (!Arrays.equals(getUnfeasibleVector(), solution.getUnfeasibleVector())) return false;
        if (!Arrays.equals(getObjectiveVector(), solution.getObjectiveVector())) return false;

        // Compare assignments:
        if (assignments.length != solution.assignments.length) return false;
        HashSet<TrafficAssignment> originalSet = new HashSet<>();
        Collections.addAll(originalSet, assignments);
        for (TrafficAssignment assig : solution.assignments) {
            if (!originalSet.contains(assig)) return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(assignments);
        result = 31 * result + Arrays.hashCode(getUnfeasibleVector());
        result = 31 * result + Arrays.hashCode(getObjectiveVector());
        return result;
    }
}
