package de.lexej.VNFP.model.factory;

import de.lexej.VNFP.algo.FlowUtils;
import de.lexej.VNFP.algo.ParetoFrontier;
import de.lexej.VNFP.model.*;
import de.lexej.VNFP.model.solution.NodeAssignment;
import de.lexej.VNFP.model.solution.Solution;
import de.lexej.VNFP.model.solution.TrafficAssignment;

import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class can be used to re-import a prior Pareto frontier
 * created by this work's PSA algorithm.
 * It requires the "placementFlows" output file, next to the
 * obligatory NetworkGraph and TrafficRequests-Array.
 *
 * @author alex
 */
public class FlowPlacementReader {
    private static Pattern pLine = Pattern.compile("(\\d+);(\\d+);([^ ;,\\]\\[]+);([^ ;,\\]\\[]+);(\\d+(?:\\.\\d+)?);([^ ;,]+(?:,[^ ;,]+)*)");
    private static Pattern pRouteNode = Pattern.compile("\\[([^ ;,\\[\\]]+)\\]");

    /**
     * Reads the traffic routes from the "placementFlows" output file of the PSA algorithm.
     * The IDs of the given TrafficRequest-Array must match the flowIDs in the given CSV file.
     *
     * @param ng             NetworkGraph of the solved problem.
     * @param reqs           The solved traffic requests.
     *                       The requests' IDs must match the flowIDs in the CSV file.
     * @param placementFlows The path of the "placementFlows" CSV output file.
     * @return A ParetoFrontier object with all imported solutions.
     * @throws IOException In case of errors while parsing the file.
     */
    public static ParetoFrontier readFromCsv(NetworkGraph ng, TrafficRequest[] reqs, Path placementFlows) throws IOException {
        return readFromCsv(ng, reqs, placementFlows.toAbsolutePath().toString());
    }

    /**
     * Reads the traffic routes from the "placementFlows" output file of the PSA algorithm.
     * The IDs of the given TrafficRequest-Array must match the flowIDs in the given CSV file.
     *
     * @param ng             NetworkGraph of the solved problem.
     * @param reqs           The solved traffic requests.
     *                       The requests' IDs must match the flowIDs in the CSV file.
     * @param placementFlows The path of the "placementFlows" CSV output file.
     * @return A ParetoFrontier object with all imported solutions.
     * @throws IOException In case of errors while parsing the file.
     */
    public static ParetoFrontier readFromCsv(NetworkGraph ng, TrafficRequest[] reqs, String placementFlows) throws IOException {
        ParetoFrontier front = new ParetoFrontier();

        HashMap<Integer, TrafficRequest> idToReq = new HashMap<>();
        for (TrafficRequest req : reqs) {
            idToReq.put(req.id, req);
        }

        int lastSolution = -1;
        ArrayList<TrafficAssignment> tAssigs = new ArrayList<>();

        LineNumberReader lnr = new LineNumberReader(new FileReader(placementFlows));
        int nr = -1;
        String line;
        while ((line = lnr.readLine()) != null) {
            nr++;

            if (nr == 0) {
                if (!line.trim().toLowerCase().equals("solutionnumber;flowid;ingress;egress;delay;route")) {
                    lnr.close();
                    throw new IOException("The header must equal 'solutionNumber;flowID;ingress;egress;delay;route'");
                }
                continue;
            }

            Matcher m = pLine.matcher(line);
            if (!m.matches()) {
                lnr.close();
                throw new IOException("line " + (nr+1) + " does not match pattern " + pLine.pattern());
            }

            int solutionNumber = Integer.parseInt(m.group(1));
            int flowId = Integer.parseInt(m.group(2));
            String ingress = m.group(3);
            String egress = m.group(4);
            double delay = Double.parseDouble(m.group(5));
            String[] route = m.group(6).split(",");

            // Save last solution into Pareto frontier:
            if (lastSolution != -1 && solutionNumber != lastSolution) {
                if (tAssigs.size() != reqs.length) {
                    throw new IOException("Not enough routes in solutionNumber " + lastSolution + " given: expected="+reqs.length+", given="+tAssigs.size());
                }

                Solution s = Solution.getInstance(ng, reqs, tAssigs.toArray(new TrafficAssignment[tAssigs.size()]));
                front.add(s);
                tAssigs.clear();
            }

            TrafficRequest req = idToReq.get(flowId);
            if (req == null) {
                lnr.close();
                throw new IOException("No request found for id " + flowId + " in line " + (nr + 1));
            }
            if (!ingress.trim().toLowerCase().equals(req.ingress.name.trim().toLowerCase())) {
                lnr.close();
                throw new IOException("Ingress of request id " + flowId + " in line " + (nr + 1) + " does not match: expected="+req.ingress.name+", given="+ingress);
            }
            if (!egress.trim().toLowerCase().equals(req.egress.name.trim().toLowerCase())) {
                lnr.close();
                throw new IOException("Egress of request id " + flowId + " in line " + (nr + 1) + " does not match: expected="+req.egress.name+", given="+egress);
            }

            ArrayList<NodeAssignment> nAssigs = new ArrayList<>();
            int vnfId = -1;
            for (int i = 0; i < route.length; i++) {
                VNF vnf = null;

                Matcher m2 = pRouteNode.matcher(route[i]);
                if (m2.matches()) {
                    vnfId++;

                    if (vnfId >= req.vnfSequence.length) {
                        lnr.close();
                        throw new IOException("Too many VNF nodes in line " + (nr + 1) + ": expected="+req.vnfSequence.length+", given>="+(vnfId+1));
                    }

                    vnf = req.vnfSequence[vnfId];
                    route[i] = m2.group(1);
                }

                Node node = ng.getNodes().get(route[i]);
                if (node == null) {
                    lnr.close();
                    throw new IOException("Node '" + route[i] + "' in line " + (nr + 1) + " cannot be found in the network graph");
                }

                // Find link:
                Link prev = null;
                if (i > 0) {
                    Node lastNode = nAssigs.get(nAssigs.size() - 1).node;
                    if (!node.equals(lastNode)) {
                        prev = node.getNeighbours().stream()
                                .filter(n -> n.getOther(node).equals(lastNode))
                                .findFirst().orElse(null);
                        if (prev == null) {
                            lnr.close();
                            throw new IOException("Cannot find link for node pair (" + lastNode.name + ", " + node.name + ") in line " + (nr + 1));
                        }
                    }
                }

                nAssigs.add(new NodeAssignment(node, vnf, prev));
            }

            TrafficAssignment tAssig = new TrafficAssignment(req, nAssigs.toArray(new NodeAssignment[nAssigs.size()]), ng);
            tAssigs.add(tAssig);

            lastSolution = solutionNumber;
        }

        if (!tAssigs.isEmpty()) {
            if (tAssigs.size() != reqs.length) {
                throw new IOException("Not enough routes in solutionNumber " + lastSolution + " given: expected="+reqs.length+", given="+tAssigs.size());
            }

            Solution s = Solution.getInstance(ng, reqs, tAssigs.toArray(new TrafficAssignment[tAssigs.size()]));
            front.add(s);
        }

        lnr.close();
        return front;
    }
}
