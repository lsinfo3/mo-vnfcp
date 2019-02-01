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

public class MinimizeCpuGurobi extends AMinimizeCpu {
    public MinimizeCpuGurobi(ProblemInstance pi) {
        super(pi);
    }

    @Override
    public void minimizeCpu(String log, String sol, String iis) throws Exception {
        try {
            GRBEnv    env   = new GRBEnv(log);
            GRBModel  model = new GRBModel(env);

            // Create decision variables
            GRBVar[][][] zrfi = new GRBVar[requests.length][][];
            for (int r = 0; r < zrfi.length; r++) {
                zrfi[r] = new GRBVar[functions[r].length][maxInst];
                for (int f = 0; f < zrfi[r].length; f++) {
                    for (int i = 0; i < zrfi[r][f].length; i++) {
                        zrfi[r][f][i] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "zrfi["+r+"]["+f+"]["+i+"]");
                    }
                }
            }

            GRBVar[][][] crfn = new GRBVar[requests.length][][];
            for (int r = 0; r < crfn.length; r++) {
                crfn[r] = new GRBVar[functions[r].length][nodes.length];
                for (int f = 0; f < crfn[r].length; f++) {
                    for (int n = 0; n < crfn[r][f].length; n++) {
                        crfn[r][f][n] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "crfn["+r+"]["+f+"]["+n+"]");
                    }
                }
            }

