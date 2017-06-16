package de.lexej.VNFP.model.log;

import de.lexej.VNFP.algo.ParetoFrontier;
import de.lexej.VNFP.model.NetworkGraph;
import de.lexej.VNFP.model.TrafficRequest;
import de.lexej.VNFP.model.solution.Solution;
import de.lexej.VNFP.util.Config;

import java.io.IOException;
import java.io.Writer;
import java.util.Objects;

/**
 * This logger prints details of every solution in the final
 * pareto frontier into a given writer in human readable form.
 * The writer will be closed afterwards.
 *
 * @author alex
 */
public class PSAResultPrinter implements PSAEventLogger {
    private Writer w;
    private long startTime;
    private long seed;

    /**
     * Initializes a new instance of this logger.
     * All output will be written into the given writer.
     * The writer will be closed afterwards.
     *
     * @param w Logging destination.
     */
    public PSAResultPrinter(Writer w) {
        this.w = Objects.requireNonNull(w);
    }

    @Override
    public void psaStart(NetworkGraph ng, TrafficRequest[] reqs, long seed) {
        startTime = System.currentTimeMillis();
        this.seed = seed;
    }

    @Override
    public void psaEnd(ParetoFrontier paretoFrontier) {
        long diff = System.currentTimeMillis() - startTime;

        Config c = Config.getInstance();
        paretoFrontier.sort(Solution::compareTo);

        try {
            w.write("PSA with s=" + c.s + " m=" + c.m + " tmax=" + c.tmax + " tmin=" + c.tmin + " rho=" + c.rho + " runtime=" + c.runtime + " prepMode=" + c.prepMode + " useWeights=" + c.useWeights + " seed=" + seed + "\n");

            w.write(paretoFrontier.size() + " Solutions in Pareto-Frontier (" + (diff / 1000.0) + "s):\n");
            for (Solution sol : paretoFrontier) {
                w.write("- " + sol.toString() + "\n");
            }

            for (Solution sol : paretoFrontier) {
                w.write("\n\n\n");
                w.write("- " + sol.toString() + "\n");
                sol.printDebugOutput(w);
            }

            w.write(paretoFrontier.size() + " Solutions in Pareto-Frontier (" + (diff / 1000.0) + "s):\n");
            for (Solution sol : paretoFrontier) {
                w.write("- " + sol.toString() + "\n");
            }

            w.write("\n");
            w.close();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
