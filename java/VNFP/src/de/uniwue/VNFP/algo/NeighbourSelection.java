package de.uniwue.VNFP.algo;

import de.uniwue.VNFP.model.*;
import de.uniwue.VNFP.model.solution.NodeAssignment;
import de.uniwue.VNFP.model.solution.Solution;
import de.uniwue.VNFP.model.solution.TrafficAssignment;
import de.uniwue.VNFP.model.solution.VnfInstances;
import de.uniwue.VNFP.model.solution.overview.LinkOverview;
import de.uniwue.VNFP.model.solution.overview.NodeOverview;
import de.uniwue.VNFP.util.Config;
import de.uniwue.VNFP.util.Median;
import de.uniwue.VNFP.util.ObjectWeight;
import de.uniwue.VNFP.util.ObjectWeights;

import java.util.*;
import java.util.function.IntToDoubleFunction;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static de.uniwue.VNFP.model.solution.Solution.Vals.*;

/**
 * This class provides static methods to search for new neighbours to given solutions.
 *
 * @author alex
 */
public class NeighbourSelection {
    /**
     * Creates a new Solution by replacing all flows that utilize
     * a chosen vnf instance.
     * The replaced instance is chosen randomly, weighted by the
     * cummulative delay and number of hops of all its flows.
     *
     * @param neigh        Neighbour-Solution (base of this solution & used for weights).
     * @param pNewInstance Probability for creating an additional VNF instance.
     * @param r            Object for random number generation.
     * @return A new solution with new TrafficAssignments for each flow that traversed the chosen VNF instance.
     */
    public static Solution replaceVnfInstance(Solution neigh, double pNewInstance, Random r) {
        VnfInstances draw;

        // Weight settings:
        ToDoubleFunction<NodeAssignment> weightMapper;
        boolean delayW = Config.getInstance().useDelayInWeights;
        boolean hopsW = Config.getInstance().useHopsInWeights;

        if (delayW && !hopsW) {
            weightMapper = (n -> n.traffAss.delayIndex);
        }
        else if (!delayW && hopsW) {
            weightMapper = (n -> n.traffAss.hopsIndex);
        }
        else if (delayW && hopsW) {
            weightMapper = (n -> n.traffAss.delayIndex + n.traffAss.hopsIndex);
        }
        else {
            weightMapper = (n -> 1.0);
        }

        // Unfeasible solution? -> Remove problems first.
        if (neigh.vals[NUMBER_OF_EXCESSIVE_VNFS.i] > 0.0) {
            VnfInstances[] choices = neigh.vnfMap.entrySet().stream()
                    .filter(e -> e.getKey().maxInstances > -1 && e.getValue().getTotal() > e.getKey().maxInstances)
                    .flatMap(e -> e.getValue().locations.values().stream())
                    .filter(inst -> inst.loads.length > 0)
                    .toArray(VnfInstances[]::new);

            draw = choices[r.nextInt(choices.length)];
        }
        else if (neigh.vals[NUMBER_OF_HDD_VIOLATIONS.i] > 0.0 || neigh.vals[NUMBER_OF_RAM_VIOLATIONS.i] > 0.0 || neigh.vals[NUMBER_OF_CPU_VIOLATIONS.i] > 0.0) {
            VnfInstances[] choices = neigh.nodeMap.values().stream()
                    .filter(ov -> ov.remainingHdd() < 0.0 || ov.remainingRam() < 0.0 || ov.remainingCpu() < 0.0)
                    .flatMap(ov -> ov.getVnfInstances().values().stream())
                    .filter(inst -> inst.loads.length > 0)
                    .toArray(VnfInstances[]::new);

            draw = choices[r.nextInt(choices.length)];
        }
        // Feasible. -> Remove high-delay instances.
        else {
            ArrayList<ObjectWeight<VnfInstances>> choices = new ArrayList<>();
            for (NodeOverview nodeOv : neigh.nodeMap.values()) {
                for (VnfInstances inst : nodeOv.getVnfInstances().values()) {
                    OptionalDouble w = Arrays.stream(nodeOv.getAssignments())
                            .filter(n -> n.vnf.equals(inst.type))
                            .mapToDouble(weightMapper)
                            .average();
                    if (w.isPresent()) {
                        choices.add(new ObjectWeight<>(inst, w.getAsDouble() * inst.loads.length));
                    }
                }
            }

            double choice = choices.stream().mapToDouble(inst -> inst.weight).sum() * r.nextDouble();
            int index = -1;

            while (choice >= 0.0) {
                index++;
                choice -= choices.get(index).weight;
            }

            draw = choices.get(index).content;
        }
        
        return replaceAllFlowsOfInstance(neigh, pNewInstance, draw, r);
    }

