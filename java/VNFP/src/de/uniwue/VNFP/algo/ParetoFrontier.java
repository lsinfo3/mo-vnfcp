package de.uniwue.VNFP.algo;

import de.uniwue.VNFP.model.Objs;
import de.uniwue.VNFP.model.solution.Solution;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/**
 * This class computes the Pareto Frontier from a collection of solutions.
 *
 * @author alex
 */
public class ParetoFrontier extends ArrayList<Solution> {
    private double[] min;
    private double[] max;

    /**
     * Calls the superior constructor (ArrayList).
     *
     * @param initialCapacity (see ArrayList)
     */
    public ParetoFrontier(int initialCapacity) {
        super(initialCapacity);
    }

    /**
     * Calls the superior constructor (ArrayList).
     */
    public ParetoFrontier() {
    }

    /**
     * Calls the superior constructor (ArrayList).
     *
     * @param c (see ArrayList)
     */
    public ParetoFrontier(Collection<? extends Solution> c) {
        super(c);
    }

    /**
     * Naive approach: every solution is compared to every other.
     * Dominated solutions get marked.
     * At the end, all non-marked solutions are returned in a list.
     * Runtime: for <tt>n</tt> solutions and <tt>k</tt> objectives in
     * <tt>O(kn²)</tt>.
     *
     * @param solutions All relevant solutions
     * @return Pareto Frontier of these solutions
     */
    public static ParetoFrontier bruteForce(Solution[] solutions) {
        boolean[] dominated = new boolean[solutions.length];

        // Check every (not yet dominated) solution:
        for (int i = 0; i < solutions.length; i++) {
            if (dominated[i]) continue;
            double[] s1 = solutions[i].getObjectiveVector();

            // Compare with every other (not yet dominated) solution:
            for (int j = i+1; j < solutions.length; j++) {
                if (dominated[i]) break;
                if (dominated[j]) continue;
                double[] s2 = solutions[j].getObjectiveVector();

                // If they are indifferent: mark s2 "dominated" and proceed:
                if (Arrays.equals(s1, s2)) {
                    dominated[j] = true;
                    continue;
                }

                // First, mark both solutions as "dominated":
                dominated[i] = true;
                dominated[j] = true;

                // Check for every objective, whether the solutions are actually dominated:
                for (int k = 0; k < s1.length; k++) {
                    if (s1[k] < s2[k]) dominated[i] = false;
                    if (s2[k] < s1[k]) dominated[j] = false;
                }
            }
        }

        // Collect all non-dominated solutions:
        int newSize = 0;
        for (int i = 0; i < dominated.length; i++) {
            if (!dominated[i]) newSize++;
        }

        ParetoFrontier paretoFrontier = new ParetoFrontier(newSize);
        for (int i = 0; i < dominated.length; i++) {
            if (!dominated[i]) paretoFrontier.add(solutions[i]);
        }

        return paretoFrontier;
    }

    /**
     * Inserts a new solution into the existing Pareto Frontier, if it is not
     * dominated by another solution already.
     * (If it is dominated, the Pareto Frontier remains unchanged.)
     * Further, deletes all points in the current Pareto Frontier that are dominated by the new point.
     * (May also change the order of elements in the list.)
     *
     * @param newSolution New solution that shall be inserted..
     * @return <tt>null</tt>, if the new solution was not inserted;
     * otherwise, an ArrayList with all removed points is returned.
     */
    public ArrayList<Solution> updateParetoFrontier(Solution newSolution) {
        Objs o = newSolution.obj;
        double[] newVector = newSolution.getObjectiveVector();
        newVector = Arrays.copyOf(newVector, newVector.length + 1);
        newVector[newVector.length - 1] = newSolution.vals[o.UNFEASIBLE.i];
        ArrayList<Solution> removed = new ArrayList<>();
        int i = 0;
        while (i < size()) {
            double[] iVector = get(i).getObjectiveVector();
            iVector = Arrays.copyOf(iVector, iVector.length + 1);
            iVector[iVector.length - 1] = get(i).vals[o.UNFEASIBLE.i];
            int dominance = getDominance(iVector, newVector);
            //if (newSolution.isFeasible() && !get(i).isFeasible()) dominance = +1;
            //if (!newSolution.isFeasible() && get(i).isFeasible()) dominance = -1;

            // Is the new point dominated by the i-th solution? -> Abort.
            if (dominance == -1 || Arrays.equals(newVector, iVector)) return removed;

            // Is the i-th solution dominated by the new point? -> Remove i-th element.
            // (Changes list's order for performance reasons.)
            else if (dominance == +1) {
                // Switch i-th element with the last:
                Solution temp = get(i);
                set(i, get(size()-1));
                set(size()-1, temp);

                // Remove last:
                removed.add(remove(size()-1));

                if (min != null) {
                    min = null;
                    max = null;
                }
            }
            else {
                i++;
            }
        }

        // Solution is not dominated? -> Insert.
        // All newly dominated points are already removed here.
        add(newSolution);

        if (min != null) {
            double[] vec = newSolution.getObjectiveVector();
            int numObjectives = vec.length;
            for (int j = 0; j < numObjectives; j++) {
                min[j] = Math.min(min[j], vec[j]);
                max[j] = Math.max(max[j], vec[j]);
            }
        }

        return removed;
    }

