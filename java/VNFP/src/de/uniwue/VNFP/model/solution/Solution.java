package de.uniwue.VNFP.model.solution;

import de.uniwue.VNFP.model.*;
import de.uniwue.VNFP.model.solution.overview.LinkOverview;
import de.uniwue.VNFP.model.solution.overview.NodeOverview;
import de.uniwue.VNFP.model.solution.overview.VnfTypeOverview;
import de.uniwue.VNFP.util.Config;
import de.uniwue.VNFP.util.HashWrapper;
import de.uniwue.VNFP.util.Median;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Objects of this class represent a placement that consists of {@link TrafficAssignment}s.
 *
 * @author alex
 */
public class Solution implements Comparable<Solution> {
    /**
     * The ProblemInstance for this Solution (including Network, VnfLib, Requests and possible Objectives)
     */
    public final ProblemInstance pi;
    /**
     * The network graph with available resources.
     */
    public final NetworkGraph graph;
    /**
     * The VNF type library for this problem.
     */
    public final VnfLib lib;
    /**
     * All network demands. The order of requests and assignments must match.
     */
    public final TrafficRequest[] requests;
    /**
     * The possible objectives library for this problem.
     */
    public final Objs obj;
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
    private int changed;

    /**
     * This Array contains values for every possible objective function
     * defined by {@link Objs}.
     */
    public double[] vals;

    private double[] objectiveVector;
    private double[] unfeasibleVector;