    /**
     * Creates a new Solution by replacing all flows that utilize
     * the chosen vnf instance.
     *
     * @param neigh        Neighbour-Solution (base of this solution & used for weights).
     * @param pNewInstance Probability for creating an additional VNF instance.
     * @param inst
     * @param r            Object for random number generation.
     * @return A new solution with new TrafficAssignments for each flow that traversed the chosen VNF instance.
     */
    private static Solution replaceAllFlowsOfInstance(Solution neigh, double pNewInstance, VnfInstances inst, Random r) {
        NodeOverview nodeOv = neigh.nodeMap.get(inst.node);

        TrafficRequest[] requests = Arrays.copyOf(neigh.requests, neigh.requests.length);
        TrafficAssignment[] assignments = Arrays.copyOf(neigh.assignments, neigh.assignments.length);

        // Remove random TrafficAssignments until the number of VNFs decreases:
        HashSet<TrafficAssignment> removedAssignments = new HashSet<>();
        int originalAmount = inst.loads.length;
        // Only 1 VNF of this type available, just remove all its assignments:
        if (originalAmount == 1) {
            for (NodeAssignment nAssig : nodeOv.getAssignments()) {
                if (nAssig.vnf.equals(inst.type)) {
                    removedAssignments.add(nAssig.traffAss);
                }
            }
        }
        // More than 1 VNF available; remove assignments until number actually decreases:
        else {
            NodeOverview ovCopy = nodeOv.copy();
            int currentAmount = originalAmount;
            while (originalAmount == currentAmount) {
                // Pick a random NodeAssignment:
                NodeAssignment[] nodeAssignments = Arrays.stream(ovCopy.getAssignments())
                        .filter(n -> inst.type.equals(n.vnf))
                        .toArray(NodeAssignment[]::new);
                NodeAssignment nAssig = nodeAssignments[r.nextInt(nodeAssignments.length)];

                // Remove all NodeAssignments of its TrafficAssignment from the Overview-Instance:
                TrafficAssignment tAssig = nAssig.traffAss;
                for (NodeAssignment nAssig2 : tAssig.path) {
                    if (nAssig2.node.equals(nAssig.node)) {
                        ovCopy.removeAssignment(nAssig2);
                    }
                }

                removedAssignments.add(tAssig);
                currentAmount = ovCopy.getVnfCapacities(inst.type).loads.length;
            }
        }

        // Swap all selected Requests und Assignments to the end of the arrays:
        int length = requests.length;
        int i = 0;
        while (i < length) {
            if (removedAssignments.contains(assignments[i])) {
                length--;

                TrafficAssignment tAssigTemp = assignments[i];
                assignments[i] = assignments[length];
                assignments[length] = tAssigTemp;

                TrafficRequest tReqTemp = requests[i];
                requests[i] = requests[length];
                requests[length] = tReqTemp;
            }
            else {
                i++;
            }
        }

        // Solve all removed assignments anew:
        TrafficRequest[] newReqs = new TrafficRequest[requests.length - length];
        System.arraycopy(requests, length, newReqs, 0, requests.length - length);

        Solution neigh2 = Solution.removeAssignmentsFromSolution(neigh, requests, assignments, length - 1);

        if (Config.getInstance().useWeights) return viterbiSelection(newReqs, neigh2, pNewInstance, r);
        else return randomSelection(newReqs, neigh2, r);
    }

