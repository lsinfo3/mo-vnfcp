package de.lexej.VNFP.model.log;

import de.lexej.VNFP.algo.ParetoFrontier;
import de.lexej.VNFP.model.NetworkGraph;
import de.lexej.VNFP.model.TrafficRequest;
import de.lexej.VNFP.model.solution.Solution;
import de.lexej.VNFP.util.Config;

import java.util.Arrays;
import java.util.stream.Collectors;

import static de.lexej.VNFP.model.solution.Solution.Vals.*;

/**
 * This logger prints information about the progress of
 * the algorithm execution into {@code System.out}.
 *
 * @author alex
 */
public class ExecutionProgressObserver implements PSAEventLogger {
    private int numberOfIterations;
    private long startTime;

    @Override
    public void psaStart(NetworkGraph ng, TrafficRequest[] reqs, long seed) {
        Config c = Config.getInstance();
        numberOfIterations = (int) Math.ceil(Math.log(c.tmin / c.tmax) / Math.log(c.rho));
        startTime = System.currentTimeMillis();
        System.out.println("Starting PSA with s="+c.s+" m="+c.m+" tmax="+c.tmax+" tmin="+c.tmin+" rho="+c.rho+" runtime="+c.runtime+" prepMode="+c.prepMode+" useWeights="+c.useWeights+" seed="+seed);
    }

    @Override
    public void beginTemperatureIteration(double currentTemperature, int temperatureIndex, ParetoFrontier currentParetoFrontier, Solution[] currentAcceptedSolutions, String... additionalInformation) {
        System.out.println("T="+currentTemperature+" ("+(temperatureIndex +1)+"/"+numberOfIterations+")");
        System.out.print("  - Pareto frontier updates: ");
        System.out.flush();
    }

    @Override
    public void endTemperatureIteration(double currentTemperature, int temperatureIndex, ParetoFrontier currentParetoFrontier, Solution[] currentAcceptedSolutions, String... additionalInformation) {
        System.out.println();
        System.out.println("  - Done. " + Arrays.stream(additionalInformation).collect(Collectors.joining("; ")));
    }

    @Override
    public void psaEnd(ParetoFrontier paretoFrontier) {
        long diff = System.currentTimeMillis() - startTime;

        paretoFrontier.sort((o1, o2) -> {
            if (o1.isFeasible() && !o2.isFeasible()) return -1;
            if (!o1.isFeasible() && o2.isFeasible()) return +1;
            if (o1.vals[TOTAL_USED_CPU.i] < o2.vals[TOTAL_USED_CPU.i]) return -1;
            if (o1.vals[TOTAL_USED_CPU.i] > o2.vals[TOTAL_USED_CPU.i]) return +1;
            if (o1.vals[MEAN_DELAY_INDEX.i] < o2.vals[MEAN_DELAY_INDEX.i]) return -1;
            if (o1.vals[MEAN_DELAY_INDEX.i] > o2.vals[MEAN_DELAY_INDEX.i]) return +1;
            if (o1.vals[MEAN_HOPS_INDEX.i] < o2.vals[MEAN_HOPS_INDEX.i]) return -1;
            if (o1.vals[MEAN_HOPS_INDEX.i] > o2.vals[MEAN_HOPS_INDEX.i]) return +1;

            return o1.compareTo(o2);
        });

        System.out.println(paretoFrontier.size() + " Solutions in Pareto-Frontier ("+(diff/1000.0)+"s):");
        for (Solution sol : paretoFrontier) {
            System.out.println();
            System.out.println(String.format("  * [%d] feasible=%b"
                            + " cpu=%.0f"
                            + " medianDelayIndex=%.3f"
                            + " medianHopsIndex=%.3f"
                            + " medianInverseLoadIndex=%.3f"
                            + " " + sol.toString(),
                    sol.creationIteration,
                    sol.vals[UNFEASIBLE.i] == 0.0,
                    sol.vals[TOTAL_USED_CPU.i],
                    sol.vals[MEDIAN_DELAY_INDEX.i],
                    sol.vals[MEDIAN_HOPS_INDEX.i],
                    sol.vals[MEDIAN_INVERSE_LOAD_INDEX.i],
                    sol.toString()
                    ));
        }
    }

    @Override
    public void newSolutionInParetoFrontier(double currentTemperature, int temperatureIndex, Solution acceptedSolution, String... additionalInformation) {
        System.out.print("*");
        System.out.flush();
    }
}
