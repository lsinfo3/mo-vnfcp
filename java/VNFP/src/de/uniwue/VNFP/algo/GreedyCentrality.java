package de.uniwue.VNFP.algo;

import de.uniwue.VNFP.model.*;
import de.uniwue.VNFP.model.solution.NodeAssignment;
import de.uniwue.VNFP.model.solution.Solution;
import de.uniwue.VNFP.model.solution.TrafficAssignment;
import de.uniwue.VNFP.model.solution.VnfInstances;
import de.uniwue.VNFP.model.solution.overview.NodeOverview;
import de.uniwue.VNFP.util.ObjectWeight;

import java.util.*;
import java.util.stream.IntStream;

/**
 * This class contains static methods to create a (possibly unfeasible) solution
 * by greedily creating VNF instances until all requests are satisfied.
 * This heuristic aims to minimize the number of VNF instances, thereby it may
 * assign flows such that their delay-constraint is not fulfilled.
 * <p/>
 * <b>Note:</b> Maximum-Latency-Pairs in vnfLibs do not work well with this heuristic
 * and might increase CPU usage.
 *
 * @author alex
 */
public class GreedyCentrality {
    /**
     * Calculates a centrality index for each node and each vnf type
     * that is based on the number of flows whose shortest paths
     * traverse these nodes. Assigns VNF instances to the highest
     * centrality's node and fills it with flows until
     * all requests are satisfied.
     * <p/>
     * <b>Note:</b> Maximum-Latency-Pairs in vnfLibs do not work well with this heuristic
     * and might increase CPU usage.
     *
     * @param ng   The problem's (physical) network graph.
     * @param lib  The VNF types library for this problem.
     * @param reqs All flow demands.
     * @return A (possibly unfeasible) Solution for the problem with minimum number of instances.
     */
    public static Solution centrality(NetworkGraph ng, VnfLib lib, TrafficRequest[] reqs) {
        Objects.requireNonNull(ng);
        Objects.requireNonNull(reqs);
        HashMap<Node, HashMap<Node, Node.Att>> bp = ng.getDijkstraBackpointers();
        VnfLib vnfLib = null;
        Node[] cpuNodes = ng.getNodes().values().stream().filter(n -> n.resources[0] > 0).toArray(Node[]::new);

        // Create dummy node to compute the number of required instances for each type:
        double[] res = new double[lib.res.length];
        for (int i = 0; i < res.length; i++) {
            res[i] = Double.POSITIVE_INFINITY;
        }
        Node dummy = new Node("dummy", res);
        NodeOverview dummyOv = new NodeOverview(dummy);
        for (TrafficRequest req : reqs) {
            for (VNF vnf : req.vnfSequence) {
                NodeAssignment nAssig = new NodeAssignment(dummy, vnf, null);
                nAssig.traffReq = req;
                dummyOv.addAssignment(nAssig);
                if (vnfLib == null) vnfLib = vnf.vnfLib;
            }
        }
        if (vnfLib == null) {
            throw new IllegalArgumentException("no traffic request with VNF demands found");
        }
        HashMap<VNF, VnfInstances> numOfInstances = dummyOv.getVnfInstances();

        // Count the number of traversing shortest paths for each node and vnf:
        HashMap<VNF, HashMap<ObjectWeight<Node>, ObjectWeight<Node>>> nodeWeightSets = new HashMap<>();
        for (VNF vnf : vnfLib.getAllVnfs()) {
            nodeWeightSets.put(vnf, new HashMap<>());
        }

        HashMap<VNF, HashSet<TrafficAssignment>> shortestFlows = new HashMap<>();
        for (VNF vnf : vnfLib.getAllVnfs()) {
            shortestFlows.put(vnf, new HashSet<>());
        }
        for (int i = 0; i < reqs.length; i++) {
            TrafficRequest req = reqs[i];

            // Find the shortest possible path with minimum 1 node with CPU resources:
            Node middle = (req.vnfSequence.length == 0 ? req.ingress : ng.getShortestMiddleStation(req.ingress, req.egress, cpuNodes, bp));

            Node[] order = new Node[req.vnfSequence.length];
            Arrays.fill(order, middle);
            TrafficAssignment tAssig = FlowUtils.fromVnfSequence(req, order, ng, bp);

            // Mark all CPU-nodes on the way with +1 for each requested VNF:
            for (VNF vnf : req.vnfSequence) {
                HashMap<ObjectWeight<Node>, ObjectWeight<Node>> thisMap = nodeWeightSets.get(vnf);
                for (NodeAssignment nAssig : tAssig.path) {
                    if (nAssig.node.resources[0] > 0.0) {
                        ObjectWeight<Node> thisNode = new ObjectWeight<>(nAssig.node, 0.0);
                        if (thisMap.containsKey(thisNode)) {
                            thisNode = thisMap.get(thisNode);
                        }
                        else {
                            thisMap.put(thisNode, thisNode);
                        }
                        thisNode.weight += 1.0;
                    }
                }

                shortestFlows.get(vnf).add(tAssig);
            }
        }

        // Used resources: [0]=cpu, [1]=ram, [2]=hdd
        HashMap<Node, double[]> usedResources = new HashMap<>();
        for (Node n : ng.getNodes().values()) {
            usedResources.put(n, new double[3]);
        }

        // Distribute VNF Instances:
        HashMap<VNF, ArrayList<VnfInstances>> locations = new HashMap<>();
        for (VNF vnf : vnfLib.getAllVnfs()) {
            HashMap<ObjectWeight<Node>, ObjectWeight<Node>> weights = nodeWeightSets.get(vnf);
            ArrayList<VnfInstances> locationList = new ArrayList<>();
            locations.put(vnf, locationList);

            VnfInstances dummyInst = numOfInstances.get(vnf);
            if (dummyInst == null) continue;
            for (int i = 0; i < dummyInst.loads.length; i++) {
                Optional<ObjectWeight<Node>> newLocOpt = weights.values().stream()
                        .filter(v -> {
                            double[] used = usedResources.get(v.content);
                            for (int j = 0; j < used.length; j++) {
                                if (used[j] + vnf.reqResources[j] > v.content.resources[j]) {
                                    return false;
                                }
                            }
                            return true;
                        })
                        .max(Comparator.comparingDouble(o1 -> o1.weight));

                // If Optional is empty, do not filter:
                if (!newLocOpt.isPresent()) {
                    newLocOpt = weights.values().stream()
                            .max(Comparator.comparingDouble(o -> o.weight));
                }
                ObjectWeight<Node> newLoc = newLocOpt.get();

                // Mark resources as used:
                double[] used = usedResources.get(newLoc.content);
                for (int j = 0; j < used.length; j++) {
                    used[j] += vnf.reqResources[j];
                }

                VnfInstances inst = new VnfInstances(newLoc.content, vnf, new double[1], null);
                locationList.add(inst);

                // Find all flows whose shortest paths traverse this node, remove their weight from all nodes in their path:
                double bandwidthSoFar = 0.0;
                for (Iterator<TrafficAssignment> it = shortestFlows.get(vnf).iterator(); it.hasNext();) {
                    TrafficAssignment tAssig = it.next();

                    bandwidthSoFar += tAssig.request.bandwidthDemand;
                    if (bandwidthSoFar > vnf.processingCapacity) {
                        break;
                    }

                    if (Arrays.stream(tAssig.path).anyMatch(n -> n.node.equals(newLoc.content))) {
                        IntStream.range(0, tAssig.path.length)
                                .filter(n -> tAssig.path[n].node.resources[0] > 0.0)
                                .mapToObj(n -> weights.get(new ObjectWeight<>(tAssig.path[n].node, 0.0)))
                                .forEach(w -> w.weight--);
                        it.remove();
                    }
                }
            }
        }

        // Create the actual TrafficAssignments based on the now known instance locations:
        TrafficAssignment[] assigs = new TrafficAssignment[reqs.length];
        for (int i = 0; i < reqs.length; i++) {
            TrafficRequest req = reqs[i];
            Node[] order = new Node[req.vnfSequence.length];

            // Create new graph for this request:
            NetworkGraph ng2 = new NetworkGraph(true);
            Node ingress = ng2.addNode("ingress", req.ingress.resources);
            Node egress = ng2.addNode("egress", req.egress.resources);
            LinkedList<Node> lastIteration = new LinkedList<>();
            lastIteration.add(ingress);

            for (int o = 0; o < order.length; o++) {
                VNF currentVNF = req.vnfSequence[o];
                ArrayList<VnfInstances> currentLocations = locations.get(currentVNF);
                LinkedList<Node> currentIteration = new LinkedList<>();

                // Are there Maximum-Latency-Pair definitions for the current and the previous function?
                VnfLib.VnfPair pair = null;
                if (o > 0) {
                    VNF prevVNF = req.vnfSequence[o-1];
                    pair = vnfLib.getPair(prevVNF, currentVNF);
                }

                for (int j = 0; j < currentLocations.size(); j++) {
                    VnfInstances v = currentLocations.get(j);
                    if (v.loads[0] + req.bandwidthDemand <= v.type.processingCapacity) {
                        currentIteration.add(ng2.addNode(o + "_" + j + "_" + v.node.name, v.node.resources));
                    }
                }

                if (currentIteration.isEmpty()) {
                    // Bad luck at solving the bin-packing problem "by chance" -> take all still available nodes into account
                    for (Node n : ng.getNodes().values()) {
                        double[] used = usedResources.get(n);
                        boolean itFits = true;
                        for (int j = 0; j < used.length; j++) {
                            if (used[j] + currentVNF.reqResources[j] > n.resources[j]) {
                                itFits = false;
                                break;
                            }
                        }
                        if (itFits) {
                            currentIteration.add(ng2.addNode("x_" + o + "_" + n.name, n.resources));
                        }
                    }
                }

                for (int repetition = 0; repetition < 2; repetition++) {
                    // If still no suitable location was found, ignore capacity constraints.
                    if (currentIteration.isEmpty()) {
                        for (Node n : ng.getNodes().values()) {
                            if (n.resources[0] > 0.0) {
                                currentIteration.add(ng2.addNode("x_" + o + "_" + n.name, n.resources));
                            }
                        }
                    }

                    if (currentIteration.isEmpty()) {
                        throw new IllegalArgumentException("Unable to place VNF type " + currentVNF.name + " on any node.");
                    }

                    // Connect all nodes from the lastIteration with the currentIteration
                    HashSet<Node> currentIterationAfterFilter = new HashSet<>();
                    for (Node n1 : lastIteration) {
                        for (Node n2 : currentIteration) {
                            Node _n1 = (n1.equals(ingress) ? req.ingress : ng.getNodes().get(n1.name.split("_", 3)[2]));
                            Node _n2 = ng.getNodes().get(n2.name.split("_", 3)[2]);

                            double latency = bp.get(_n1).get(_n2).d;
                            if (pair == null || latency <= pair.latency) {
                                ng2.addLink(n1, n2, Double.POSITIVE_INFINITY, latency);
                                currentIterationAfterFilter.add(n2);
                            }
                        }
                    }

                    if (currentIterationAfterFilter.isEmpty()) {
                        // Repeat with all nodes.
                        if (repetition == 0) {
                            currentIteration = new LinkedList<>();
                        }
                        else {
                            throw new IllegalArgumentException("unable to compute valid path for request " + req);
                        }
                    }
                    else {
                        currentIteration = new LinkedList<>(currentIterationAfterFilter);
                        break;
                    }
                }

                lastIteration = currentIteration;
            }

            // Connect lastIteration to egress:
            for (Node n1 : lastIteration) {
                Node _n1 = (order.length == 0 ? req.ingress : ng.getNodes().get(n1.name.split("_", 3)[2]));
                ng2.addLink(n1, egress, Double.POSITIVE_INFINITY, bp.get(_n1).get(req.egress).d);
            }

            // Get shortest path through the newly created graph:
            ArrayList<NodeAssignment> path = FlowUtils.createPath(ingress, egress, ng2.getDijkstraBackpointers());
            if (path.size() != order.length + 2) {
                throw new IllegalArgumentException("path length mismatch: path.size()=" + path.size() + ", order.length=" + order.length);
            }

            // Copy chosen nodes into order-Array for the actual path through the real network:
            for (int o = 0; o < order.length; o++) {
                Node n = ng.getNodes().get(path.get(o+1).node.name.split("_", 3)[2]);
                order[o] = n;
                VnfInstances v;

                // Has an existing VnfInstance been chosen...
                if (!path.get(o+1).node.name.startsWith("x")) {
                    v = locations.get(req.vnfSequence[o]).get(Integer.parseInt(path.get(o+1).node.name.split("_", 3)[1]));
                }
                // ... or should a new Instance be created?
                else {
                    v = new VnfInstances(n, req.vnfSequence[o], new double[1], null);
                    locations.get(req.vnfSequence[o]).add(v);

                    double[] used = usedResources.get(n);
                    for (int j = 0; j < used.length; j++) {
                        used[j] += v.type.reqResources[j];
                    }
                }

                v.loads[0] += req.bandwidthDemand;
            }

            assigs[i] = FlowUtils.fromVnfSequence(req, order, ng, bp);
        }

        return Solution.getInstance(new ProblemInstance(ng, lib, reqs, new Objs(lib.getResources())), assigs);
    }
}