    /**
     * Creates a new Solution by replacing one single TrafficAssignment.
     *
     * @param neigh        Neighbour-Solution (base of this solution & used for weights).
     * @param pNewInstance Probability for creating an additional VNF instance.
     * @param r            Object for random number generation.
     * @return A new solution, with 1 Flow being altered.
     */
    public static Solution replaceTrafficAssignment(Solution neigh, double pNewInstance, Random r) {
        TrafficRequest[] reqs = Arrays.copyOf(neigh.requests, neigh.requests.length);
        TrafficAssignment[] assigs = Arrays.copyOf(neigh.assignments, neigh.assignments.length);
        int draw = -1;

        // Weight settings:
        IntToDoubleFunction weightMapper;
        boolean delayW = Config.getInstance().useDelayInWeights;
        boolean hopsW = Config.getInstance().useHopsInWeights;

        if (delayW && !hopsW) {
            weightMapper = (i -> neigh.assignments[i].delayIndex);
        }
        else if (!delayW && hopsW) {
            weightMapper = (i -> neigh.assignments[i].hopsIndex);
        }
        else if (delayW && hopsW) {
            weightMapper = (i -> neigh.assignments[i].delayIndex + neigh.assignments[i].hopsIndex);
        }
        else {
            weightMapper = (n -> 1.0);
        }

        // Unfeasible solution? -> Remove problems first.
        if (neigh.vals[NUMBER_OF_DELAY_VIOLATIONS.i] > 0.0) {
            int[] choices = IntStream.range(0, assigs.length)
                    .filter(i -> assigs[i].delay > assigs[i].request.expectedDelay)
                    .toArray();
            draw = choices[r.nextInt(choices.length)];
        }
        else if (neigh.vals[NUMBER_OF_CONGESTED_LINKS.i] > 0.0) {
            HashSet<Link> congested = neigh.linkMap.values().stream().filter(l -> l.remainingBandwidth() < 0.0).map(l -> l.link).collect(Collectors.toCollection(HashSet::new));
            int[] choices = IntStream.range(0, assigs.length)
                    .filter(i -> Arrays.stream(assigs[i].path).map(p -> p.prev).anyMatch(congested::contains))
                    .toArray();
            draw = choices[r.nextInt(choices.length)];
        }
        else {
            // Define weights based on delay and number of hops:
            double[] weights = IntStream.range(0, neigh.assignments.length)
                    .mapToDouble(weightMapper)
                    .toArray();
            double choice = Arrays.stream(weights).sum() * r.nextDouble();

            while (choice >= 0.0) {
                draw++;
                choice -= weights[draw];
            }
        }

        // Swap stuff:
        TrafficRequest temp = reqs[draw];
        reqs[draw] = reqs[reqs.length - 1];
        reqs[reqs.length - 1] = temp;

        TrafficAssignment temp2 = assigs[draw];
        assigs[draw] = assigs[assigs.length - 1];
        assigs[assigs.length - 1] = temp2;

        TrafficRequest[] newReqs = new TrafficRequest[]{temp};

        Solution neigh2 = Solution.removeAssignmentsFromSolution(neigh, reqs, assigs, reqs.length - 2);

        if (Config.getInstance().useWeights) return viterbiSelection(newReqs, neigh2, pNewInstance, r);
        else return randomSelection(newReqs, neigh2, r);
    }