    /**
     * Creates a new Solution instance with the given content and calculates objective values for it.
     *
     * @param pi          The solved problem instance (network, vnf lib, requests)
     * @param assignments Contains one TrafficAssignment for each TrafficRequest. The order of requests and assignments must match.
     * @param calcStats   True indicates that the objective vector should be calculated right away.
     */
    private Solution(ProblemInstance pi, TrafficAssignment[] assignments, boolean calcStats) {
        this.pi = Objects.requireNonNull(pi);
        this.graph = pi.ng;
        this.requests = pi.reqs;
        this.lib = pi.vnfLib;
        this.obj = pi.objectives;

        this.assignments = Objects.requireNonNull(assignments);

        vals = new double[obj.values().length];

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
     * @param pi          The solved problem instance (network, vnf lib, requests)
     * @param assignments Contains one TrafficAssignment for each TrafficRequest. The order of requests and assignments must match.
     * @return A Solution object with the given content.
     */
    public static Solution getInstance(ProblemInstance pi, TrafficAssignment[] assignments) {
        return new Solution(pi, assignments, true);
    }

    /**
     * Creates a new Solution instance with the given content and makes it unfeasible.
     *
     * @param pi The solved problem instance (network, vnf lib, requests)
     */
    private Solution(ProblemInstance pi) {
        this.pi = Objects.requireNonNull(pi);
        this.graph = pi.ng;
        this.requests = pi.reqs;
        this.assignments = null;
        this.lib = pi.vnfLib;
        this.obj = pi.objectives;

        vals = new double[obj.values().length];
        vals[obj.UNFEASIBLE.i] = 1;
    }

    /**
     * Creates a new unfeasible dummy Solution with invalid objective values.
     *
     * @param pi The solved problem instance (network, vnf lib, requests)
     * @return An empty, unfeasible Solution object.
     */
    public static Solution getUnfeasibleInstance(ProblemInstance pi) {
        return new Solution(pi);
    }

    /**
     * Creates a new Solution-instance with the same network, but without requests or assignments.
     * May be used for neighbour selection algorithms in order to create entirely new solutions.
     *
     * @param pi The solved problem instance (network, vnf lib, requests)
     * @return A new solution without flows.
     */
    public static Solution createEmptyInstance(ProblemInstance pi) {
        return new Solution(pi.copyWith(new TrafficRequest[0]), new TrafficAssignment[0], true);
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

        Solution s2 = new Solution(old.pi.copyWith(reqs), assigs, false);

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
        s2.changed = old.changed + 1;

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

        // Prevent additive floating point arithmetic errors by frequently recalculating from scratch.
        //if (old.changed >= 0) {
        //    return new Solution(old.graph, reqs, assigs, true);
        //}

        Solution s2 = new Solution(old.pi.copyWith(reqs), assigs, false);

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
        s2.changed = old.changed + 1;

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
            if (Arrays.stream(nodeOv.remainingResources()).anyMatch(d -> d < 0.0)) {
                w.write("* ");
            }
            else {
                w.write("  ");
            }

            w.write("Node " + nodeOv.node.name +
                    " | Resources left: " + Arrays.toString(nodeOv.remainingResources()) +
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
                n.getNeighbors().forEach(l -> {
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
            for (int i = 0; i < nodeOv.node.resources.length; i++) {
                vals[obj.TOTAL_USED_RESOURCES[i].i] += nodeOv.node.resources[i] - nodeOv.remainingResources()[i];
            }

            // Check node resources
            if (Arrays.stream(nodeOv.remainingResources()).anyMatch(d -> d < 0.0)) {
                vals[obj.UNFEASIBLE.i] = 1.0;
                vals[obj.NUMBER_OF_RESOURCE_VIOLATIONS.i]++;
            }

            for (VnfInstances inst : nodeOv.getVnfInstances().values()) {
                // Check capacities
                for (double d : inst.loads) {
                    vals[obj.TOTAL_ROOTED_VNF_LOADS.i] += Math.sqrt(d);
                    if (Arrays.stream(nodeOv.remainingResources()).anyMatch(v -> v < 0.0)) {
                        vals[obj.TOTAL_OVERLOADED_VNF_CAPACITY.i] += d;
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
        vals[obj.MEAN_INVERSE_LOAD_INDEX.i] = loads.stream().mapToDouble(Double::doubleValue).sum() / loads.size();
        vals[obj.MEDIAN_INVERSE_LOAD_INDEX.i] = Median.median(loads.stream().mapToDouble(Double::doubleValue).toArray());

        // Check number of instances
        for (Map.Entry<VNF, Double[]> e : numberOfVnfsPerType.entrySet()) {
            if (e.getKey().maxInstances > -1L && Math.round(e.getValue()[0]) > e.getKey().maxInstances) {
                vals[obj.NUMBER_OF_EXCESSIVE_VNFS.i] += (e.getValue()[0] - e.getKey().maxInstances);
                vals[obj.TOTAL_ROOTED_EXCESSIVE_VNF_CAPACITY.i] += e.getValue()[1];
                vals[obj.UNFEASIBLE.i] = 1.0;
            }
            vals[obj.NUMBER_OF_VNF_INSTANCES.i] += e.getValue()[0];
        }

        // Check link bandwidth
        for (LinkOverview linkOv : linkMap.values()) {
            if (linkOv.remainingBandwidth() < 0.0) {
                vals[obj.UNFEASIBLE.i] = 1.0;
                vals[obj.NUMBER_OF_CONGESTED_LINKS.i]++;
            }
        }

        // Check delay
        double delayIndex = 0.0;
        double hopsIndex = 0.0;
        for (TrafficAssignment assig : assignments) {
            vals[obj.TOTAL_DELAY.i] += assig.delay;
            vals[obj.NUMBER_OF_HOPS.i] += assig.numberOfHops;
            delayIndex += assig.delayIndex;
            hopsIndex += assig.hopsIndex;
            if (assig.delayIndex > vals[obj.MAX_DELAY_INDEX.i]) vals[obj.MAX_DELAY_INDEX.i] = assig.delayIndex;
            if (assig.hopsIndex > vals[obj.MAX_HOPS_INDEX.i]) vals[obj.MAX_HOPS_INDEX.i] = assig.hopsIndex;

            if (assig.delay > assig.request.expectedDelay) {
                vals[obj.UNFEASIBLE.i] = 1.0;
                vals[obj.NUMBER_OF_DELAY_VIOLATIONS.i]++;
            }
        }
        vals[obj.MEAN_DELAY_INDEX.i] = delayIndex / (double) assignments.length;
        vals[obj.MEAN_HOPS_INDEX.i] = hopsIndex / (double) assignments.length;

        vals[obj.MEDIAN_DELAY_INDEX.i] = Median.median(Arrays.stream(assignments).mapToDouble(a -> a.delayIndex).toArray());
        vals[obj.MEDIAN_HOPS_INDEX.i] = Median.median(Arrays.stream(assignments).mapToDouble(a -> a.hopsIndex).toArray());

		if (pi.initialSolutions != null) {
			// Number of VNF Replacements
			vals[obj.NUMBER_OF_VNF_REPLACEMENTS.i] = -1;
			for (Solution s : pi.initialSolutions) {
				double replacements = 0;
				for (Node n : nodeMap.keySet()) {
					for (VNF v : vnfMap.keySet()) {
						VnfInstances inst1 = nodeMap.get(n).getVnfCapacities(v);
						VnfInstances inst2 = s.nodeMap.get(n).getVnfCapacities(v);

						replacements += Math.abs(inst1.loads.length - inst2.loads.length);
					}
				}
				if (vals[obj.NUMBER_OF_VNF_REPLACEMENTS.i] == -1 || vals[obj.NUMBER_OF_VNF_REPLACEMENTS.i] > replacements) {
					vals[obj.NUMBER_OF_VNF_REPLACEMENTS.i] = replacements;
				}
			}

			// Prepare assignments map
			HashMap<HashWrapper, TrafficAssignment> assigMap = new HashMap<>();
			for (TrafficAssignment assig : assignments) {
				assigMap.put(new HashWrapper(assig.request), assig);
			}

			// Flow Migration Penalty
			vals[obj.TOTAL_FLOW_MIGRATION_PENALTY.i] = -1;
			for (Solution s : pi.initialSolutions) {
				double penalty = 0;
				for (TrafficAssignment assig : s.assignments) {
					TrafficAssignment assig2 = assigMap.get(new HashWrapper(assig.request));
					if (assig2 != null) {
						Node[] assigPath = Arrays.stream(assig.path).filter(n -> n.vnf != null).map(n -> n.node).toArray(Node[]::new);
						Node[] assig2Path = Arrays.stream(assig2.path).filter(n -> n.vnf != null).map(n -> n.node).toArray(Node[]::new);
						for (int i = 0; i < assigPath.length; i++) {
							if (!assigPath[i].equals(assig2Path[i])) {
								penalty += assig.request.vnfSequence[i].flowMigrationPenalty;
							}
						}
					}
				}
				if (vals[obj.TOTAL_FLOW_MIGRATION_PENALTY.i] == -1 || vals[obj.TOTAL_FLOW_MIGRATION_PENALTY.i] > penalty) {
					vals[obj.TOTAL_FLOW_MIGRATION_PENALTY.i] = penalty;
				}
			}
		}
    }

    /**
     * Returns a new array that equals a point in the objective space.
     *
     * @return <tt>new double[]</tt> with selected objective values.
     */
    public double[] getObjectiveVector() {
        if (objectiveVector == null) {
            objectiveVector = Config.getInstance().objectiveVector(this);
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
        return vals[obj.UNFEASIBLE.i] == 0.0;
    }

    @Override
    public String toString() {
        return "Solution{" + Arrays.stream(obj.values())
                .map(v -> v.toString().replace(" ", "_") + "=" + formatDouble(vals[v.i]))
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
        String header = Arrays.stream(obj.values()).map(o -> o.toString().replace(" ", "_")).collect(Collectors.joining(";"))
                + ";" + IntStream.range(0, getObjectiveVector().length).mapToObj(i -> "obj"+i).collect(Collectors.joining(";"));
        String values = Arrays.stream(obj.values()).map(v -> String.valueOf(vals[v.i])).collect(Collectors.joining(";"))
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
