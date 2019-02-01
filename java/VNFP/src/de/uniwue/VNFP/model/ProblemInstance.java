package de.uniwue.VNFP.model;

import de.uniwue.VNFP.algo.ParetoFrontier;
import de.uniwue.VNFP.model.solution.Solution;

import java.util.Objects;

/**
 * This class summarized all objects that belong to the problem's input and gather them in one instance.
 */
public class ProblemInstance {
    /**
     * The network graph with available resources.
     */
    public final NetworkGraph ng;
    /**
     * The VNF type library for this problem.
     */
    public final VnfLib vnfLib;
    /**
     * All network demands.
     */
    public final TrafficRequest[] reqs;
    /**
     * The possible objectives library for this problem.
     */
    public final Objs objectives;
    /**
     * The initial solution set that the algorithm starts with at iteration 0.
     * May be null (e.g. for the initial solutions themselves).
     */
    public Solution[] initialSolutions;
    /**
     * A set of solutions for this problem instance. May be null.
     */
    public ParetoFrontier solution;

    /**
     * Creates a new ProblemInstance from the given input objects.
     *
     * @param ng               The network graph with available resources.
     * @param vnfLib           The VNF type library for this problem.
     * @param reqs             All network demands.
     * @param objectives       The possible objectives library for this problem.
     * @param initialSolutions The initial solution set that the algorithm starts with at iteration 0.
     *                         May be null (e.g. for the initial solutions themselves).
     */
    public ProblemInstance(NetworkGraph ng, VnfLib vnfLib, TrafficRequest[] reqs, Objs objectives, Solution[] initialSolutions) {
        this.ng = Objects.requireNonNull(ng);
        this.vnfLib = Objects.requireNonNull(vnfLib);
        this.reqs = Objects.requireNonNull(reqs);
        this.objectives = Objects.requireNonNull(objectives);
        this.initialSolutions = initialSolutions;
    }

	/**
	 * Creates a new ProblemInstance from the given input objects.
	 *
	 * @param ng               The network graph with available resources.
	 * @param vnfLib           The VNF type library for this problem.
	 * @param reqs             All network demands.
	 * @param objectives       The possible objectives library for this problem.
	 */
	public ProblemInstance(NetworkGraph ng, VnfLib vnfLib, TrafficRequest[] reqs, Objs objectives) {
		this(ng, vnfLib, reqs, objectives, null);
	}

    /**
     * Creates a new ProblemInstance object as a copy of the given object.
     * The solution frontier is not copied in the process, only the problem input is.
     *
     * @param pi Previous ProblemInstance to copy from.
     */
    public ProblemInstance(ProblemInstance pi) {
        this(pi.ng, pi.vnfLib, pi.reqs, pi.objectives, pi.initialSolutions);
    }

    /**
     * Creates a new ProblemInstance object as a copy of this object.
     * The solution frontier is not copied in the process, only the problem input is.
     * The TrafficRequests are orverwritten with the given array.
     *
     * @param newReqs New TrafficRequests for the new ProblemInstance.
     * @return Clone of this ProblemInstance with new Request array.
     */
    public ProblemInstance copyWith(TrafficRequest[] newReqs) {
        return new ProblemInstance(ng, vnfLib, newReqs, objectives, initialSolutions);
    }
}
