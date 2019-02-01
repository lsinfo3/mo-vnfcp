package de.uniwue.VNFP.algo;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * This class is used for the Automated Decision Making process.
 * It calculates weights and rankings from a given List of uncomparable Solutions.
 */
public class ParetoFrontierAnalysis {
	private final ParetoFrontier frontier;
	private final double[][] objectiveVectors;
	private final int numSolutions;
	private final int numObjectives;
	private final int[] allSolutions;
	private final int[] allObjectives;
	private final double[] maxByObjective;
	private final double[] minByObjective;

	private double[][] normalized_maxMin;
	private double[][] normalized_min;
	private double[][] normalized_sum;
	private double[][] normalized_squ;
	private double[] means;
	private double[] variance;
	private double[] stds;
	private double[] variationCoefficients;
	private double[] entropies;

	private double[] uniformWeights;
	private double[] entropyWeights;
	private double[] coeffWeights;
	private double[] stdWeights;

	/**
	 * Initializes a new ParetoFrontierAnalysis object.
	 * Creates a (shallow) copy of the given frontier for internal use.
	 * Considers only feasible solutions.
	 *
	 * @param frontier The Pareto frontier of an evaluation run.
	 */
	public ParetoFrontierAnalysis(ParetoFrontier frontier) {
		frontier = frontier.feasibleSubfront();
		this.frontier = frontier;

		numObjectives = frontier.size() > 0 ? frontier.get(0).getObjectiveVector().length : 0;
		numSolutions = frontier.size();
		allObjectives = IntStream.range(0, numObjectives).toArray();
		allSolutions = IntStream.range(0, numSolutions).toArray();

		objectiveVectors = new double[numSolutions][numObjectives];
		for (int i : allSolutions) {
			objectiveVectors[i] = Arrays.copyOf(frontier.get(i).getObjectiveVector(), numObjectives);
		}

		maxByObjective = Arrays.copyOf(objectiveVectors[0], numObjectives);
		minByObjective = Arrays.copyOf(objectiveVectors[0], numObjectives);
		for (int i : allSolutions) {
			for (int j : allObjectives) {
				if (objectiveVectors[i][j] > maxByObjective[j]) maxByObjective[j] = objectiveVectors[i][j];
				if (objectiveVectors[i][j] < minByObjective[j]) minByObjective[j] = objectiveVectors[i][j];
			}
		}
	}

	/**
	 * Returns the frontier used internally (only feasible solutions).
	 *
	 * @return Feasible frontier from initialization.
	 */
	public ParetoFrontier getFrontier() {
		return frontier;
	}

	/**
	 * Calculates additive rankings (SAW) for the frontier.
	 *
	 * @param weights Weight vector for the individual objectives.
	 * @return An array with one ranking value for every solution in the frontier.
	 */
	public double[] getAdditiveRanks(double[] weights) {
		if (normalized_min == null) normalize();

		return Arrays.stream(allSolutions).mapToDouble(
				i -> Arrays.stream(allObjectives).mapToDouble(
						j -> weights[j] * normalized_min[i][j]
				).sum()
		).toArray();
	}

	/**
	 * Calculates multiplicative rankings (MEW) for the frontier.
	 *
	 * @param weights Weight vector for the individual objectives.
	 * @return An array with one ranking value for every solution in the frontier.
	 */
	public double[] getMultiplicativeRanks(double[] weights) {
		if (normalized_min == null) normalize();

		return Arrays.stream(allSolutions).mapToDouble(
				i -> Arrays.stream(allObjectives).mapToDouble(
						j -> Math.pow(normalized_min[i][j], weights[j])
				).reduce(1.0, (a, b) -> a * b)
		).toArray();
	}

	/**
	 * Calculates the TOPSIS rankings (Technique for Order Preference by Similarity to Ideal Solution).
	 *
	 * @param weights Weight vector for the individual objectives.
	 * @return An array with one ranking value for every solution in the frontier.
	 */
	public double[] getTopsisRanks(double[] weights) {
		if (normalized_squ == null) normalize();

		double[] vMin = Arrays.stream(allObjectives).mapToDouble(
				j -> Arrays.stream(allSolutions).mapToDouble(i -> weights[j] * normalized_squ[i][j]).min().getAsDouble()
		).toArray();
		double[] vMax = Arrays.stream(allObjectives).mapToDouble(
				j -> Arrays.stream(allSolutions).mapToDouble(i -> weights[j] * normalized_squ[i][j]).max().getAsDouble()
		).toArray();

		double[] sepMin = Arrays.stream(allSolutions).mapToDouble(
			i -> Math.sqrt(Arrays.stream(allObjectives).mapToDouble(
					j -> (weights[j] * normalized_squ[i][j] - vMin[j]) * (weights[j] * normalized_squ[i][j] - vMin[j])
			).sum())
		).toArray();
		double[] sepMax = Arrays.stream(allSolutions).mapToDouble(
				i -> Math.sqrt(Arrays.stream(allObjectives).mapToDouble(
						j -> (weights[j] * normalized_squ[i][j] - vMax[j]) * (weights[j] * normalized_squ[i][j] - vMax[j])
				).sum())
		).toArray();

		return Arrays.stream(allSolutions).mapToDouble(
				i -> sepMax[i] / (sepMin[i] + sepMax[i])
		).toArray();
	}

