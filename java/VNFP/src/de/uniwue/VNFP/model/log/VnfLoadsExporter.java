package de.uniwue.VNFP.model.log;

import de.uniwue.VNFP.algo.ParetoFrontier;
import de.uniwue.VNFP.model.NetworkGraph;
import de.uniwue.VNFP.model.TrafficRequest;
import de.uniwue.VNFP.model.solution.overview.NodeOverview;
import de.uniwue.VNFP.model.solution.Solution;
import de.uniwue.VNFP.model.solution.VnfInstances;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * This logger prints information about the VNF instances' loads into the given writer.
 * For every temperature level, the mean load of a chosen solution (usually the first in the array)
 * is written into the file. The writer will be closed afterwards.
 *
 * @author alex
 */
public class VnfLoadsExporter implements PSAEventLogger {
    private Writer w;

    /**
     * Initializes a new instance of this logger.
     * All output will be written into the given writer.
     * The writer will be closed afterwards.
     *
     * @param w Logging destination.
     */
    public VnfLoadsExporter(Writer w) {
        this.w = Objects.requireNonNull(w);
    }

    @Override
    public void psaStart(NetworkGraph ng, TrafficRequest[] reqs, long seed) {
        try {
            w.write("temperature;iteration;numberOfVnfInstances;meanLoad;individualLoads");
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void endTemperatureIteration(double currentTemperature, int temperatureIndex, ParetoFrontier currentParetoFrontier, Solution[] currentAcceptedSolutions, String... additionalInformation) {
        // Find vnf loads for this solution:
        double sumLoads = 0.0;
        double sumCaps = 0.0;
        LinkedList<String> individualLoads = new LinkedList<>();

        for (NodeOverview nodeOv : currentAcceptedSolutions[0].nodeMap.values()) {
            for (VnfInstances vnfInst : nodeOv.getVnfInstances().values()) {
                sumLoads += Arrays.stream(vnfInst.loads).sum();
                sumCaps += vnfInst.type.processingCapacity * vnfInst.loads.length;
                Arrays.stream(vnfInst.getLoadPercentage()).mapToObj(d -> "" + d).forEach(individualLoads::add);
            }
        }

        // Write information:
        try {
            w.write(String.format("\n%s;%s;%.0f;%s;%s",
                    currentTemperature,
                    temperatureIndex,
                    currentAcceptedSolutions[0].vals[currentAcceptedSolutions[0].obj.NUMBER_OF_VNF_INSTANCES.i],
                    sumLoads / sumCaps,
                    individualLoads.stream().collect(Collectors.joining(","))));
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
}
