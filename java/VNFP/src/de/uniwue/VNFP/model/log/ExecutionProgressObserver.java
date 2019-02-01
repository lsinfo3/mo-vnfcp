package de.uniwue.VNFP.model.log;

import de.uniwue.VNFP.algo.ParetoFrontier;
import de.uniwue.VNFP.model.NetworkGraph;
import de.uniwue.VNFP.model.Objs;
import de.uniwue.VNFP.model.TrafficRequest;
import de.uniwue.VNFP.model.solution.Solution;
import de.uniwue.VNFP.util.Config;

import java.util.Arrays;
import java.util.stream.Collectors;

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
        System.out.printf("Input: Graph with %d nodes and %d links, %d traffic demands given.\n",
                ng.getNodes().size(),
                ng.getLinks().size(),
                reqs.length);
        System.out.println("Preparing acceptance probabilities...");
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
            Objs o = o1.obj;
            if (o1.isFeasible() && !o2.isFeasible()) return -1;
            if (!o1.isFeasible() && o2.isFeasible()) return +1;
            if (o1.vals[o.TOTAL_USED_RESOURCES[0].i] < o2.vals[o.TOTAL_USED_RESOURCES[0].i]) return -1;
            if (o1.vals[o.TOTAL_USED_RESOURCES[0].i] > o2.vals[o.TOTAL_USED_RESOURCES[0].i]) return +1;
            if (o1.vals[o.MEAN_DELAY_INDEX.i] < o2.vals[o.MEAN_DELAY_INDEX.i]) return -1;
            if (o1.vals[o.MEAN_DELAY_INDEX.i] > o2.vals[o.MEAN_DELAY_INDEX.i]) return +1;
            if (o1.vals[o.MEAN_HOPS_INDEX.i] < o2.vals[o.MEAN_HOPS_INDEX.i]) return -1;
            if (o1.vals[o.MEAN_HOPS_INDEX.i] > o2.vals[o.MEAN_HOPS_INDEX.i]) return +1;

            return o1.compareTo(o2);
        });

        System.out.println(paretoFrontier.size() + " Solutions in Pareto-Frontier ("+(diff/1000.0)+"s):");
        String[][] solutionStrings = new String[paretoFrontier.size()][];
        int s = 0;
        int parts = 0;
        for (Solution sol : paretoFrontier) {
            Objs o = sol.obj;
            solutionStrings[s] = String.format("[%d] feasible=%b"
                            + " res[0]=%.0f"
                            + " numberOfHops=%.0f"
                            + " loadIndex=%.2f"
                            + " delayIndex=%.2f"
                            + " flowMigration=%.2f"
                            + "   %s",
                    sol.creationIteration,
                    sol.vals[o.UNFEASIBLE.i] == 0.0,
                    sol.vals[o.TOTAL_USED_RESOURCES[0].i],
                    sol.vals[o.NUMBER_OF_HOPS.i],
                    sol.vals[o.MEDIAN_INVERSE_LOAD_INDEX.i],
                    sol.vals[o.MEAN_DELAY_INDEX.i],
                    sol.vals[o.TOTAL_FLOW_MIGRATION_PENALTY.i],
                    sol.toString()).split(" ", parts);
            if (parts == 0) parts = solutionStrings[s].length;

            s++;
        }
        int[] sizes = new int[parts];
        for (s = 0; s < solutionStrings.length; s++) {
            for (int i = 0; i < solutionStrings[s].length; i++) {
                sizes[i] = Math.max(sizes[i], solutionStrings[s][i].length());
            }
        }

        for (s = 0; s < solutionStrings.length; s++) {
            System.out.print("  * ");
            for (int i = 0; i < solutionStrings[s].length; i++) {
                System.out.print(solutionStrings[s][i] + repeatString(" ", sizes[i] + 1 - solutionStrings[s][i].length()));
            }
            System.out.println();
        }
    }

    @Override
    public void newSolutionInParetoFrontier(double currentTemperature, int temperatureIndex, Solution acceptedSolution, String... additionalInformation) {
        System.out.print("*");
        System.out.flush();
    }

    private static String repeatString(String s, int num) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < num; i++) {
            sb.append(s);
        }
        return sb.toString();
    }
}
