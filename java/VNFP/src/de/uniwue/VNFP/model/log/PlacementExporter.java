package de.uniwue.VNFP.model.log;

import de.uniwue.VNFP.algo.ParetoFrontier;
import de.uniwue.VNFP.model.TrafficRequest;
import de.uniwue.VNFP.model.solution.NodeAssignment;
import de.uniwue.VNFP.model.solution.Solution;
import de.uniwue.VNFP.model.solution.TrafficAssignment;
import de.uniwue.VNFP.model.solution.VnfInstances;
import de.uniwue.VNFP.model.solution.overview.LinkOverview;
import de.uniwue.VNFP.model.solution.overview.NodeOverview;
import de.uniwue.VNFP.model.solution.overview.VnfTypeOverview;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Epic class for epic things.
 * (This generic message was probably left here because
 * this file's name is self-explanatory.)
 *
 * @author alex
 */
public class PlacementExporter implements PSAEventLogger {
    private Writer nodeOvW;
    private Writer linkOvW;
    private Writer vnfOvW;
    private Writer requestOvW;

    /**
     * Initializes a new instance of this logger.
     * All output will be written into the respective writers.
     * They will be closed afterwards.
     * Some of the arguments may be null, at least one argument must be present.
     *
     * @param nodeOv Writer for node-related info.
     * @param linkOv Writer for node-related info.
     * @param vnfOv Writer for node-related info.
     * @param requestOv Writer for node-related info.
     *
     */
    public PlacementExporter(Writer nodeOv, Writer linkOv, Writer vnfOv, Writer requestOv) {
        if (nodeOv == null && linkOv == null && vnfOv == null && requestOv == null) {
            throw new NullPointerException("all given Writers are null");
        }

        this.nodeOvW = nodeOv;
        this.linkOvW = linkOv;
        this.vnfOvW = vnfOv;
        this.requestOvW = requestOv;
    }

    @Override
    public void psaEnd(ParetoFrontier paretoFrontier) {
        try {
            // Nodes
            if (nodeOvW != null) {
                nodeOvW.write("solutionNumber;nodeName;usedResources;remainingResources;vnfList");

                for (int i = 0; i < paretoFrontier.size(); i++) {
                    Solution s = paretoFrontier.get(i);

                    for (NodeOverview nodeOv : s.nodeMap.values()) {
                        nodeOvW.write(String.format("\n%d;%s;%s;%s;",
                                i, nodeOv.node.name,
                                IntStream.range(0, nodeOv.node.resources.length).mapToObj(d -> "" + (nodeOv.node.resources[d] - nodeOv.remainingResources()[d])).collect(Collectors.joining(",")),
                                Arrays.stream(nodeOv.remainingResources()).mapToObj(d -> ""+d).collect(Collectors.joining(","))));

                        String sep = "";
                        for (VnfInstances v : nodeOv.getVnfInstances().values()) {
                            double[] loads = v.getLoadPercentage();
                            for (int j = 0; j < v.loads.length; j++) {
                                nodeOvW.write(sep);
                                nodeOvW.write(String.format("%s[%s][%d]=%.2f",
                                        v.type.name,
                                        nodeOv.node.name,
                                        j,
                                        loads[j]));
                                sep = ",";
                            }
                        }
                    }
                }

                nodeOvW.write("\n");
                nodeOvW.close();
            }

            // Links
            if (linkOvW != null) {
                linkOvW.write("solutionNumber;linkNode1;linkNode2;usedBandwidth;remainingBandwidth;flowList");

                for (int i = 0; i < paretoFrontier.size(); i++) {
                    Solution s = paretoFrontier.get(i);

                    for (LinkOverview linkOv : s.linkMap.values()) {
                        linkOvW.write(String.format("\n%d;%s;%s;%.2f;%.2f;",
                                i, linkOv.link.node1.name, linkOv.link.node2.name,
                                linkOv.link.bandwidth - linkOv.remainingBandwidth(),
                                linkOv.remainingBandwidth()));

                        String sep = "";
                        for (Map.Entry<TrafficRequest,Integer> e : linkOv.requests.entrySet()) {
                            for (int j = 0; j < e.getValue(); j++) {
                                linkOvW.write(sep);
                                linkOvW.write("" + e.getKey().id);
                                sep = ",";
                            }
                        }
                    }
                }

                linkOvW.write("\n");
                linkOvW.close();
            }

            // Vnfs
            if (vnfOvW != null) {
                vnfOvW.write("solutionNumber;vnfType[node][id];load;usedCapacity;remainingCapacity;fullCapacity;flowList");

                for (int i = 0; i < paretoFrontier.size(); i++) {
                    Solution s = paretoFrontier.get(i);

                    for (VnfTypeOverview vnfOv : s.vnfMap.values()) {
                        for (VnfInstances inst : vnfOv.locations.values()) {
                            double[] loads = inst.getLoadPercentage();

                            for (int j = 0; j < inst.loads.length; j++) {
                                TrafficRequest[] reqs = inst.flows[j];

                                vnfOvW.write(String.format("\n%d;%s[%s][%d];%.2f;%.2f;%.2f;%.2f;",
                                        i, inst.type.name, inst.node.name, j,
                                        loads[j],
                                        inst.loads[j],
                                        inst.type.processingCapacity - inst.loads[j],
                                        inst.type.processingCapacity));

                                String sep = "";
                                for (TrafficRequest req : reqs) {
                                    vnfOvW.write(sep);
                                    vnfOvW.write("" + req.id);
                                    sep = ",";
                                }
                            }
                        }
                    }
                }

                vnfOvW.write("\n");
                vnfOvW.close();
            }

            // Flows
            if (requestOvW != null) {
                requestOvW.write("solutionNumber;flowID;ingress;egress;delay;vnfs;route");

                for (int i = 0; i < paretoFrontier.size(); i++) {
                    Solution s = paretoFrontier.get(i);

                    for (TrafficAssignment assig : s.assignments) {
                        requestOvW.write(String.format("\n%d;%d;%s;%s;%.2f;",
                                i, assig.request.id,
                                assig.request.ingress.name, assig.request.egress.name,
                                assig.delay));

                        // vnfs
                        requestOvW.write(Arrays.stream(assig.request.vnfSequence).map(v -> v.name).collect(Collectors.joining(",")));
                        requestOvW.write(";");

                        // route
                        String sep = "";
                        for (NodeAssignment n : assig.path) {
                            requestOvW.write(sep);
                            requestOvW.write(n.vnf == null ? n.node.name : "["+n.node.name+"]");
                            sep = ",";
                        }
                    }
                }

                requestOvW.write("\n");
                requestOvW.close();
            }
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