	/**
	 * Calculates the VIKOR rankings.
	 *
	 * @param weights Weight vector for the individual objectives.
	 * @param gamma   Weight for the two strategies (S and R, Sum vs Max)
	 * @return An array with one ranking value for every solution in the frontier.
	 */
	public double[] getVikorRanks(double[] weights, double gamma) {
		double[][] weightedFrac = new double[numSolutions][numObjectives];
		for (int i : allSolutions) {
			for (int j : allObjectives) {
				weightedFrac[i][j] = weights[j] * (minByObjective[j] - objectiveVectors[i][j]) / (minByObjective[j] - maxByObjective[j]);
			}
		}

		double[] stratS = Arrays.stream(allSolutions).mapToDouble(
				i -> Arrays.stream(weightedFrac[i]).sum()
		).toArray();
		double[] stratR = Arrays.stream(allSolutions).mapToDouble(
				i -> Arrays.stream(weightedFrac[i]).max().getAsDouble()
		).toArray();

		double stratSmin = Arrays.stream(stratS).min().getAsDouble();
		double stratSmax = Arrays.stream(stratS).max().getAsDouble();
		double stratRmin = Arrays.stream(stratR).min().getAsDouble();
		double stratRmax = Arrays.stream(stratR).max().getAsDouble();

		return Arrays.stream(allSolutions).mapToDouble(
				i -> gamma * (stratS[i] - stratSmin) / (stratSmax - stratSmin)
					+ (1 - gamma) * (stratR[i] - stratRmin) / (stratRmax - stratRmin)
		).toArray();
	}

	private void normalize() {
		normalized_maxMin = new double[numSolutions][numObjectives];
		for (int i : allSolutions) {
			for (int j : allObjectives) {
				normalized_maxMin[i][j] = (maxByObjective[j] + minByObjective[j] - objectiveVectors[i][j]) / (maxByObjective[j] + minByObjective[j]);
			}
		}

		normalized_min = new double[numSolutions][numObjectives];
		for (int i : allSolutions) {
			for (int j : allObjectives) {
				normalized_min[i][j] = minByObjective[j] / objectiveVectors[i][j];
			}
		}

		normalized_sum = new double[numSolutions][numObjectives];
		for (int j : allObjectives) {
			double sum = Arrays.stream(objectiveVectors).mapToDouble(a -> a[j]).sum();
			for (int i : allSolutions) {
				normalized_sum[i][j] = objectiveVectors[i][j] / sum;
			}
		}

		normalized_squ = new double[numSolutions][numObjectives];
		for (int j : allObjectives) {
			double sum = 0.0;
			for (int i : allSolutions) {
				sum += objectiveVectors[i][j] * objectiveVectors[i][j];
			}
			for (int i : allSolutions) {
				normalized_squ[i][j] = objectiveVectors[i][j] / Math.sqrt(sum);
			}
		}
	}

	private void means() {
		if (normalized_maxMin == null) normalize();

		means = new double[numObjectives];
		for (int j : allObjectives) {
			for (int i : allSolutions) {
				means[j] += normalized_maxMin[i][j];
			}
			means[j] = means[j] / numSolutions;
		}
	}

	private void variances() {
		if (normalized_maxMin == null) normalize();
		if (means == null) means();

		variance = new double[numObjectives];
		for (int j : allObjectives) {
			for (int i : allSolutions) {
				variance[j] += (normalized_maxMin[i][j] - means[j]) * (normalized_maxMin[i][j] - means[j]);
			}
			variance[j] = variance[j] / (numSolutions - 1);
		}
	}

	private void stds() {
		if (variance == null) variances();

		stds = new double[numObjectives];
		for (int j : allObjectives) {
			stds[j] = Math.sqrt(variance[j]);
		}
	}

