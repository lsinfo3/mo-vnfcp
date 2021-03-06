package de.uniwue.VNFP.solvers.gurobi;

import de.uniwue.VNFP.model.*;
import de.uniwue.VNFP.model.factory.TopologyFileReader;
import de.uniwue.VNFP.model.factory.TrafficRequestsReader;
import de.uniwue.VNFP.model.factory.VnfLibReader;
import de.uniwue.VNFP.solvers.AMinimizeCpu;
import gurobi.*;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Locale;
import java.util.stream.Collectors;

public class MinimizeCpuGurobi2 extends AMinimizeCpu {
	public MinimizeCpuGurobi2(ProblemInstance pi) {
		super(pi);
	}

	public static void main(String[] args) throws Exception {
		Locale.setDefault(Locale.US);

		// Read topology and request files.
		//String base = "/home/alex/w/17/benchmark-vnfcp-generator/java/VNFCP_benchmarking/res/eval-topo/";
		//String base = "/home/alex/w/17/benchmark-vnfcp-generator/java/VNFCP_benchmarking/res/msgp/";
		String base = "/home/alex/w/old/ma/java/VNFP/res/problem_instances/BCAB15/";
		//String base = "/home/alex/w/17/benchmark-vnfcp-generator/eval/dynamic/1507907420074/r5/j5/";
		VnfLib lib = VnfLibReader.readFromFile(base + "vnfLib");
		NetworkGraph ng = TopologyFileReader.readFromFile(base + "topology", lib);
		TrafficRequest[] reqs = TrafficRequestsReader.readFromFile(base + "requests-small", ng, lib);
		ProblemInstance pi = new ProblemInstance(ng, lib, reqs, new Objs(lib.getResources()));

		MinimizeCpuGurobi2 minCpu = new MinimizeCpuGurobi2(pi);
		minCpu.minimizeCpu("mip2.log", "mip2.sol", "mip2.ilp");
	}

	@Override
	public void minimizeCpu(String log, String sol, String iis) throws Exception {
		try {
			GRBEnv env = new GRBEnv(log);
			GRBModel model = new GRBModel(env);

			// Create decision variables
			GRBVar[][][] arfe = new GRBVar[requests.length][][];
			for (int r = 0; r < arfe.length; r++) {
				arfe[r] = new GRBVar[functions[r].length + 1][links.length];
				for (int f = 0; f < arfe[r].length; f++) {
					for (int e = 0; e < arfe[r][f].length; e++) {
						arfe[r][f][e] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "arfe[" + r + "][" + f + "][" + e + "]");
					}
				}
			}

			GRBVar[][][][] mrfni = new GRBVar[requests.length][][][];
			for (int r = 0; r < mrfni.length; r++) {
				mrfni[r] = new GRBVar[functions[r].length][nodes.length][];
				for (int f = 0; f < mrfni[r].length; f++) {
					for (int n = 0; n < mrfni[r][f].length; n++) {
						int maxInst_ = maxInst;
						for (int i = 0; i < pi.objectives.TOTAL_USED_RESOURCES.length; i++) {
							maxInst_ = Math.min(maxInst_, (int) Math.floor(nodes[n].resources[i] / functions[r][f].reqResources[i]));
						}

						mrfni[r][f][n] = new GRBVar[maxInst_];
						for (int i = 0; i < mrfni[r][f][n].length; i++) {
							mrfni[r][f][n][i] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "mrfni["+r+"]["+f+"]["+n+"]["+i+"]");
						}
					}
				}
			}