    /**
     * Picks VNF locations based on weights aquired by a viterbi-like algorithm.
     * Possible locations are all nodes that already have the required VNF instantiated.
     * The weight of each location is based on the number of hops and delay (50/50) of the 'best' way toward this location.
     * Alternatively, a new VNF instance is created with a given probability.
     *
     * @param reqs         All requests that should be reassigned. They must not be contained in <tt>neigh</tt>.
     * @param neigh        Neighbour-Solution (base of this solution & used for weights).
     * @param pNewInstance Probability for creating an additional VNF instance.
     * @param r            Object for random number generation.
     * @return A new solution, including the given requests.
     */
    public static Solution viterbiSelection(TrafficRequest[] reqs, Solution neigh, double pNewInstance, Random r) {
        HashMap<Node, HashMap<Node, Node.Att>> bfs = neigh.graph.getBfsBackpointers();
        HashMap<Node, HashMap<Node, Node.Att>> dijkstra = neigh.graph.getDijkstraBackpointers();

        LinkedList<VnfInstances> newInstances = new LinkedList<>();

        // Weight config:
        boolean delayW = Config.getInstance().useDelayInWeights;
        boolean hopsW = Config.getInstance().useHopsInWeights;

        // For each request...
        for (TrafficRequest req : reqs) {
            List<ObjectWeights<Node>>[] nodeWeights = new List[req.vnfSequence.length + 1];
            nodeWeights[0] = Collections.singletonList(new ObjectWeights<>(req.ingress, 0, 0));
            Node[] order = new Node[req.vnfSequence.length];
            boolean forceNewInstances = false;

            repeatPreparation: do {
                boolean createNewInstances = forceNewInstances || (r.nextDouble() <= pNewInstance / reqs.length);

                // For each VNF inside the request... prepare initial weights as delay and number of hops:
                for (int i = 0; i < req.vnfSequence.length; i++) {
                    VNF vnf = req.vnfSequence[i];
                    ArrayList<ObjectWeights<Node>> list = new ArrayList<>();
                    nodeWeights[i + 1] = list;

                    // Prepare VNF-Pair definitions:
                    VnfLib.VnfPair pair = null;
                    if (i > 0) {
                        pair = req.vnfSequence[i - 1].getPair(vnf);
                    }

                    if (createNewInstances) {
                        for (NodeOverview nodeOv : neigh.nodeMap.values()) {
                            // Are there enough ressources for the new instance?
                            if (nodeOv.node.cpuCapacity < vnf.cpuRequired || nodeOv.node.ramCapacity < vnf.ramRequired || nodeOv.node.hddCapacity < vnf.hddRequired) {
                                continue;
                            }

                            // Find the 'best' connection to the nodes of the last step:
                            addNodeWithBestConnection(nodeWeights[i], list, nodeOv.node, neigh.graph, pair, r);
                        }
                    }
                    if (list.isEmpty()) {
                        // Find all existing VNF instances of the same type...
                        for (VnfInstances inst : neigh.vnfMap.get(vnf).locations.values()) {
                            // Is there still enough room for the current request in one of the instances?
                            boolean requestFits = false;
                            for (double load : inst.loads) {
                                if (load + req.bandwidthDemand <= vnf.processingCapacity) {
                                    requestFits = true;
                                    break;
                                }
                            }
                            if (!requestFits) {
                                continue;
                            }

                            // Find the 'best' connection to the nodes of the last step:
                            addNodeWithBestConnection(nodeWeights[i], list, inst.node, neigh.graph, pair, r);
                        }
                        // No suitable vnf instances found? Create a new one.
                        if (list.isEmpty()) {
                            for (NodeOverview nodeOv : neigh.nodeMap.values()) {
                                // Are there still enough ressources for the new instance?
                                if (nodeOv.remainingCpu() < vnf.cpuRequired || nodeOv.remainingRam() < vnf.ramRequired || nodeOv.remainingHdd() < vnf.hddRequired) {
                                    continue;
                                }

                                // Find the 'best' connection to the nodes of the last step:
                                addNodeWithBestConnection(nodeWeights[i], list, nodeOv.node, neigh.graph, pair, r);
                            }
                        }
                        // No nodes with enough capacity left? Need to overload a node...
                        if (list.isEmpty()) {
                            for (NodeOverview nodeOv : neigh.nodeMap.values()) {
                                // Are there enough ressources for the new instance?
                                if (nodeOv.node.cpuCapacity < vnf.cpuRequired || nodeOv.node.ramCapacity < vnf.ramRequired || nodeOv.node.hddCapacity < vnf.hddRequired) {
                                    continue;
                                }

                                // Find the 'best' connection to the nodes of the last step:
                                addNodeWithBestConnection(nodeWeights[i], list, nodeOv.node, neigh.graph, pair, r);
                            }
                        }
                        // Still empty? Problem cannot be solved with current restrictions... anyway:
                        if (list.isEmpty()) {
                            for (NodeOverview nodeOv : neigh.nodeMap.values()) {
                                addNodeWithBestConnection(nodeWeights[i], list, nodeOv.node, neigh.graph, pair, r);
                            }
                        }
                    }
                }

                double delaySoFar = Arrays.stream(req.vnfSequence).mapToDouble(v -> v.delay).sum();
                double hopsSoFar = 0.0;
                for (int o = nodeWeights.length - 1; o >= 1; o--) {
                    // Add the delay / hops towards the next node to the weights of the current stage:
                    Node last = (o < nodeWeights.length - 1 ? order[o] : req.egress);
                    for (ObjectWeights<Node> w : nodeWeights[o]) {
                        w.w[0] += delaySoFar + dijkstra.get(w.content).get(last).d;
                        w.w[1] += hopsSoFar + bfs.get(w.content).get(last).d;
                    }

                    // Prepare values:
                    List<ObjectWeights<Node>> weights = nodeWeights[o].stream()
                            .filter(w -> w.w[0] <= req.expectedDelay)
                            .collect(Collectors.toCollection(ArrayList::new));
                    if (!createNewInstances && weights.isEmpty()) {
                        // Jump back and create a new instance where needed.
                        forceNewInstances = true;
                        continue repeatPreparation;
                    }
                    if (weights.isEmpty()) {
                        weights = nodeWeights[o];
                    }
                    double minDelay = weights.stream()
                            .mapToDouble(w -> w.w[0])
                            .filter(w -> w > 0.0)
                            .min().orElse(1.0);
                    Collections.shuffle(weights, r);

                    // Smooth out 0-values to prevent extreme cases:
                    for (ObjectWeights<Node> weight : weights) {
                        if (weight.w[0] == 0.0) {
                            weight.w[0] = minDelay / 2.0;
                        }
                        if (weight.w[1] == 0.0) {
                            weight.w[1] = 0.5;
                        }
                    }

                    double[] actualWeights;

                    // Weight settings:
                    if (delayW && !hopsW) {
                        actualWeights = weights.stream()
                                .mapToDouble(w -> 1.0 / w.w[0])
                                .toArray();
                    }
                    else if (!delayW && hopsW) {
                        actualWeights = weights.stream()
                                .mapToDouble(w -> 1.0 / w.w[1])
                                .toArray();
                    }
                    else if (delayW && hopsW) {
                        // Divide by median to normalize data:
                        double medianDelays = Median.randomizedSelect(weights, 0, (weights.size() - 1) / 2, r);
                        double medianHops = Median.randomizedSelect(weights, 1, (weights.size() - 1) / 2, r);

                        actualWeights = weights.stream()
                                .mapToDouble(w -> 1.0 / (w.w[0] / medianDelays + w.w[1] / medianHops))
                                .toArray();
                    }
                    else {
                        actualWeights = weights.stream()
                                .mapToDouble(w -> 1.0)
                                .toArray();
                    }

                    double sum = Arrays.stream(actualWeights).sum();

                    // Draw one random index:
                    double draw = sum * r.nextDouble();
                    int currentIndex = -1;
                    while (draw > 0) {
                        currentIndex++;
                        draw -= actualWeights[currentIndex];
                    }
                    order[o - 1] = weights.get(currentIndex).content;

                    delaySoFar += dijkstra.get(order[o - 1]).get(last).d;
                    hopsSoFar += bfs.get(order[o - 1]).get(last).d;
                }

                TrafficAssignment tassig = FlowUtils.fromVnfSequence(req, order, neigh.graph, neigh.graph.getRandomBackpointers(r));
                Solution neigh2 = Solution.addAssignmentsToSolution(neigh, tassig);

                // Check whether one of the chosen VNFs is a new instance:
                for (int i = 0; i < order.length; i++) {
                    VnfInstances before = neigh.nodeMap.get(order[i]).getVnfCapacities(req.vnfSequence[i]);
                    VnfInstances after = neigh2.nodeMap.get(order[i]).getVnfCapacities(req.vnfSequence[i]);
                    if (before.loads.length != after.loads.length) {
                        newInstances.add(after);
                    }
                }

                neigh = neigh2;
                break;
            } while (true); // End of silly goto hack -> repeatPreparation :)
        }

        // For each newly created VNF instance, redirect flows that benefit from it
        neigh = improveFlowsForInstance(neigh, newInstances);

        return neigh;
    }

