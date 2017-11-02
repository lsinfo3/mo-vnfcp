import de.uniwue.VNFP.algo.PSA;
import de.uniwue.VNFP.model.*;
import de.uniwue.VNFP.model.factory.TopologyFileReader;
import de.uniwue.VNFP.model.factory.TrafficRequestsReader;
import de.uniwue.VNFP.model.factory.ViterbiSolutionReader;
import de.uniwue.VNFP.model.factory.VnfLibReader;
import de.uniwue.VNFP.model.solution.Solution;
import de.uniwue.VNFP.util.Config;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Epic class for epic things.
 * (This generic message was probably left here because
 * this file's name is self-explanatory.)
 *
 * @author alex
 */
public class Comparison {
    public static void main(String[] args) throws Exception {
        long prefix = System.currentTimeMillis();
        int iterations = 50;
        String baseDir = "/home/debian/eirene/home/alex/MA/eval/compare/" + prefix;

        String[] problems = new String[]{
                "BCAB15",
                "geant",
                "geant2",
                "germany",
                "germany2"
        };

        for (String problem : problems) {
            String input_topology = "res/problem_instances/" + problem + "/topology";
            String input_requests = "res/problem_instances/" + problem + "/requests";
            String input_vnfs = "res/problem_instances/" + problem + "/vnfLib";

            NetworkGraph ng = TopologyFileReader.readFromFile(input_topology);
            VnfLib vnfLib = VnfLibReader.readFromFile(input_vnfs);
            TrafficRequest[] reqs = TrafficRequestsReader.readFromFile(input_requests, ng, vnfLib);

            for (int i = 0; i < iterations; i++) {
                String outFolder = baseDir + "/" + problem + "/" + i;
                Files.createDirectories(Paths.get(outFolder));

                TrafficRequest[] reqs2 = modifyReqs(problem, ng, vnfLib, reqs);
                String outReqsOrig = outFolder + "/orig-requests";
                writeRequestArray(reqs2, outReqsOrig);

                String inTopo = "res/problem_instances/" + problem + "/topology";
                String outTopoPSA = "res/problem_instances/" + problem + "/topology-fixed-psa";
                String outTopoViterbi = "res/problem_instances/" + problem + "/topology-fixed-viterbi";

                String inVnfs = "res/problem_instances/" + problem + "/vnfLib";
                String outVnfsPSA = "res/problem_instances/" + problem + "/vnfLib-fixed-psa";
                String outVnfsViterbi = "res/problem_instances/" + problem + "/vnfLib-fixed-viterbi";

                String inReq = outReqsOrig;
                String outReqPSA = outFolder + "/psa-requests";
                String outReqViterbi = outFolder + "/viterbi-requests";

                TranslateTopologyToViterbiFormat.translate(inTopo, outTopoPSA, outTopoViterbi, inVnfs, outVnfsPSA, outVnfsViterbi, inReq, outReqPSA, outReqViterbi);



                String defaultConfigContent = new Scanner(new FileInputStream("res/problem_instances/" + problem + "/config.js"), "utf-8").useDelimiter("\\Z").next();
                String addContent = "// Run-Specific Config:\n"
                        + "topologyFile = \"topology-fixed-psa\"\n"
                        + "requestsFile = \"" + outReqPSA + "\"\n"
                        + "paretoFrontier = \"" + outFolder + "/pareto_frontier\"\n"
                        + "vnfLoads = \"" + outFolder + "/vnf_loads\"\n"
                        + "vnfDetails = \"" + outFolder + "/vnf_details\"\n"
                        + "solutionSets = \"" + outFolder + "/solution_sets\"\n"
                        + "placementNodes = \"" + outFolder + "/placementNodes\"\n"
                        + "placementLinks = \"" + outFolder + "/placementLinks\"\n"
                        + "placementVnfs = \"" + outFolder + "/placementVnfs\"\n"
                        + "placementFlows = \"" + outFolder + "/placementFlows\"\n";
                String configContent = defaultConfigContent + "\n" + addContent;
                Config c = Config.getInstance(new ByteArrayInputStream(configContent.getBytes()));
                c.writeConfig(new FileOutputStream(outFolder + "/config.js"));

                NetworkGraph _ng = TopologyFileReader.readFromFile(c.topologyFile);
                VnfLib _vnfLib = VnfLibReader.readFromFile(c.vnfLibFile);
                TrafficRequest[] _reqs = TrafficRequestsReader.readFromFile(c.requestsFile, _ng, _vnfLib);

                PSA psa = new PSA(_ng, _reqs, c.s, c.m, c.tmax, c.tmin, c.rho, c.runtime);
                c.createAllEventLoggers().forEach(psa::addEventLogger);

                switch (c.prepMode) {
                    case SHORT_PSA:
                        psa.runPSAPrepPSA();
                        break;
                    case LEAST_DELAY:
                        psa.runPSAPrepDelay();
                        break;
                    case LEAST_CPU:
                        psa.runPSAPrepCpu();
                        break;
                    default:
                        psa.runPSARand();
                }

                System.out.println("\n\n");
            }
        }

        System.out.println("\n\n");
        System.out.println("PSA executions finished. Beginning Viterbi executions...");
        System.out.println("\n\n");

        main2(baseDir);
    }

    public static void main2(String evalDir) throws Exception {
        File f = new File(evalDir);
        String pathToExec = "/home/alex/MA/reference-implementation/middlebox-placement/clean/";

        for (File f2 : f.listFiles()) {
            if (f2.isDirectory()) {
                String problem = f2.getName();
                int i = 0;
                for (File f3 : f2.listFiles()) {
                    if (f3.isDirectory()) {
                        File topoFile = new File("/home/alex/MA/java/VNFP/res/problem_instances/"+problem+"/topology-fixed-viterbi");
                        File reqFile = new File(f3.getAbsolutePath() + "/viterbi-requests");
                        String exec = pathToExec+"middleman --per_core_cost=0.01 --per_bit_transit_cost=3.626543209876543e-7 --topology_file="+topoFile.getAbsolutePath()+" --middlebox_spec_file="+pathToExec+"middlebox-spec --traffic_request_file="+reqFile.getAbsolutePath()+" --max_time=1440 --algorithm=viterbi";
                        Process proc = Runtime.getRuntime().exec(exec);

                        //BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));
                        //BufferedReader stdError = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
                        //String s = null;
                        //while ((s = stdInput.readLine()) != null) {
                        //    System.out.println(s);
                        //}
                        //while ((s = stdError.readLine()) != null) {
                        //    System.out.println(s);
                        //}

                        proc.waitFor();
                        if (!new File("log.sequences").exists()) {
                            throw new RuntimeException("Result file log.sequences not found");
                        }

                        Path moveFrom = Paths.get("log.sequences");
                        Path moveTo = Paths.get(f3.getAbsolutePath() + "/log.sequences");
                        Files.move(moveFrom, moveTo, StandardCopyOption.REPLACE_EXISTING);

                        String input_topology = "/home/alex/MA/java/VNFP/res/problem_instances/"+problem+"/topology-fixed-psa";
                        String input_requests = f3.getAbsolutePath() + "/psa-requests";
                        String input_viterbi = moveTo.toString();
                        String input_vnfs = "/home/alex/MA/java/VNFP/res/problem_instances/"+problem+"/vnfLib";

                        NetworkGraph ng = TopologyFileReader.readFromFile(input_topology);
                        VnfLib vnfLib = VnfLibReader.readFromFile(input_vnfs);
                        TrafficRequest[] reqs = TrafficRequestsReader.readFromFile(input_requests, ng, vnfLib);

                        Solution s = ViterbiSolutionReader.readFromCsv(ng, reqs, input_viterbi);
                        String[] csv = s.toStringCsv();
                        BufferedWriter w = Files.newBufferedWriter(Paths.get(f3.getAbsolutePath() + "/pareto_frontier_viterbi"));
                        w.write(csv[0] + "\n");
                        w.write(csv[1] + "\n");
                        w.close();

                        System.out.println("["+ (++i) +"] Problem " + problem + " number " + f3.getName() + " done.");
                    }
                }
                System.out.println("Problem " + problem + " done.");
            }
        }
    }

    public static TrafficRequest[] modifyReqs(String problem, NetworkGraph ng, VnfLib vnfLib, TrafficRequest[] reqs) {
        if (problem.toLowerCase().equals("bcab15")) {
            TrafficRequest[] reqs2 = Arrays.copyOf(reqs, reqs.length);
            Collections.shuffle(Arrays.asList(reqs2));
            return reqs2;
        }

        Random r = new Random();
        TrafficRequest[] reqs2 = new TrafficRequest[reqs.length];

        List<String> possible = Arrays.asList("IDS", "Proxy", "Firewall", "NAT");
        VNF[][] vnfSequences = new VNF[reqs.length][];

        for (int i = 0; i < reqs.length; i++) {
            int anzahl = r.nextInt(possible.size())+1;
            Collections.shuffle(possible);
            vnfSequences[i] = possible.subList(0, anzahl).stream().flatMap(s -> Arrays.stream(vnfLib.fromString(s))).toArray(VNF[]::new);
        }

        double min_multiplier = 2.0;
        double max_multiplier = 5.0;
        if (problem.toLowerCase().equals("germany")) {
            min_multiplier = 20.0;
            max_multiplier = 50.0;
        }

        HashMap<Node, HashMap<Node, Node.Att>> bp = ng.getDijkstraBackpointers();
        for (int i = 0; i < reqs.length; i++) {
            TrafficRequest req = reqs[i];
            double shortest = bp.get(req.ingress).get(req.egress).d;

            if (vnfSequences[i].length > 0) {
                Node[] cpuNodes = ng.getNodes().values().stream().filter(n -> n.cpuCapacity > 0.0).toArray(Node[]::new);
                Node middle = ng.getShortestMiddleStation(req.ingress, req.egress, cpuNodes, bp);
                shortest = bp.get(req.ingress).get(middle).d + bp.get(middle).get(req.egress).d;
            }

            double mult = r.nextDouble() * (max_multiplier - min_multiplier) + min_multiplier;
            long d = Math.round(shortest * mult);
            d += Arrays.stream(vnfSequences[i]).mapToDouble(v -> v.delay).sum();

            reqs2[i] = new TrafficRequest(req.id, req.ingress, req.egress, req.bandwidthDemand, d, vnfSequences[i]);
        }

        Collections.shuffle(Arrays.asList(reqs2));
        return reqs2;
    }

    public static void writeRequestArray(TrafficRequest[] reqs, String path) throws Exception {
        BufferedWriter w = Files.newBufferedWriter(Paths.get(path));

        for (TrafficRequest req : reqs) {
            w.write(req.ingress.name);
            w.write(",");
            w.write(req.egress.name);
            w.write(",");
            w.write("" + (int) (req.bandwidthDemand * 1000.0));
            w.write(",");
            w.write("" + (int) req.expectedDelay);
            w.write(",");
            w.write(Arrays.stream(req.vnfSequence).map(v -> v.name).collect(Collectors.joining(",")));

            w.write("\n");
        }
        w.close();
    }
}
