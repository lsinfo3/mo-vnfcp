package de.lexej.VNFP.model.log;

import de.lexej.VNFP.algo.ParetoFrontier;
import de.lexej.VNFP.model.NetworkGraph;
import de.lexej.VNFP.model.TrafficRequest;
import de.lexej.VNFP.model.VNF;
import de.lexej.VNFP.model.VnfLib;
import de.lexej.VNFP.model.solution.overview.NodeOverview;
import de.lexej.VNFP.model.solution.Solution;
import de.lexej.VNFP.model.solution.VnfInstances;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;

/**
 * This logger prints an overview (node, type, load) of every VNF instance
 * of a selected solution (usually the first in the array)
 * at every temperature change into the given writer.
 * The writer will be closed afterwards.
 *
 * This is mainly useful for debugging purposes, as to observe
 * the changes of VNF instances.
 *
 * @author alex
 */
public class VnfDetailsExporter implements PSAEventLogger {
    private Writer w;
    private int solutionNumber;
    private HashMap<VnfInstances, double[]> lastSolution;

    /**
     * Initializes a new instance of this logger.
     * All output will be written into the given writer.
     * The writer will be closed afterwards.
     *
     * @param w Logging destination.
     */
    public VnfDetailsExporter(Writer w) {
        this.w = Objects.requireNonNull(w);
        this.solutionNumber = 0;
    }

    @Override
    public void psaStart(NetworkGraph ng, TrafficRequest[] reqs, long seed) {
        try {
            w.write("solutionNumber;instanceNumber;node;vnfType;variable;value");
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void endTemperatureIteration(double currentTemperature, int temperatureIndex, ParetoFrontier currentParetoFrontier, Solution[] currentAcceptedSolutions, String... additionalInformation) {
        try {
            exportVnfDetails(currentAcceptedSolutions[0]);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void psaEnd(ParetoFrontier paretoFrontier) {
        try {
            w.write("\n");
            w.close();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    //@Override
    //public synchronized void innerIteration(double currentTemperature, int temperatureIndex, int innerIterationIndex, Solution currentSolution, String... additionalInformation) {
    //    // TODO: Remove, only for debugging.
    //    try {
    //        exportVnfDetails(currentSolution);
    //    }
    //    catch (IOException e) {
    //        throw new RuntimeException(e);
    //    }
    //}

    /**
     * Prints one line for every VNF instance into the writer.
     * Also prints load-differences compared to the last observed solution.
     * Finally, prints number of newly instantiated as well as removed instances.
     *
     * @param s Solution whose information should be exported.
     */
    private void exportVnfDetails(Solution s) throws IOException {
        // Find vnf loads for this solution:
        int instanceNumber = 0;
        HashMap<VnfInstances, double[]> current = new HashMap<>();
        int newVnfs = 0;
        int removedVnfs = 0;
        VnfLib vnfLib = s.vnfMap.keySet().iterator().next().vnfLib;

        for (NodeOverview nodeOv : s.nodeMap.values()) {
            for (VNF vnf : vnfLib.getAllVnfs()) {
                VnfInstances vnfInst = nodeOv.getVnfCapacities(vnf);

                double[] loads = Arrays.copyOf(vnfInst.loads, vnfInst.loads.length);
                Arrays.sort(loads);
                current.put(vnfInst, loads);

                double[] last = null;
                if (lastSolution != null) {
                    last = lastSolution.get(vnfInst);
                }

                for (double d : loads) {
                    w.write(String.format("\n%d;%d;%s;%s;%s;%s",
                            solutionNumber,
                            instanceNumber,
                            nodeOv.node.name,
                            vnf.name,
                            "load",
                            d / vnf.processingCapacity));

                    instanceNumber++;
                }

                if (last != null) {
                    double currentLoad = 0.0;
                    if (loads.length != 0) {
                        currentLoad = Arrays.stream(loads).sum() / vnf.processingCapacity;
                    }
                    double lastLoad = 0.0;
                    if (last.length != 0) {
                        lastLoad = Arrays.stream(last).sum() / vnf.processingCapacity;
                    }

                    w.write(String.format("\n%d;%d;%s;%s;%s;%s",
                            solutionNumber,
                            instanceNumber,
                            nodeOv.node.name,
                            vnf.name,
                            "diff",
                            currentLoad - lastLoad));

                    if (loads.length < last.length) {
                        removedVnfs += last.length - loads.length;
                    }
                    else if (loads.length > last.length) {
                        newVnfs += loads.length - last.length;
                    }

                    instanceNumber++;
                }
            }
        }

        // How did the number of instances change for this solution?
        w.write(String.format("\n%d;%d;%s;%s;%s;%s",
                solutionNumber,
                instanceNumber,
                "zz+",
                "all",
                "load",
                (double) newVnfs / 10.0));
        instanceNumber++;
        w.write(String.format("\n%d;%d;%s;%s;%s;%s",
                solutionNumber,
                instanceNumber,
                "zz-",
                "all",
                "load",
                (double) removedVnfs / 10.0));

        if (lastSolution != null) lastSolution.clear();
        lastSolution = current;
        solutionNumber++;
    }
}