    /**
     * This method calculates weights based on the number of hops and delay
     * on paths from every lastStep-node to the current node.
     * If certain conditions (e.g. VnfPair-restrictions) are met,
     * the current node is added to the currentStep-list with the best possible weights.
     *
     * @param lastStep    List containing all nodes (including weights) of the last VNF type.
     * @param currentStep List containing all nodes (including weights) of the current VNF type.
     *                    The current node might be added here.
     * @param node        The current node that might be added into the currentStep-list (including weights).
     * @param ng          The problem graph (used for BFS and Dijkstra pointers).
     * @param pair        VnfPair-restrictions that need to be considered (may be null).
     * @param r           Object for random number generation.
     */
    private static void addNodeWithBestConnection(List<ObjectWeights<Node>> lastStep, List<ObjectWeights<Node>> currentStep, Node node, NetworkGraph ng, VnfLib.VnfPair pair, Random r) {
        HashMap<Node, HashMap<Node, Node.Att>> bfs = ng.getBfsBackpointers();
        HashMap<Node, HashMap<Node, Node.Att>> dijkstra = ng.getDijkstraBackpointers();

        // Weights:
        boolean delayW = Config.getInstance().useDelayInWeights;
        boolean hopsW = Config.getInstance().useHopsInWeights;

        double[] delays = new double[lastStep.size()];
        double[] hops = new double[lastStep.size()];

        for (int j = 0; j < lastStep.size(); j++) {
            ObjectWeights<Node> wPrev = lastStep.get(j);
            delays[j] = wPrev.w[0] + dijkstra.get(wPrev.content).get(node).d;
            hops[j] = wPrev.w[1] + bfs.get(wPrev.content).get(node).d;
        }

        double medianDelays = 1.0;
        double medianHops = 1.0;
        if (delayW == hopsW) {
            medianDelays = Median.median(delays);
            medianHops = Median.median(hops);
            if (medianDelays == 0.0) medianDelays = 1.0;
            if (medianHops == 0.0) medianHops = 1.0;
        }

        double bestDelay = Double.POSITIVE_INFINITY;
        double bestHops = Double.POSITIVE_INFINITY;
        for (int j = 0; j < lastStep.size(); j++) {
            ObjectWeights<Node> wPrev = lastStep.get(j);
            // Don't forget VnfPair restrictions:
            if (pair == null || dijkstra.get(wPrev.content).get(node).d <= pair.latency) {
                if ((delayW == hopsW && delays[j]/medianDelays + hops[j]/medianHops < bestDelay/medianDelays + bestHops/medianHops)
                        || (hopsW && hops[j]< bestHops)
                        || (delayW && delays[j] < bestDelay)) {
                    bestDelay = delays[j];
                    bestHops = hops[j];
                }
            }
        }
        if (bestDelay < Double.POSITIVE_INFINITY && bestHops < Double.POSITIVE_INFINITY) {
            currentStep.add(new ObjectWeights<>(node, bestDelay, bestHops));
        }
    }