            GRBVar[][][] arfe = new GRBVar[requests.length][][];
            for (int r = 0; r < arfe.length; r++) {
                arfe[r] = new GRBVar[functions[r].length + 1][links.length];
                for (int f = 0; f < arfe[r].length; f++) {
                    for (int e = 0; e < arfe[r][f].length; e++) {
                        arfe[r][f][e] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "arfe["+r+"]["+f+"]["+e+"]");
                    }
                }
            }

            GRBVar[][][][] mrfni = new GRBVar[requests.length][][][];
            for (int r = 0; r < mrfni.length; r++) {
                mrfni[r] = new GRBVar[functions[r].length][nodes.length][maxInst];
                for (int f = 0; f < mrfni[r].length; f++) {
                    for (int n = 0; n < mrfni[r][f].length; n++) {
                        for (int i = 0; i < mrfni[r][f][n].length; i++) {
                            mrfni[r][f][n][i] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "mrfni["+r+"]["+f+"]["+n+"]["+i+"]");
                        }
                    }
                }
            }

            GRBVar[][][] mtni = new GRBVar[vnfs.length][nodes.length][maxInst];
            for (int t = 0; t < mtni.length; t++) {
                for (int n = 0; n < mtni[t].length; n++) {
                    for (int i = 0; i < mtni[t][n].length; i++) {
                        mtni[t][n][i] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "mtni["+t+"]["+n+"]["+i+"]");
                    }
                }
            }

            GRBVar[][] mtn = new GRBVar[vnfs.length][nodes.length];
            for (int t = 0; t < mtn.length; t++) {
                for (int n = 0; n < mtn[t].length; n++) {
                    mtn[t][n] = model.addVar(0.0, Double.MAX_VALUE, 0.0, GRB.CONTINUOUS, "mtn["+t+"]["+n+"]");
                }
            }

            // Set objective
            GRBLinExpr expr = new GRBLinExpr();
            for (int t = 0; t < vnfs.length; t++) {
                for (int n = 0; n < nodes.length; n++) {
                    expr.addTerm(100.0 * vnfs[t].reqResources[0], mtn[t][n]);
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

            // Add constraints for auxiliary variables "count number of instances"
            for (int r = 0; r < mrfni.length; r++) {
                for (int f = 0; f < mrfni[r].length; f++) {
                    for (int n = 0; n < mrfni[r][f].length; n++) {
                        for (int i = 0; i < mrfni[r][f][n].length; i++) {
                            //model.addGenConstrAnd(mrfni[r][f][n][i], new GRBVar[]{crfn[r][f][n], zrfi[r][f][i]}, "c4_1");

                            expr = new GRBLinExpr();
                            expr.addTerm(1.0, crfn[r][f][n]);
                            expr.addTerm(1.0, zrfi[r][f][i]);
                            expr.addTerm(-2.0, mrfni[r][f][n][i]);
                            model.addRange(expr, 0.0, 1.0, "c4_1");
                        }
                    }
                }
            }
            for (int t = 0; t < mtni.length; t++) {
                ArrayList<Integer[]> listRf = typeAssoc[t];
                if (!listRf.isEmpty()) {
                    for (int n = 0; n < mtni[t].length; n++) {
                        for (int i = 0; i < mtni[t][n].length; i++) {
                            //final int n_ = n;
                            //final int i_ = i;
                            //model.addGenConstrOr(mtni[t][n][i], listRf.stream().map(rf -> mrfni[rf[0]][rf[1]][n_][i_]).toArray(GRBVar[]::new), "c4_2");

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
            for (int t = 0; t < mtni.length; t++) {
                for (int n = 0; n < mtni[t].length; n++) {
                    expr = new GRBLinExpr();
                    expr.addTerm(-1.0, mtn[t][n]);
                    for (int i = 0; i < mtni[t][n].length; i++) {
                        expr.addTerm(1.0, mtni[t][n][i]);
                    }
                    model.addConstr(expr, GRB.EQUAL, 0.0, "c4_3");
                }
            }

            // Add constraints (2) "exactly one instance and one node for each requested function"
            for (int r = 0; r < zrfi.length; r++) {
                for (int f = 0; f < zrfi[r].length; f++) {
                    expr = new GRBLinExpr();
                    for (int i = 0; i < zrfi[r][f].length; i++) {
                        expr.addTerm(1.0, zrfi[r][f][i]);
                    }
                    model.addConstr(expr, GRB.EQUAL, 1.0, "c1_1");

                    expr = new GRBLinExpr();
                    for (int n = 0; n < crfn[r][f].length; n++) {
                        expr.addTerm(1.0, crfn[r][f][n]);
                    }
                    model.addConstr(expr, GRB.EQUAL, 1.0, "c1_2");
                }
            }

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
                        expr.addTerm(1.0, crfn[r][0][nn]);
                        for (Link ll : nodes[nn].getNeighbors()) {
                            int lll = linkIndices.get(ll);
                            if (lll != e) {
                                expr.addTerm(1.0, arfe[r][0][lll]);
                            }
                        }
                        model.addConstr(expr, GRB.GREATER_EQUAL, (requests[r].ingress.equals(nodes[nn]) ? -1.0 : 0.0), "c2_1");
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
                            expr.addTerm(1.0, crfn[r][f][nn]);
                            expr.addTerm(1.0, crfn[r][f-1][nn]);
                            for (Link ll : nodes[nn].getNeighbors()) {
                                int lll = linkIndices.get(ll);
                                if (lll != e) {
                                    expr.addTerm(1.0, arfe[r][f][lll]);
                                }
                            }
                            model.addConstr(expr, GRB.GREATER_EQUAL, 0.0, "c2_2");
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
                        expr.addTerm(1.0, crfn[r][dst-1][nn]);
                        for (Link ll : nodes[nn].getNeighbors()) {
                            int lll = linkIndices.get(ll);
                            if (lll != e) {
                                expr.addTerm(1.0, arfe[r][dst][lll]);
                            }
                        }
                        model.addConstr(expr, GRB.GREATER_EQUAL, (requests[r].egress.equals(nodes[nn]) ? -1.0 : 0.0), "c2_3");
                    }
                }
            }

            // Add constraints (4) "source/destination points must be connected to the paths"
            for (int r = 0; r < crfn.length; r++) {
                // Edge case: path from src to first vnf
                for (int n = 0; n < nodes.length; n++) {
                    expr = new GRBLinExpr();
                    expr.addTerm(-1.0, crfn[r][0][n]);
                    for (Link ll : nodes[n].getNeighbors()) {
                        expr.addTerm(1.0, arfe[r][0][linkIndices.get(ll)]);
                    }
                    model.addConstr(expr, GRB.GREATER_EQUAL, (requests[r].ingress.equals(nodes[n]) ? -1.0 : 0.0), "c3_1");

                    expr = new GRBLinExpr();
                    expr.addTerm(1.0, crfn[r][0][n]);
                    for (Link ll : nodes[n].getNeighbors()) {
                        expr.addTerm(1.0, arfe[r][0][linkIndices.get(ll)]);
                    }
                    model.addConstr(expr, GRB.GREATER_EQUAL, (requests[r].ingress.equals(nodes[n]) ? 1.0 : 0.0), "c3_1");
                }

                // VNF f-1 to VNF f
                for (int f = 1; f < crfn[r].length; f++) {
                    for (int n = 0; n < crfn[r][f].length; n++) {
                        expr = new GRBLinExpr();
                        expr.addTerm(-1.0, crfn[r][f][n]);
                        expr.addTerm(1.0, crfn[r][f-1][n]);
                        for (Link ll : nodes[n].getNeighbors()) {
                            expr.addTerm(1.0, arfe[r][f][linkIndices.get(ll)]);
                        }
                        model.addConstr(expr, GRB.GREATER_EQUAL, 0.0, "c3_2");

                        expr = new GRBLinExpr();
                        expr.addTerm(1.0, crfn[r][f][n]);
                        expr.addTerm(-1.0, crfn[r][f-1][n]);
                        for (Link ll : nodes[n].getNeighbors()) {
                            expr.addTerm(1.0, arfe[r][f][linkIndices.get(ll)]);
                        }
                        model.addConstr(expr, GRB.GREATER_EQUAL, 0.0, "c3_2");
                    }
                }

                // Edge case: path from last vnf to dst
                int last = requests[r].vnfSequence.length - 1;
                for (int n = 0; n < nodes.length; n++) {
                    expr = new GRBLinExpr();
                    expr.addTerm(1.0, crfn[r][last][n]);
                    for (Link ll : nodes[n].getNeighbors()) {
                        expr.addTerm(1.0, arfe[r][last+1][linkIndices.get(ll)]);
                    }
                    model.addConstr(expr, GRB.GREATER_EQUAL, (requests[r].egress.equals(nodes[n]) ? 1.0 : 0.0), "c3_3");

                    expr = new GRBLinExpr();
                    expr.addTerm(-1.0, crfn[r][last][n]);
                    for (Link ll : nodes[n].getNeighbors()) {
                        expr.addTerm(1.0, arfe[r][last+1][linkIndices.get(ll)]);
                    }
                    model.addConstr(expr, GRB.GREATER_EQUAL, (requests[r].egress.equals(nodes[n]) ? -1.0 : 0.0), "c3_3");
                }
            }

            // Add constraints (5) "respect available computational resources on nodes"
            for (int n = 0; n < nodes.length; n++) {
                for (int j = 0; j < pi.objectives.TOTAL_USED_RESOURCES.length; j++) {
                    expr = new GRBLinExpr();
                    for (int t = 0; t < vnfs.length; t++) {
                        expr.addTerm(vnfs[t].reqResources[j], mtn[t][n]);
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
                            expr.addTerm(requests[rf[0]].bandwidthDemand, mrfni[rf[0]][rf[1]][n][i]);
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
                for (int n = 0; n < nodes.length; n++) {
                    for (int t = 0; t < vnfs.length; t++) {
                        if (mtn[t][n].get(GRB.DoubleAttr.X) > 0.0) {
                            out.println(String.format("  %s: [%.0fx %s]", nodes[n].name, mtn[t][n].get(GRB.DoubleAttr.X), vnfs[t].name));
                        }
                    }
                }

                double cores = 0.0;
                for (int n = 0; n < nodes.length; n++) {
                    for (int t = 0; t < vnfs.length; t++) {
                        cores += mtn[t][n].get(GRB.DoubleAttr.X) * vnfs[t].reqResources[0];
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
                    out.println("  ["+r+"]: " + findPath(arfe, r));
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
                    out.print("  ["+n+"]: '" + nodes[n].name + "'   (");
                    int n_ = n;
                    out.print(nodes[n].getNeighbors().stream().map(l -> l.getOther(nodes[n_]).name).collect(Collectors.joining(",")));
                    out.print(")   (");
                    out.print(nodes[n].getNeighbors().stream().map(l -> ""+nodeIndices.get(l.getOther(nodes[n_]))).collect(Collectors.joining(",")));
                    out.println(")");
                }

                out.println("Links:");
                for (int l = 0; l < links.length; l++) {
                    out.print("  ["+l+"]: " + links[l].node1.name + " -> " + links[l].node2.name + "   ");
                    out.println("(" + nodeIndices.get(links[l].node1) + " -> " + nodeIndices.get(links[l].node2) + ")");
                }

                out.println("VNFs:");
                for (int v = 0; v < vnfs.length; v++) {
                    out.println("  ["+v+"]: '" + vnfs[v].name + "'");
                }

                out.println("Requests:");
                for (int r = 0; r < requests.length; r++) {
                    out.println("  ["+r+"]: '" + requests[r].ingress.name + "' -> '" + requests[r].egress.name + "' (or "+nodeIndices.get(requests[r].ingress)+"->"+nodeIndices.get(requests[r].egress)+")");
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

    public static void exampleModel() {
        try {
            GRBEnv    env   = new GRBEnv("mip1.log");
            GRBModel  model = new GRBModel(env);

            // Create variables
            GRBVar x = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "x");
            GRBVar y = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "y");
            GRBVar z = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "z");
            GRBVar a = model.addVar(-2.0, 2.0, 0.0, GRB.CONTINUOUS, "a");
            GRBVar b = model.addVar(-2.0, 2.0, 0.0, GRB.CONTINUOUS, "b");
            GRBVar c = model.addVar(-2.0, 2.0, 0.0, GRB.CONTINUOUS, "c");

            // Set objective: maximize x + y + 2 z
            GRBLinExpr expr = new GRBLinExpr();
            expr.addTerm(1.0, x); expr.addTerm(1.0, y); expr.addTerm(2.0, z);
            model.setObjective(expr, GRB.MAXIMIZE);

            // Add constraint: x + y >= 1
            expr = new GRBLinExpr();
            expr.addTerm(1.0, x); expr.addTerm(1.0, y);
            model.addConstr(expr, GRB.GREATER_EQUAL, 1.0, "c1");

            // Add constraint: a == -1
            expr = new GRBLinExpr();
            expr.addTerm(1.0, a);
            model.addConstr(expr, GRB.EQUAL, -1.0, "c2");

            // Add constraint: b == 1
            expr = new GRBLinExpr();
            expr.addTerm(1.0, b);
            model.addConstr(expr, GRB.EQUAL, 1.0, "c3");

            // Add constraint: c == a OR b
            model.addGenConstrOr(c, new GRBVar[]{a, b}, "c4");

            model.optimize();

            for (GRBVar v : new GRBVar[]{x, y, z, a, b, c}) {
                System.out.println(v.get(GRB.StringAttr.VarName) + " " + v.get(GRB.DoubleAttr.X));
            }

            System.out.println("Obj: " + model.get(GRB.DoubleAttr.ObjVal));

            model.dispose();
            env.dispose();

        }
        catch (GRBException e) {
            System.out.println("Error code: " + e.getErrorCode() + ". " +
                    e.getMessage());
        }
    }

    public static void main(String[] args) throws Exception {
        Locale.setDefault(Locale.US);

        // Read topology and request files.
        //String base = "/home/alex/w/17/benchmark-vnfcp-generator/java/VNFCP_benchmarking/res/eval-topo/";
        //String base = "/home/alex/w/17/benchmark-vnfcp-generator/java/VNFCP_benchmarking/res/msgp/";
        String base = "/home/alex/w/misc/ma/java/VNFP/res/problem_instances/BCAB15/";
        //String base = "/home/alex/w/17/benchmark-vnfcp-generator/eval/dynamic/1507907420074/r5/j5/";
        VnfLib lib = VnfLibReader.readFromFile(base + "vnfLib");
        NetworkGraph ng = TopologyFileReader.readFromFile(base + "topology", lib);
        TrafficRequest[] reqs = TrafficRequestsReader.readFromFile(base + "requests-small", ng, lib);
        ProblemInstance pi = new ProblemInstance(ng, lib, reqs, new Objs(lib.getResources()));

        MinimizeCpuGurobi minCpu = new MinimizeCpuGurobi(pi);
        minCpu.minimizeCpu("mip2.log", "mip2.sol", "mip2.ilp");
        //exampleModel();
    }
}
