import de.uniwue.VNFP.algo.GreedyCentrality;
import de.uniwue.VNFP.algo.PSA;
import de.uniwue.VNFP.algo.ParetoFrontier;
import de.uniwue.VNFP.gui.GuiApp;
import de.uniwue.VNFP.model.NetworkGraph;
import de.uniwue.VNFP.model.Node;
import de.uniwue.VNFP.model.TrafficRequest;
import de.uniwue.VNFP.model.VnfLib;
import de.uniwue.VNFP.model.factory.TopologyFileReader;
import de.uniwue.VNFP.model.factory.TrafficRequestsReader;
import de.uniwue.VNFP.model.factory.ViterbiSolutionReader;
import de.uniwue.VNFP.model.factory.VnfLibReader;
import de.uniwue.VNFP.model.solution.Solution;
import de.uniwue.VNFP.util.Config;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Epic class for epic things.
 * (This generic message was probably left here because
 * this file's name is self-explanatory.)
 *
 * @author alex
 */
public class Main {
    public static void main(String[] args) throws Exception {
        Locale.setDefault(Locale.US);

        //Comparison.main(args);
        //ParameterEval.main(args);
        runPSA(args.length > 0 ? args[0] : null);
        //printTopologyDetails();
        //testGreedy();
        //testScriptEngine();
        //checkRequests();
        //testCplexSolution();
        //createRandomVnfSeqs(462);
        //createMaxDelayFromTopology();
    }

    public static void printTopologyDetails() throws Exception {
        String problem = "BCAB15";
        String input_topology = "res/problem_instances/"+problem+"/topology";
        String input_requests = "res/problem_instances/"+problem+"/requests";
        String input_vnfs = "res/problem_instances/"+problem+"/vnfLib";

        NetworkGraph ng = TopologyFileReader.readFromFile(input_topology);
        VnfLib vnfLib = VnfLibReader.readFromFile(input_vnfs);
        TrafficRequest[] reqs = TrafficRequestsReader.readFromFile(input_requests, ng, vnfLib);

        //System.out.println(ng.toDotFile());

        System.out.println(Arrays.stream(reqs)
                .map(r -> ""+r.expectedDelay)
                .collect(Collectors.joining(",")));
        System.out.println(Arrays.stream(reqs)
                .map(r -> ""+r.expectedDelay/(r.getShortestDelay(ng.getDijkstraBackpointers())
                        + Arrays.stream(r.vnfSequence).mapToDouble(t -> t.delay).sum()))
                .collect(Collectors.joining(",")));
        System.out.println(Arrays.stream(reqs)
                .map(r -> ""+r.bandwidthDemand)
                .collect(Collectors.joining(",")));

    }

    public static void runPSA(String configPath) throws Exception {
        if (configPath == null) {
            configPath = "config.js";
        }
        if (!Files.exists(Paths.get(configPath))) {
            System.out.println("Config file '" + configPath + "' not found.");
            System.out.println("Creating default config file in:");
            System.out.println(Paths.get(configPath).toAbsolutePath().toString());

            Config.getInstance().writeConfig(new FileOutputStream(configPath));

            System.out.println("... done. Please review the configuration and run this program again.");
            return;
        }
        else {
            Config.getInstance(new FileInputStream(configPath));
        }
        Config c = Config.getInstance();

        NetworkGraph ng = TopologyFileReader.readFromFile(c.topologyFile);
        VnfLib vnfLib = VnfLibReader.readFromFile(c.vnfLibFile);
        TrafficRequest[] reqs = TrafficRequestsReader.readFromFile(c.requestsFile, ng, vnfLib);

        PSA psa = new PSA(ng, reqs, c.s, c.m, c.tmax, c.tmin, c.rho, c.runtime);
        c.createAllEventLoggers().forEach(psa::addEventLogger);

        ParetoFrontier front;
        switch (Config.getInstance().prepMode) {
            case LEAST_CPU:
                front = psa.runPSAPrepCpu();
                break;
            case LEAST_DELAY:
                front = psa.runPSAPrepDelay();
                break;
            case SHORT_PSA:
                front = psa.runPSAPrepPSA();
                break;
            default:
                front = psa.runPSARand();
        }

        if (Config.getInstance().showGui) {
            GuiApp.frontier = front;
            GuiApp.launch(GuiApp.class);
        }
    }

    public static void testGreedy() throws Exception {
        String input_topology = "res/problem_instances/germany2/topology";
        String input_requests = "res/problem_instances/germany2/requests";
        String input_vnfs = "res/problem_instances/germany2/vnfLib";

        NetworkGraph ng = TopologyFileReader.readFromFile(input_topology);
        VnfLib vnfLib = VnfLibReader.readFromFile(input_vnfs);
        TrafficRequest[] reqs = TrafficRequestsReader.readFromFile(input_requests, ng, vnfLib);

        //System.out.println(ng.toDotFile());

        Solution s = GreedyCentrality.centrality(ng, reqs);
        System.out.println(s);
        s.printDebugOutput();
    }

    public static void testScriptEngine() throws Exception {
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine js = sem.getEngineByName("JavaScript");
        js.eval("blubb = 1\n" +
                "muh = blubb + 2\n" +
                "// just a comment...\n" +
                "crap = \"asd\"");
        System.out.println(js.get("muh").getClass());

        System.out.println("Testing ScriptEngine Performance...");
        long start = System.currentTimeMillis();
        Config c = Config.getInstance(new FileInputStream("res/customConfig.js"));
        double[] d = new double[]{1.0, 7.5, 1.3+2.5+6.3+3.1, 6.0};;
        for (int i = 0; i < 100000; i++) {
            //c.pReassignVnf(i);
            //d = new double[]{d[0] + 1.0, d[1] - 0.5, d[0] + d[1], d[3] * 1.1};
            c.objectiveVector(new double[Solution.Vals.values().length]);
        }
        System.out.println(Arrays.toString(d));
        long dur = System.currentTimeMillis() - start;
        System.out.println("Duration: " + dur + "ms");
        System.out.println(c.topologyFile.toAbsolutePath().toString());
    }

    public static void testCplexSolution() throws Exception {
        String p = "germany";
        String input_topology = "res/problem_instances/"+p+"/topology-fixed-psa";
        String input_requests = "/home/alex/w/MA/eval/compare/1482855182613/germany/0/psa-requests";
        String input_cplex_paths = "res/problem_instances/"+p+"/log.cplex.paths";
        String input_cplex_seqs = "res/problem_instances/"+p+"/log.cplex.sequences";
        String input_viterbi = "/home/alex/Desktop/check_energy_cost/clean/log.sequences";
        String input_vnfs = "res/problem_instances/"+p+"/vnfLib";

        NetworkGraph ng = TopologyFileReader.readFromFile(input_topology);
        VnfLib vnfLib = VnfLibReader.readFromFile(input_vnfs);
        TrafficRequest[] reqs = TrafficRequestsReader.readFromFile(input_requests, ng, vnfLib);

        //Solution reference = CplexSolutionReader.readFromCsv(ng, reqs, input_cplex_paths, input_cplex_seqs);
        Solution reference = ViterbiSolutionReader.readFromCsv(ng, reqs, input_viterbi);
        System.out.println(reference);
        reference.printDebugOutput();
    }

    public static void checkRequests() throws Exception {
        String input_topology = "res/problem_instances/geant/topology";
        String input_requests = "res/problem_instances/geant/requests";
        String input_vnfs = "res/problem_instances/geant/vnfLib";

        NetworkGraph ng = TopologyFileReader.readFromFile(input_topology);
        System.out.println(ng.toDotFile());

        System.out.println("\n\n");

        VnfLib vnfLib = VnfLibReader.readFromFile(input_vnfs);
        TrafficRequest[] reqs = TrafficRequestsReader.readFromFile(input_requests, ng, vnfLib);
        //System.out.println(Arrays.toString(reqs).replace(", TrafficRequest", ",\nTrafficRequest"));
        //Arrays.stream(reqs).forEach(p -> System.out.println(p.toOldCsvFormat()));

        double sumBandwidth = 0.0;
        double sumFirewall = 0.0;
        double sumNat = 0.0;
        double sumProxy = 0.0;
        double sumIDS = 0.0;
        for (TrafficRequest req : reqs) {
            sumBandwidth += req.bandwidthDemand;
            if (Arrays.asList(req.vnfSequence).contains(vnfLib.fromString("firewall")[0])) sumFirewall += req.bandwidthDemand;
            if (Arrays.asList(req.vnfSequence).contains(vnfLib.fromString("nat")[0])) sumNat += req.bandwidthDemand;
            if (Arrays.asList(req.vnfSequence).contains(vnfLib.fromString("proxy")[0])) sumProxy += req.bandwidthDemand;
            if (Arrays.asList(req.vnfSequence).contains(vnfLib.fromString("ids_1")[0])) sumIDS += req.bandwidthDemand;
            if (Arrays.asList(req.vnfSequence).contains(vnfLib.fromString("ids_2")[0])) sumIDS += req.bandwidthDemand;
        }
        System.out.println("sumBandwidth = " + sumBandwidth);
        System.out.println("sumBandwidthFirewall = " + sumFirewall);
        System.out.println("sumBandwidthNAT = " + sumNat);
        System.out.println("sumBandwidthProxy = " + sumProxy);
        System.out.println("sumBandwidthIDS = " + sumIDS);
        System.out.println("sumAll = " + (sumFirewall + sumNat + sumProxy + sumIDS));
    }

    public static void createRandomVnfSeqs(int number) throws Exception {
        List<String> possible = Arrays.asList("IDS", "Proxy", "Firewall", "NAT");
        Random r = new Random();

        for (int i = 0; i < number; i++) {
            int anzahl = r.nextInt(possible.size()+1);
            Collections.shuffle(possible);
            System.out.println(i + " " + possible.subList(0, anzahl).stream().collect(Collectors.joining(",")));
        }
    }

    public static void createMaxDelayFromTopology() throws Exception {
        String input_topology = "res/problem_instances/germany2/topology";
        String input_requests = "res/problem_instances/germany2/requests";
        String input_vnfs = "res/problem_instances/germany2/vnfLib";

        double min_multiplier = 2.0;
        double max_multiplier = 5.0;

        NetworkGraph ng = TopologyFileReader.readFromFile(input_topology);
        VnfLib vnfLib = VnfLibReader.readFromFile(input_vnfs);
        TrafficRequest[] reqs = TrafficRequestsReader.readFromFile(input_requests, ng, vnfLib);
        Random r = new Random();

        HashMap<Node, HashMap<Node, Node.Att>> bp = ng.getDijkstraBackpointers();
        for (TrafficRequest req : reqs) {
            double shortest = bp.get(req.ingress).get(req.egress).d;

            if (req.vnfSequence.length > 0) {
                Node[] cpuNodes = ng.getNodes().values().stream().filter(n -> n.cpuCapacity > 0.0).toArray(Node[]::new);
                Node middle = ng.getShortestMiddleStation(req.ingress, req.egress, cpuNodes, bp);
                shortest = bp.get(req.ingress).get(middle).d + bp.get(middle).get(req.egress).d;
            }

            double mult = r.nextDouble() * (max_multiplier - min_multiplier) + min_multiplier;
            long d = Math.round(shortest * mult);
            d += Arrays.stream(req.vnfSequence).mapToDouble(v -> v.delay).sum();
            System.out.println(d + ",        ");
        }
    }
}