			GRBVar[][][] mtni = new GRBVar[vnfs.length][nodes.length][];
			for (int t = 0; t < mtni.length; t++) {
				for (int n = 0; n < mtni[t].length; n++) {
					int maxInst_ = maxInst;
					for (int i = 0; i < pi.objectives.TOTAL_USED_RESOURCES.length; i++) {
						maxInst_ = Math.min(maxInst_, (int) Math.floor(nodes[n].resources[i] / vnfs[t].reqResources[i]));
					}

					mtni[t][n] = new GRBVar[maxInst_];
					for (int i = 0; i < mtni[t][n].length; i++) {
						mtni[t][n][i] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "mtni["+t+"]["+n+"]["+i+"]");
					}
				}
			}

			// Combine mtni and mrfni variables semantically
			GRBLinExpr expr;
			for (int t = 0; t < mtni.length; t++) {
				ArrayList<Integer[]> listRf = typeAssoc[t];
				if (!listRf.isEmpty()) {
					for (int n = 0; n < mtni[t].length; n++) {
						for (int i = 0; i < mtni[t][n].length; i++) {
							expr = new GRBLinExpr();
							for (Integer[] rf : listRf) {
								expr.addTerm(-1.0, mrfni[rf[0]][rf[1]][n][i]);
							}
							expr.addTerm(listRf.size(), mtni[t][n][i]);
							model.addRange(expr, 0.0, listRf.size() - 1.0, "c4_2");
						}
					}
				}
			}

			// Set objective
			expr = new GRBLinExpr();
			for (int t = 0; t < mtni.length; t++) {
				for (int n = 0; n < mtni[t].length; n++) {
					for (int i = 0; i < mtni[t][n].length; i++) {
						expr.addTerm(100.0 * vnfs[t].reqResources[0], mtni[t][n][i]);
					}
				}
			}
			for (int r = 0; r < arfe.length; r++) {
				for (int f = 0; f < arfe[r].length; f++) {
					for (int e = 0; e < arfe[r][f].length; e++) {
						expr.addTerm(1.0, arfe[r][f][e]);
					}
				}
			}
			model.setObjective(expr, GRB.MINIMIZE);
			//model.set(GRB.DoubleParam.MIPGap, 1.00e-6);

			// Add constraints (2) "exactly one instance and one node for each requested function"
			for (int r = 0; r < mrfni.length; r++) {
				for (int f = 0; f < mrfni[r].length; f++) {
					expr = new GRBLinExpr();
					for (int n = 0; n < mrfni[r][f].length; n++) {
						for (int i = 0; i < mrfni[r][f][n].length; i++) {
							expr.addTerm(1.0, mrfni[r][f][n][i]);
						}
					}
					model.addConstr(expr, GRB.EQUAL, 1.0, "c2");
				}
			}

			if (pi.ng.directed) {
				// Add constraints (3+4) for the directed (or undirected but full-duplex) link case
				for (int r = 0; r < arfe.length; r++) {
					for (int f = 0; f < arfe[r].length; f++) {
						for (int n = 0; n < nodes.length; n++) {
							expr = new GRBLinExpr();

							// Special case: f = 0
							if (f == 0 && nodes[n].equals(requests[r].ingress)) {
								expr.addConstant(1.0);
							}
							// General case
							else if (f > 0) {
								for (int i = 0; i < mrfni[r][f-1][n].length; i++) {
									expr.addTerm(1.0, mrfni[r][f-1][n][i]);
								}
							}

							// Incoming links
							for (Link l : nodes[n].getInLinks()) {
								int e = linkIndices.get(l);
								expr.addTerm(1.0, arfe[r][f][e]);
							}

							// Special case: f = |r_c| + 1
							if (f == mrfni[r].length && nodes[n].equals(requests[r].egress)) {
								expr.addConstant(-1.0);
							}
							// General case
							else if (f < mrfni[r].length) {
								for (int i = 0; i < mrfni[r][f][n].length; i++) {
									expr.addTerm(-1.0, mrfni[r][f][n][i]);
								}
							}

							for (Link l : nodes[n].getOutLinks()) {
								int e = linkIndices.get(l);
								expr.addTerm(-1.0, arfe[r][f][e]);
							}

							model.addConstr(expr, GRB.EQUAL, 0.0, "c3+4");
						}
					}
				}
			}

			else {
				// Add constraints (3) "all used links are connected to endpoints or another link"
				for (int r = 0; r < arfe.length; r++) {
					// Edge case: path from src to first vnf
					for (int e = 0; e < arfe[r][0].length; e++) {
						Link l = links[e];
						int n1 = nodeIndices.get(l.node1);
						int n2 = nodeIndices.get(l.node2);

						for (int nn : new int[]{n1, n2}) {
							expr = new GRBLinExpr();
							expr.addTerm(-1.0, arfe[r][0][e]);
							for (int i = 0; i < mrfni[r][0][nn].length; i++) {
								expr.addTerm(1.0, mrfni[r][0][nn][i]);
							}
							for (Link ll : nodes[nn].getNeighbors()) {
								int lll = linkIndices.get(ll);
								if (lll != e) {
									expr.addTerm(1.0, arfe[r][0][lll]);
								}
							}
							model.addConstr(expr, GRB.GREATER_EQUAL, (requests[r].ingress.equals(nodes[nn]) ? -1.0 : 0.0), "c3_1");
						}
					}

					// VNF f-1 to VNF f
					for (int f = 1; f < arfe[r].length - 1; f++) {
						for (int e = 0; e < arfe[r][f].length; e++) {
							Link l = links[e];
							int n1 = nodeIndices.get(l.node1);
							int n2 = nodeIndices.get(l.node2);

							for (int nn : new int[]{n1, n2}) {
								expr = new GRBLinExpr();
								expr.addTerm(-1.0, arfe[r][f][e]);
								for (int i = 0; i < mrfni[r][f][nn].length; i++) {
									expr.addTerm(1.0, mrfni[r][f][nn][i]);
								}
								for (int i = 0; i < mrfni[r][f-1][nn].length; i++) {
									expr.addTerm(1.0, mrfni[r][f-1][nn][i]);
								}
								for (Link ll : nodes[nn].getNeighbors()) {
									int lll = linkIndices.get(ll);
									if (lll != e) {
										expr.addTerm(1.0, arfe[r][f][lll]);
									}
								}
								model.addConstr(expr, GRB.GREATER_EQUAL, 0.0, "c3_2");
							}
						}
					}

					// Edge case: path from last vnf to dst
					int dst = arfe[r].length - 1;
					for (int e = 0; e < arfe[r][dst].length; e++) {
						Link l = links[e];
						int n1 = nodeIndices.get(l.node1);
						int n2 = nodeIndices.get(l.node2);

						for (int nn : new int[]{n1, n2}) {
							expr = new GRBLinExpr();
							expr.addTerm(-1.0, arfe[r][dst][e]);
							for (int i = 0; i < mrfni[r][dst - 1][nn].length; i++) {
								expr.addTerm(1.0, mrfni[r][dst - 1][nn][i]);
							}
							for (Link ll : nodes[nn].getNeighbors()) {
								int lll = linkIndices.get(ll);
								if (lll != e) {
									expr.addTerm(1.0, arfe[r][dst][lll]);
								}
							}
							model.addConstr(expr, GRB.GREATER_EQUAL, (requests[r].egress.equals(nodes[nn]) ? -1.0 : 0.0), "c3_3");
						}
					}
				}

				// Add constraints (4) "source/destination points must be connected to the paths"
				for (int r = 0; r < mrfni.length; r++) {
					// Edge case: path from src to first vnf
					for (int n = 0; n < nodes.length; n++) {
						expr = new GRBLinExpr();
						for (int i = 0; i < mrfni[r][0][n].length; i++) {
							expr.addTerm(-1.0, mrfni[r][0][n][i]);
						}
						for (Link ll : nodes[n].getNeighbors()) {
							expr.addTerm(1.0, arfe[r][0][linkIndices.get(ll)]);
						}
						model.addConstr(expr, GRB.GREATER_EQUAL, (requests[r].ingress.equals(nodes[n]) ? -1.0 : 0.0), "c4_1");

						expr = new GRBLinExpr();
						for (int i = 0; i < mrfni[r][0][n].length; i++) {
							expr.addTerm(1.0, mrfni[r][0][n][i]);
						}
						for (Link ll : nodes[n].getNeighbors()) {
							expr.addTerm(1.0, arfe[r][0][linkIndices.get(ll)]);
						}
						model.addConstr(expr, GRB.GREATER_EQUAL, (requests[r].ingress.equals(nodes[n]) ? 1.0 : 0.0), "c4_1");
					}

					// VNF f-1 to VNF f
					for (int f = 1; f < mrfni[r].length; f++) {
						for (int n = 0; n < mrfni[r][f].length; n++) {
							expr = new GRBLinExpr();
							for (int i = 0; i < mrfni[r][f][n].length; i++) {
								expr.addTerm(-1.0, mrfni[r][f][n][i]);
							}
							for (int i = 0; i < mrfni[r][f-1][n].length; i++) {
								expr.addTerm(1.0, mrfni[r][f-1][n][i]);
							}
							for (Link ll : nodes[n].getNeighbors()) {
								expr.addTerm(1.0, arfe[r][f][linkIndices.get(ll)]);
							}
							model.addConstr(expr, GRB.GREATER_EQUAL, 0.0, "c4_2");

							expr = new GRBLinExpr();
							for (int i = 0; i < mrfni[r][f][n].length; i++) {
								expr.addTerm(1.0, mrfni[r][f][n][i]);
							}
							for (int i = 0; i < mrfni[r][f-1][n].length; i++) {
								expr.addTerm(-1.0, mrfni[r][f-1][n][i]);
							}
							for (Link ll : nodes[n].getNeighbors()) {
								expr.addTerm(1.0, arfe[r][f][linkIndices.get(ll)]);
							}
							model.addConstr(expr, GRB.GREATER_EQUAL, 0.0, "c4_2");
						}
					}

					// Edge case: path from last vnf to dst
					int last = requests[r].vnfSequence.length - 1;
					for (int n = 0; n < nodes.length; n++) {
						expr = new GRBLinExpr();
						for (int i = 0; i < mrfni[r][last][n].length; i++) {
							expr.addTerm(1.0, mrfni[r][last][n][i]);
						}
						for (Link ll : nodes[n].getNeighbors()) {
							expr.addTerm(1.0, arfe[r][last + 1][linkIndices.get(ll)]);
						}
						model.addConstr(expr, GRB.GREATER_EQUAL, (requests[r].egress.equals(nodes[n]) ? 1.0 : 0.0), "c4_3");

						expr = new GRBLinExpr();
						for (int i = 0; i < mrfni[r][last][n].length; i++) {
							expr.addTerm(-1.0, mrfni[r][last][n][i]);
						}
						for (Link ll : nodes[n].getNeighbors()) {
							expr.addTerm(1.0, arfe[r][last + 1][linkIndices.get(ll)]);
						}
						model.addConstr(expr, GRB.GREATER_EQUAL, (requests[r].egress.equals(nodes[n]) ? -1.0 : 0.0), "c4_3");
					}
				}
			}

			// Add constraints (5) "respect available computational resources on nodes"
			for (int n = 0; n < nodes.length; n++) {
				for (int j = 0; j < pi.objectives.TOTAL_USED_RESOURCES.length; j++) {
					expr = new GRBLinExpr();
					for (int t = 0; t < vnfs.length; t++) {
						for (int i = 0; i < mtni[t][n].length; i++) {
							expr.addTerm(vnfs[t].reqResources[j], mtni[t][n][i]);
						}
					}
					model.addConstr(expr, GRB.LESS_EQUAL, nodes[n].resources[j], "c5_"+j);
				}
			}

			// Add constraints (6) "respect available bandwidth on links"
			for (int e = 0; e < links.length; e++) {
				expr = new GRBLinExpr();
				for (int r = 0; r < arfe.length; r++) {
					for (int f = 0; f < arfe[r].length; f++) {
						expr.addTerm(requests[r].bandwidthDemand, arfe[r][f][e]);
					}
				}
				model.addConstr(expr, GRB.LESS_EQUAL, links[e].bandwidth, "c6");
			}

			// Add constraints (7) "respect available capacity of VNFs"
			for (int t = 0; t < vnfs.length; t++) {
				ArrayList<Integer[]> listRf = typeAssoc[t];
				for (int n = 0; n < nodes.length; n++) {
					for (int i = 0; i < maxInst; i++) {
						expr = new GRBLinExpr();
						for (Integer[] rf : listRf) {
							if (i < mrfni[rf[0]][rf[1]][n].length) {
								expr.addTerm(requests[rf[0]].bandwidthDemand, mrfni[rf[0]][rf[1]][n][i]);
							}
						}
						model.addConstr(expr, GRB.LESS_EQUAL, vnfs[t].processingCapacity, "c7");
					}
				}
			}

			// Add constraints (8) "respect maximum delay of r"
			for (int r = 0; r < requests.length; r++) {
				expr = new GRBLinExpr();
				double vnfDelay = 0.0;
				for (int f = 0; f < requests[r].vnfSequence.length; f++) {
					for (int e = 0; e < links.length; e++) {
						expr.addTerm(links[e].delay, arfe[r][f][e]);
					}
					vnfDelay += requests[r].vnfSequence[f].delay;
				}
				model.addConstr(expr, GRB.LESS_EQUAL, requests[r].expectedDelay - vnfDelay, "c8");
			}

			// Optimize model
			model.optimize();
			PrintStream out = new PrintStream(new FileOutputStream(sol));
			//PrintStream out = System.out;

			if (model.get(GRB.IntAttr.SolCount) > 0) {
				out.println("Feasible solution found!");

				out.println("Instances:");
				double cores = 0.0;
				for (int t = 0; t < mtni.length; t++) {
					for (int n = 0; n < mtni[t].length; n++) {
						int count = 0;
						for (int i = 0; i < mtni[t][n].length; i++) {
							if (mtni[t][n][i].get(GRB.DoubleAttr.X) > 0.0) {
								count++;
							}
						}
						if (count > 0) {
							out.println(String.format("  %s: [%dx %s]", nodes[n].name, count, vnfs[t].name));
							cores += count * vnfs[t].reqResources[0];
						}
					}
				}
				out.println("Used CPU cores: " + cores);

				double numHops = 0.0;
				for (int r = 0; r < arfe.length; r++) {
					for (int f = 0; f < arfe[r].length; f++) {
						for (int e = 0; e < arfe[r][f].length; e++) {
							numHops += arfe[r][f][e].get(GRB.DoubleAttr.X);
						}
					}
				}
				out.println("Number of hops: " + numHops);

				out.println("Paths:");
				for (int r = 0; r < requests.length; r++) {
					out.println("  [" + r + "]: " + findPath(arfe, r));
				}
			}
			else {
				model.computeIIS();
				model.write(iis);

				out.print("\n\nConflicting constraints (check output file):\n ");
				for (GRBConstr c : model.getConstrs()) {
					if (c.get(GRB.IntAttr.IISConstr) > 0) {
						out.print(" " + c.get(GRB.StringAttr.ConstrName));
					}
				}
				for (GRBGenConstr c : model.getGenConstrs()) {
					if (c.get(GRB.IntAttr.IISGenConstr) > 0) {
						out.print(" " + c.get(GRB.StringAttr.GenConstrName));
					}
				}
				out.println();

				out.println("Nodes (+Neighbors):");
				for (int n = 0; n < nodes.length; n++) {
					out.print("  [" + n + "]: '" + nodes[n].name + "'   (");
					int n_ = n;
					out.print(nodes[n].getNeighbors().stream().map(l -> l.getOther(nodes[n_]).name).collect(Collectors.joining(",")));
					out.print(")   (");
					out.print(nodes[n].getNeighbors().stream().map(l -> "" + nodeIndices.get(l.getOther(nodes[n_]))).collect(Collectors.joining(",")));
					out.println(")");
				}

				out.println("Links:");
				for (int l = 0; l < links.length; l++) {
					out.print("  [" + l + "]: " + links[l].node1.name + " -> " + links[l].node2.name + "   ");
					out.println("(" + nodeIndices.get(links[l].node1) + " -> " + nodeIndices.get(links[l].node2) + ")");
				}

				out.println("VNFs:");
				for (int v = 0; v < vnfs.length; v++) {
					out.println("  [" + v + "]: '" + vnfs[v].name + "'");
				}

				out.println("Requests:");
				for (int r = 0; r < requests.length; r++) {
					out.println("  [" + r + "]: '" + requests[r].ingress.name + "' -> '" + requests[r].egress.name + "' (or " + nodeIndices.get(requests[r].ingress) + "->" + nodeIndices.get(requests[r].egress) + ")");
				}
			}

			// Dispose of model and environment
			model.dispose();
			env.dispose();
		}
		catch (GRBException e) {
			System.out.println("\nError code: " + e.getErrorCode() + ". " + e.getMessage());
			throw new RuntimeException(e);
		}
	}
}
