package de.uniwue.VNFP.model.log;

import de.uniwue.VNFP.algo.ParetoFrontier;
import de.uniwue.VNFP.algo.ParetoFrontierAnalysis;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * This logger prints the rankings (SAW, MEW, TOPSIS and VIKOR) of every solution
 * into the given CSV file.
 * The writer will be closed afterwards.
 */
public class RankingsExporter implements PSAEventLogger {
	private Writer wFeasibleFrontier;
	private Writer wOrder;
	private Writer wWeights;
	private Writer wRanks;

	/**
	 * Initializes a new instance of this logger.
	 * All output will be written into the given writers.
	 * They will be closed afterwards.
	 *
	 * @param wFeasibleFrontier Logging destination for the filtered frontier (only feasible solutions). May be null if not required.
	 * @param wOrder            Logging destination for the sorted IDs of Pareto optimal solutions with respect to the rankings. May be null if not required.
	 * @param wWeights          Logging destination for weights. May be null if not required.
	 * @param wRanks            Logging destination for ranks. May be null if not required.
	 */
	public RankingsExporter(Writer wFeasibleFrontier, Writer wOrder, Writer wWeights, Writer wRanks) {
		this.wFeasibleFrontier = wFeasibleFrontier;
		this.wOrder = wOrder;
		this.wWeights = wWeights;
		this.wRanks = wRanks;
	}

	@Override
	public void psaEnd(ParetoFrontier paretoFrontier) {
		try {
			ParetoFrontierAnalysis analysis = new ParetoFrontierAnalysis(paretoFrontier);
			ParetoFrontierAnalysis.Weights[] allWeights = analysis.getAllWeights();
			ParetoFrontierAnalysis.Ranks[] allRanks = analysis.getAllRanks();

			if (wFeasibleFrontier != null) {
				new ParetoFrontierExporter(wFeasibleFrontier).psaEnd(analysis.getFrontier());
			}
			if (wOrder != null) {
				wOrder.write("ranktype;weighttype;top95;all");
				Integer[] solutionIdList = IntStream.range(0, analysis.getFrontier().size()).boxed().toArray(Integer[]::new);

				for (ParetoFrontierAnalysis.Ranks r : analysis.getAllRanks()) {
					double f = (r.order == ParetoFrontierAnalysis.RankOrder.MAXIMIZE ? -1 : +1);
					Arrays.sort(solutionIdList, Comparator.comparingDouble(o -> f * r.r[o]));
					double max = Math.max(r.r[solutionIdList[0]], r.r[solutionIdList[solutionIdList.length-1]]);

					wOrder.write("\n" + r.rankDesc + ";" + r.weightDesc + ";");
					wOrder.write(Arrays.stream(solutionIdList)
							.filter(i -> Math.abs(r.r[i] - r.r[solutionIdList[0]]) / max <= 0.05)
							.map(i -> Integer.toString(i))
							.collect(Collectors.joining(",")));
					wOrder.write(";");
					wOrder.write(Arrays.stream(solutionIdList)
							.map(i -> Integer.toString(i))
							.collect(Collectors.joining(",")));
				}
				wOrder.write("\n");
				wOrder.close();
			}
			if (wWeights != null) {
				wWeights.write("weighttype;objective;weightvalue");
				for (ParetoFrontierAnalysis.Weights weights : allWeights) {
					for (int i = 0; i < weights.w.length; i++) {
						wWeights.write("\n" + weights.desc + ";" + i + ";" + weights.w[i]);
					}
				}
				wWeights.write("\n");
				wWeights.close();
			}
			if (wRanks != null) {
				wRanks.write("solution;ranktype;weights;rankvalue");
				for (ParetoFrontierAnalysis.Ranks ranks : allRanks) {
					for (int i = 0; i < ranks.r.length; i++) {
						wRanks.write("\n" + i + ";" + ranks.rankDesc + ";" + ranks.weightDesc + ";" + ranks.r[i]);
					}
				}
				wRanks.write("\n");
				wRanks.close();
			}
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
