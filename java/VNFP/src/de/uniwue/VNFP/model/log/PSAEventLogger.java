package de.uniwue.VNFP.model.log;

import de.uniwue.VNFP.algo.ParetoFrontier;
import de.uniwue.VNFP.model.NetworkGraph;
import de.uniwue.VNFP.model.TrafficRequest;
import de.uniwue.VNFP.model.solution.Solution;

/**
 * This interface is used during the PSA-Algorithm to notify
 * implementing classes of certain events during the execution
 * (beginning of the calculation, new temperature level, ...)
 * while providing information about the current status
 * (current pareto frontier).
 * <p>
 * In order to receive information, an event logger must be
 * registred in the PSA instance through {@code addEventLogger()}.
 *
 * @author alex
 */
public interface PSAEventLogger {
    /**
     * This method is called when the PSA algorithm starts computing solutions.
     *
     * @param ng      Network Graph (problem specific argument)
     * @param reqs    All traffic requests (problem specific argument)
     * @param seed    Seed used to create the random object
     */
    default void psaStart(NetworkGraph ng, TrafficRequest[] reqs, long seed) {}

    /**
     * This method is called when a new temperature level is reached.
     *
     * @param currentTemperature       The new temperature level
     * @param temperatureIndex         The number of the current temperature level (0 = starting temperature)
     * @param currentParetoFrontier       The current pareto frontier from previous iterations
     * @param currentAcceptedSolutions The current solution set that is used in the neighbour search.
     * @param additionalInformation    Further execution details (for debugging, e.g. acceptanceProbabilities)
     */
    default void beginTemperatureIteration(double currentTemperature, int temperatureIndex, ParetoFrontier currentParetoFrontier, Solution[] currentAcceptedSolutions, String... additionalInformation) {}

    /**
     * This method is called when all iterations for a temperature level are finished.
     *
     * @param currentTemperature    The finished temperature level
     * @param temperatureIndex      The number of the current temperature level (0 = starting temperature)
     * @param currentParetoFrontier    The current pareto frontier after this iteration
     * @param currentAcceptedSolutions The current solution set that is used in the neighbour search.
     * @param additionalInformation Further execution details (for debugging, e.g. acceptanceProbabilities)
     */
    default void endTemperatureIteration(double currentTemperature, int temperatureIndex, ParetoFrontier currentParetoFrontier, Solution[] currentAcceptedSolutions, String... additionalInformation) {}

    /**
     * This method is called when the PSA algorithm has finished its work.
     *
     * @param paretoFrontier The final solution set of the algorithm
     */
    default void psaEnd(ParetoFrontier paretoFrontier) {}

    /**
     * This method is called after every execution of the inner loop of PSA.
     * The entire pareto frontier is currently unknown due to multi threading, only one solution
     * is visible in this context.
     * Note that the implementation should be synchronized in most cases,
     * it is mostly intended for debugging purposes.
     *
     * @param currentTemperature    The current temperature level
     * @param temperatureIndex      The number of the current temperature level (0 = starting temperature)
     * @param innerIterationIndex   The number of the inner iteration (0 = first iteration for this temperature level)
     * @param currentSolution       The solution after the last loop execution
     * @param additionalInformation Further execution details (for debugging, e.g. acceptanceProbabilities)
     */
    default void innerIteration(double currentTemperature, int temperatureIndex, int innerIterationIndex, Solution currentSolution, String... additionalInformation) {}

    /**
     * This method is called whenever a new solution enters the Pareto frontier.
     * Note that the implementation should be synchronized in most cases,
     * it is mostly intended for debugging purposes.
     *
     * @param currentTemperature    The current temperature level
     * @param temperatureIndex      The number of the current temperature level (0 = starting temperature)
     * @param acceptedSolution      The solution that was added to the Pareto frontier
     * @param additionalInformation Further execution details (for debugging, e.g. acceptanceProbabilities)
     */
    default void newSolutionInParetoFrontier(double currentTemperature, int temperatureIndex, Solution acceptedSolution, String... additionalInformation) {}
}
