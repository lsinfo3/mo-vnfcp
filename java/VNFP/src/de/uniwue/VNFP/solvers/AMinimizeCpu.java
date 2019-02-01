package de.uniwue.VNFP.solvers;

import de.uniwue.VNFP.model.*;
import gurobi.GRB;
import gurobi.GRBException;
import gurobi.GRBVar;
import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;

import java.util.*;

public abstract class AMinimizeCpu {
	public ProblemInstance pi;
	public TrafficRequest[] requests;
	public VNF[][] functions;
	public VNF[] vnfs;
	public Node[] nodes;
	public Link[] links;
	public HashMap<Node, Integer> nodeIndices;
	public HashMap<Link, Integer> linkIndices;
	public HashMap<VNF, Integer> vnfIndices;
	public ArrayList<Integer[]>[] typeAssoc;
	public int maxInst;

	public AMinimizeCpu(ProblemInstance pi) {
		this.pi = pi;

		// Prepare: Calc. upper bound for number of instances.
		HashMap<VNF, ArrayList<Double>> requestsPerVnf = new HashMap<>();
		for (VNF v : pi.vnfLib.getAllVnfs()) {
			requestsPerVnf.put(v, new ArrayList<>());
		}
		for (TrafficRequest r : pi.reqs) {
			for (VNF v : r.vnfSequence) {
				requestsPerVnf.get(v).add(r.bandwidthDemand);
			}
		}
		maxInst = 0;
		for (VNF v : pi.vnfLib.getAllVnfs()) {
			maxInst = Math.max(maxInst, firstFitPacking(requestsPerVnf.get(v), v.processingCapacity));
		}
		requestsPerVnf.clear();
		requestsPerVnf = null;

		// Setup arrays for index -> object conversion
		requests = pi.reqs;
		functions = new VNF[pi.reqs.length][];
		for (int v = 0; v < functions.length; v++) {
			functions[v] = requests[v].vnfSequence;
		}
		HashSet<VNF> allVnfs = pi.vnfLib.getAllVnfs();
		vnfs = allVnfs.toArray(new VNF[allVnfs.size()]);
		nodes = pi.ng.getNodes().values().toArray(new Node[pi.ng.getNodes().size()]);
		HashSet<Link> allLinks = pi.ng.getLinks();
		links = allLinks.toArray(new Link[allLinks.size()]);

		// Setup HashMaps for object -> index conversion
		nodeIndices = new HashMap<>();
		for (int n = 0; n < nodes.length; n++) {
			nodeIndices.put(nodes[n], n);
		}
		linkIndices = new HashMap<>();
		for (int l = 0; l < links.length; l++) {
			linkIndices.put(links[l], l);
		}
		vnfIndices = new HashMap<>();
		for (int v = 0; v < vnfs.length; v++) {
			vnfIndices.put(vnfs[v], v);
		}

		// Prepare [VNF type] -> [relevant requests] mapping
		typeAssoc = new ArrayList[vnfs.length];
		for (int t = 0; t < typeAssoc.length; t++) {
			typeAssoc[t] = new ArrayList<>();
		}
		for (int r = 0; r < requests.length; r++) {
			for (int f = 0; f < functions[r].length; f++) {
				VNF v = functions[r][f];
				int t = vnfIndices.get(v);
				typeAssoc[t].add(new Integer[]{r, f});
			}
		}
	}

	public abstract void minimizeCpu(String log, String sol, String iis) throws Exception;

	public static int firstFitPacking(ArrayList<Double> elements, double binSize) {
		ArrayList<Double> bins = new ArrayList<>();

		// Sort bandwidth demands (desc):
		elements.sort(Comparator.reverseOrder());

		for (Double d : elements) {
			boolean platzGefunden = false;
			int aktuellerBin = 0;
			while (!platzGefunden) {
				// Current bin does not exist? -> Create new one, add request:
				if (aktuellerBin == bins.size()) {
					bins.add(d);
					platzGefunden = true;
				}
				// Request fits into current bin:
				else if (bins.get(aktuellerBin) + d <= binSize) {
					bins.set(aktuellerBin, bins.get(aktuellerBin) + d);
					platzGefunden = true;
				}
				// Request does not fit, but more bins exist:
				else {
					aktuellerBin++;
				}
			}
		}

		return bins.size();
	}

	public String findPath(Object[][][] arfe, int r, IloCplex... cplex) {
		StringBuilder sb = new StringBuilder();

		Node curr = requests[r].ingress;
		sb.append(curr.name);

		for (int f = 0; f < arfe[r].length; f++) {
			// Count number of links in this sub-path:
			long num = Arrays.stream(arfe[r][f]).filter(a -> isUsed(a, cplex)).count();

			HashSet<Node> visited = new HashSet<>();
			visited.add(curr);
			outer: for (int i = 0; i < num; i++) {
				for (Link l : curr.getNeighbors()) {
					if (!visited.contains(l.getOther(curr))) {
						int e = linkIndices.get(l);
						if (isUsed(arfe[r][f][e], cplex)) {
							Node other = l.getOther(curr);
							sb.append("  ").append(other.name);
							curr = other;
							visited.add(other);
							continue outer;
						}
					}
				}
				sb.append("  ??");
			}

			if (f < arfe[r].length - 1) sb.append("*");
		}

		return sb.toString();
	}

	private boolean isUsed(Object var, IloCplex... cplex) {
		try {
			if (var instanceof GRBVar) {
				return ((GRBVar) var).get(GRB.DoubleAttr.X) > 0.0;
			}
			else if (var instanceof IloNumVar) {
				return cplex[0].getValue((IloNumVar) var) > 0.0;
			}
			else {
				throw new IllegalArgumentException("Required object type: GRBVar or IloNumVar");
			}
		}
		catch (GRBException | IloException e) {
			throw new RuntimeException(e);
		}
	}
}