    /**
     * This method may be called when a new instance is created.
     * It adjusts flows by rerouting them toward this VNF if it enhances their delays / hops.
     *
     * @param solution  Solution before the reassignment.
     * @param instances Newly created VNF instances.
     * @return A new solution, possibly having several flows reassigned if it enhances the overall delay / number of hops.
     */
    private static Solution improveFlowsForInstance(Solution solution, Collection<VnfInstances> instances) {
        Objects.requireNonNull(solution);
        Objects.requireNonNull(instances);
        HashMap<Node, HashMap<Node, Node.Att>> dijkstra = solution.graph.getDijkstraBackpointers();
        HashMap<Node, HashMap<Node, Node.Att>> bfs = solution.graph.getBfsBackpointers();

        // Weights:
        boolean delayW = Config.getInstance().useDelayInWeights;
        boolean hopsW = Config.getInstance().useHopsInWeights;

        for (VnfInstances inst : instances) {
            HashSet<TrafficRequest> toRemove = new HashSet<>();
            ArrayList<TrafficAssignment> toAdd = new ArrayList<>();

            // The given instance may be outdated, more flows might have been added since its creation.
            // --> Get the current version:
            inst = solution.nodeMap.get(inst.node).getVnfCapacities(inst.type).copy();

            for (VnfInstances inst2 : solution.vnfMap.get(inst.type).locations.values()) {
                if (inst.equals(inst2)) continue;

                NodeOverview nodeOv = solution.nodeMap.get(inst2.node);
                for (NodeAssignment nassig : nodeOv.getAssignments()) {
                    TrafficAssignment tassig = nassig.traffAss;
                    TrafficRequest req = tassig.request;

                    if (!toRemove.contains(req)) {
                        LinkedList<Integer> replacedIndices = new LinkedList<>();

                        // Check for free capacities:
                        int freeIndex = -1;
                        for (int i = 0; i < inst.loads.length; i++) {
                            if (inst.loads[i] + req.bandwidthDemand <= inst.type.processingCapacity) {
                                freeIndex = i;
                                break;
                            }
                        }

                        if (freeIndex != -1) {
                            Node[] newOrder = new Node[req.vnfSequence.length];
                            int j = 0;
                            VNF type = inst.type;
                            for (int i = 0; i < tassig.path.length; i++) {
                                if (tassig.path[i].vnf != null) {
                                    if (tassig.path[i].vnf.equals(type)) {
                                        newOrder[j] = inst.node;
                                        type = null;
                                    }
                                    else {
                                        newOrder[j] = tassig.path[i].node;
                                    }
                                    j++;
                                }
                            }

                            TrafficAssignment newAssigBfs = FlowUtils.fromVnfSequence(req, newOrder, solution.graph, solution.graph.getBfsBackpointers());
                            TrafficAssignment newAssigDij = FlowUtils.fromVnfSequence(req, newOrder, solution.graph, solution.graph.getDijkstraBackpointers());

                            if (newAssigBfs.delay <= tassig.delay && newAssigBfs.numberOfHops <= tassig.numberOfHops) {
                                toRemove.add(req);
                                toAdd.add(newAssigBfs);

                                inst.loads[freeIndex] += req.bandwidthDemand;
                            }
                            else if (newAssigDij.delay <= tassig.delay && newAssigDij.numberOfHops <= tassig.numberOfHops) {
                                toRemove.add(req);
                                toAdd.add(newAssigDij);

                                inst.loads[freeIndex] += req.bandwidthDemand;
                            }
                        }
                    }
                }
            }

            TrafficRequest[] reqs = Arrays.copyOf(solution.requests, solution.requests.length);
            TrafficAssignment[] assigs = Arrays.copyOf(solution.assignments, solution.assignments.length);

            // Remove all listed assignments -> push them to the end of the array
            int lastIndex = reqs.length;
            int i = 0;
            while (i < lastIndex) {
                if (toRemove.contains(reqs[i])) {
                    lastIndex--;

                    TrafficRequest temp = reqs[i];
                    reqs[i] = reqs[lastIndex];
                    reqs[lastIndex] = temp;

                    TrafficAssignment temp2 = assigs[i];
                    assigs[i] = assigs[lastIndex];
                    assigs[lastIndex] = temp2;
                }
                else {
                    i++;
                }
            }

            solution = Solution.removeAssignmentsFromSolution(solution, reqs, assigs, lastIndex-1);
            solution = Solution.addAssignmentsToSolution(solution, toAdd.toArray(new TrafficAssignment[toAdd.size()]));
        }

        return solution;
    }

