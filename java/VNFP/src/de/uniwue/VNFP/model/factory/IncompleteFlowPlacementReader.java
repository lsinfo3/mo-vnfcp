package de.uniwue.VNFP.model.factory;

import de.uniwue.VNFP.algo.ParetoFrontier;
import de.uniwue.VNFP.model.*;
import de.uniwue.VNFP.model.solution.NodeAssignment;
import de.uniwue.VNFP.model.solution.Solution;
import de.uniwue.VNFP.model.solution.TrafficAssignment;
import de.uniwue.VNFP.util.HashWrapper;

import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class can be used to re-import a prior Solution
 * created by this work's PSA algorithm.
 * Unlike the {@link FlowPlacementReader}, it can import
 * placements whose requests do not entirely match the placement file.
 * In addition, only the first Solution of a frontier will be imported (for performance reasons).
 * The resulting Solution will contain the overlap of requested and provided flows.
 *
 * @author alex
 */
public class IncompleteFlowPlacementReader {
	private static Pattern pLine = Pattern.compile("(\\d+);(\\d+);([^ ;,\\]\\[]+);([^ ;,\\]\\[]+);(\\d+(?:\\.\\d+)?);([^ ;]*);([^ ;,]+(?:,[^ ;,]+)*)");
	private static Pattern pRouteNode = Pattern.compile("\\[([^ ;,\\[\\]]+)\\]");

	/**
	 * Reads the traffic routes from the "placementFlows" output file of the PSA algorithm.
	 *
	 * @param pi             The new, to-be-solved ProblemInstance.
	 * @param placementFlows The path of the "placementFlows" CSV output file.
	 * @return A ParetoFrontier object with the first imported solution.
	 * @throws IOException In case of errors while parsing the file.
	 */
	public static ParetoFrontier readFromCsv(ProblemInstance pi, Path placementFlows) throws IOException {
		return readFromCsv(pi, placementFlows.toAbsolutePath().toString());
	}

	/**
	 * Reads the traffic routes from the "placementFlows" output file of the PSA algorithm.
	 *
	 * @param pi             The new, to-be-solved ProblemInstance.
	 * @param placementFlows The path of the "placementFlows" CSV output file.
	 * @return A ParetoFrontier object with the first imported solution.
	 * @throws IOException In case of errors while parsing the file.
	 */
	public static ParetoFrontier readFromCsv(ProblemInstance pi, String placementFlows) throws IOException {
		NetworkGraph ng = pi.ng;
		TrafficRequest[] reqs = pi.reqs;
		ParetoFrontier front = new ParetoFrontier();

		// Prepare requests map
		HashMap<HashWrapper, TrafficRequest> reqMap = new HashMap<>();
		for (TrafficRequest req : reqs) {
			reqMap.put(new HashWrapper(req), req);
		}

		int lastSolution = -1;
		ArrayList<TrafficAssignment> tAssigs = new ArrayList<>();
		ArrayList<TrafficRequest> tReqs = new ArrayList<>();

		LineNumberReader lnr = new LineNumberReader(new FileReader(placementFlows));
		int nr = -1;
		String line;
		while ((line = lnr.readLine()) != null) {
			nr++;

			if (nr == 0) {
				if (!line.trim().toLowerCase().equals("solutionnumber;flowid;ingress;egress;delay;vnfs;route")) {
					lnr.close();
					throw new IOException("The header must equal 'solutionNumber;flowID;ingress;egress;delay;vnfs;route'");
				}
				continue;
			}

			Matcher m = pLine.matcher(line);
			if (!m.matches()) {
				lnr.close();
				throw new IOException("line " + (nr+1) + " does not match pattern " + pLine.pattern());
			}

			int solutionNumber = Integer.parseInt(m.group(1));
			int flowId = Integer.parseInt(m.group(2));
			String ingress = m.group(3);
			String egress = m.group(4);
			double delay = Double.parseDouble(m.group(5));
			String[] vnfString = m.group(6).split(",");
			String[] route = m.group(7).split(",");

			// Save last solution into Pareto frontier:
			if (lastSolution != -1 && solutionNumber != lastSolution) {
				ProblemInstance pi2 = pi.copyWith(tReqs.toArray(new TrafficRequest[tReqs.size()]));
				Solution s = Solution.getInstance(pi2, tAssigs.toArray(new TrafficAssignment[tAssigs.size()]));
				front.add(s);
				break;
			}

			VNF[] vnfs = Arrays.stream(vnfString).map(s -> pi.vnfLib.fromString(s)[0]).toArray(VNF[]::new);
			TrafficRequest req = reqMap.get(new HashWrapper(ng.getNodes().get(ingress), ng.getNodes().get(egress), vnfs));
			if (req == null) {
				continue;
			}

			ArrayList<NodeAssignment> nAssigs = new ArrayList<>();
			int vnfId = -1;
			for (int i = 0; i < route.length; i++) {
				VNF vnf = null;

				Matcher m2 = pRouteNode.matcher(route[i]);
				if (m2.matches()) {
					vnfId++;

					if (vnfId >= req.vnfSequence.length) {
						lnr.close();
						throw new IOException("Too many VNF nodes in line " + (nr + 1) + ": expected="+req.vnfSequence.length+", given>="+(vnfId+1));
					}

					vnf = req.vnfSequence[vnfId];
					route[i] = m2.group(1);
				}

				Node node = ng.getNodes().get(route[i]);
				if (node == null) {
					lnr.close();
					throw new IOException("Node '" + route[i] + "' in line " + (nr + 1) + " cannot be found in the network graph");
				}

				// Find link:
				Link prev = null;
				if (i > 0) {
					Node lastNode = nAssigs.get(nAssigs.size() - 1).node;
					if (!node.equals(lastNode)) {
						prev = node.getNeighbors().stream()
								.filter(n -> n.getOther(node).equals(lastNode))
								.findFirst().orElse(null);
						if (prev == null) {
							lnr.close();
							throw new IOException("Cannot find link for node pair (" + lastNode.name + ", " + node.name + ") in line " + (nr + 1));
						}
					}
				}

				nAssigs.add(new NodeAssignment(node, vnf, prev));
			}

			TrafficAssignment tAssig = new TrafficAssignment(req, nAssigs.toArray(new NodeAssignment[nAssigs.size()]), ng);
			tAssigs.add(tAssig);
			tReqs.add(req);

			lastSolution = solutionNumber;
		}

		if (!tAssigs.isEmpty()) {
			ProblemInstance pi2 = pi.copyWith(tReqs.toArray(new TrafficRequest[tReqs.size()]));
			Solution s = Solution.getInstance(pi2, tAssigs.toArray(new TrafficAssignment[tAssigs.size()]));
			front.add(s);
		}

		lnr.close();
		return front;
	}
}