    /**
     * Returns an array that contains the smallest value of every objective in the current Pareto Frontier.
     *
     * @return <tt>new double[]</tt> with the min. value for every objective
     */
    public double[] getMin() {
        if (min == null) {
            int numObjectives = get(0).getObjectiveVector().length;
            min = new double[numObjectives];
            max = new double[numObjectives];

            for (int i = 0; i < size(); i++) {
                double[] vec = get(i).getObjectiveVector();
                for (int o = 0; o < numObjectives; o++) {
                    if (i == 0) {
                        min[o] = vec[o];
                        max[o] = vec[o];
                    } else {
                        min[o] = Math.min(min[o], vec[o]);
                        max[o] = Math.max(max[o], vec[o]);
                    }
                }
            }
        }

        return Arrays.copyOf(min, min.length);
    }

    /**
     * Returns an array that contains the biggest value of every objective in the current Pareto Frontier.
     *
     * @return <tt>new double[]</tt> with the max. value for every objective
     */
    public double[] getMax() {
        // Create array, if necessary:
        getMin();

        return Arrays.copyOf(max, max.length);
    }

    /**
     * Checks whether one vector dominates the other.
     *
     * @param a First objective vector.
     * @param b Second objective vector.
     * @return -1, if <tt>a</tt> is better than <tt>b</tt>...
     * +1, if <tt>b</tt> is better than <tt>a</tt>...
     * 0, if they are incomparable or indifferent.
     */
    public static int getDominance(double[] a, double[] b) {
        // Is a dominated by b?
        boolean dominated = true;
        for (int i = 0; i < a.length; i++) {
            if (a[i] < b[i]) {
                dominated = false;
                break;
            }
        }
        if (dominated) return +1;

        // Is b dominated by a?
        dominated = true;
        for (int i = 0; i < a.length; i++) {
            if (b[i] < a[i]) {
                dominated = false;
                break;
            }
        }
        if (dominated) return -1;

        return 0;
    }

    /**
     * Returns the Euclidean distance between two points.
     *
     * @param a First objective vector.
     * @param b Second objective vector.
     * @return Euclidean distance (sqrt(sum of distances²))
     */
    public static double getDistance(double[] a, double[] b) {
        double sum = 0.0;
        for (int i = 0; i < a.length; i++) {
            sum += (a[i] - b[i]) * (a[i] - b[i]);
        }
        return Math.sqrt(sum);
    }

    /**
     * Returns a new ParetoFrontier object with the same solutions.
     *
     * @return Clone of <tt>this</tt>.
     */
    public ParetoFrontier copy() {
        ParetoFrontier ret = new ParetoFrontier(this);
        ret.min = min;
        ret.max = max;
        return ret;
    }

    /**
     * Returns a subset of this ParetoFrontier containing only feasible solutions.
     *
     * @return All feasible solutions of this frontier.
     */
    public ParetoFrontier feasibleSubfront() {
        ParetoFrontier ret = new ParetoFrontier();
        for (Solution s : this) {
            if (s.isFeasible()) {
                ret.updateParetoFrontier(s);
            }
        }
        return ret;
    }
}