	private void variationCoefficients() {
		if (stds == null) stds();

		variationCoefficients = new double[numObjectives];
		for (int j : allObjectives) {
			variationCoefficients[j] = stds[j] / means[j];
		}
	}

	private void entropies() {
		if (normalized_sum == null) normalize();

		entropies = new double[numObjectives];
		for (int j : allObjectives) {
			double sum = 0.0;
			for (int i : allSolutions) {
				sum += normalized_sum[i][j] * Math.log(normalized_sum[i][j]);
			}
			entropies[j] = -1 / Math.log(numSolutions) * sum;
		}
	}

	private void uniformWeights() {
		uniformWeights = new double[numObjectives];
		for (int j : allObjectives) {
			uniformWeights[j] = 1.0 / numObjectives;
		}
	}

	private void entropyWeights() {
		if (entropies == null) entropies();

		double sum = Arrays.stream(entropies).map(d -> 1 - d).sum();
		entropyWeights = Arrays.stream(entropies).map(d -> (1 - d) / sum).toArray();
	}

	private void coeffWeights() {
		if (variationCoefficients == null) variationCoefficients();

		double sum = Arrays.stream(variationCoefficients).sum();
		coeffWeights = Arrays.stream(variationCoefficients).map(d -> d / sum).toArray();
	}

	private void stdWeights() {
		if (stds == null) stds();

		double sum = Arrays.stream(stds).sum();
		stdWeights = Arrays.stream(stds).map(d -> d / sum).toArray();
	}

	/**
	 * Retrieve all weight vectors (with description) for this Pareto Frontier.
	 *
	 * @return An array with 4 internal weight arrays (uniform, entropy, coeff and std).
	 */
	public Weights[] getAllWeights() {
		if (uniformWeights == null) uniformWeights();
		if (entropyWeights == null) entropyWeights();
		if (coeffWeights == null) coeffWeights();
		if (stdWeights == null) stdWeights();

		Weights[] w = new Weights[4];
		w[0] = new Weights("uniform", uniformWeights);
		w[1] = new Weights("entropy", entropyWeights);
		w[2] = new Weights("coeff", coeffWeights);
		w[3] = new Weights("std", stdWeights);
		return w;
	}

	/**
	 * Utility class used to store weight arrays with their respective name.
	 * (To be used in evaluation loops.)
	 */
	public static class Weights {
		public final String desc;
		public final double[] w;

		public Weights(String desc, double[] w) {
			this.desc = desc;
			this.w = w;
		}
	}

	/**
	 * Retrieve all ranking vectors (with ranking and weighting description) for this Pareto Frontier.
	 *
	 * @return An array with 16 internal rank arrays (4 weighting methods x 4 ranking methods)
	 */
	public Ranks[] getAllRanks() {
		Weights[] allWeights = getAllWeights();

		Ranks[] r = new Ranks[allWeights.length * 6];
		int i = 0;
		for (Weights w : allWeights) {
			r[i] = new Ranks("SAW", RankOrder.MAXIMIZE, w.desc, getAdditiveRanks(w.w)); i++;
			r[i] = new Ranks("MEW", RankOrder.MAXIMIZE, w.desc, getMultiplicativeRanks(w.w)); i++;
			r[i] = new Ranks("TOPSIS", RankOrder.MAXIMIZE, w.desc, getTopsisRanks(w.w)); i++;
			r[i] = new Ranks("VIKOR 0.0", RankOrder.MINIMIZE, w.desc, getVikorRanks(w.w, 0.0)); i++;
			r[i] = new Ranks("VIKOR 0.5", RankOrder.MINIMIZE, w.desc, getVikorRanks(w.w, 0.5)); i++;
			r[i] = new Ranks("VIKOR 1.0", RankOrder.MINIMIZE, w.desc, getVikorRanks(w.w, 1.0)); i++;
		}
		return r;
	}

	/**
	 * Utility class used to store rank arrays with their respective weighting and ranking method descriptions.
	 * (To be used in evaluation loops.)
	 */
	public static class Ranks {
		public final String rankDesc;
		public final RankOrder order;
		public final String weightDesc;
		public final double[] r;

		public Ranks(String rankDesc, RankOrder order, String weightDesc, double[] r) {
			this.rankDesc = rankDesc;
			this.order = order;
			this.weightDesc = weightDesc;
			this.r = r;
		}
	}

	/**
	 * Enum that represents whether a ranking type has to be maximized (bigger = better) or minimized.
	 */
	public enum RankOrder {
		MAXIMIZE,
		MINIMIZE
	}
}