    /**
     * Picks VNF locations for the given requests by random choice (with equal probabilities).
     *
     * @param reqs         All requests that should be reassigned. They must not be contained in <tt>neigh</tt>.
     * @param neigh        Neighbour-Solution (base of this solution).
     * @param r            Object for random number generation.
     * @return A new solution, including the given requests.
     */
    public static Solution randomSelection(TrafficRequest[] reqs, Solution neigh, Random r) {
        Node[] possibleNodes = neigh.graph.getNodes().values().stream().filter(n -> n.cpuCapacity > 0.0).toArray(Node[]::new);
        if (possibleNodes.length == 0) {
            possibleNodes = neigh.graph.getNodes().values().toArray(new Node[neigh.graph.getNodes().size()]);
        }

        // For each request...
        for (TrafficRequest req : reqs) {
            // Define its VNF-order:
            Node[] order = new Node[req.vnfSequence.length];
            for (int i = 0; i < order.length; i++) {
                order[i] = possibleNodes[r.nextInt(possibleNodes.length)];
            }

            TrafficAssignment tassig = FlowUtils.fromVnfSequence(req, order, neigh.graph, neigh.graph.getRandomBackpointers(r));
            neigh = Solution.addAssignmentsToSolution(neigh, tassig);
        }

        return neigh;
    }
}
