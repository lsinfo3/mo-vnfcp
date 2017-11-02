import de.uniwue.VNFP.algo.PSA;
import de.uniwue.VNFP.model.NetworkGraph;
import de.uniwue.VNFP.model.TrafficRequest;
import de.uniwue.VNFP.model.VnfLib;
import de.uniwue.VNFP.model.factory.TopologyFileReader;
import de.uniwue.VNFP.model.factory.TrafficRequestsReader;
import de.uniwue.VNFP.model.factory.VnfLibReader;
import de.uniwue.VNFP.util.Config;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;

/**
 * Epic class for epic things.
 * (This generic message was probably left here because
 * this file's name is self-explanatory.)
 *
 * @author alex
 */
public class ParameterEval {
    public static void main(String[] args) throws Exception {
        long prefix = System.currentTimeMillis();

        int iterations = 16;

        String defaultConfigContent = new Scanner(new FileInputStream("res/config.js"), "utf-8").useDelimiter("\\Z").next();



        String[] _problem = new String[]{
                "BCAB15",
                //"geant2",
                //"germany2"
        };
        for (String problem : _problem) {
            String basePath = "basePath=\"res/problem_instances/"+problem+"\"";



            String[] _s = new String[]{
                    "s=1",
                    "s=2",
                    "s=4",
                    "s=8",
                    "s=16",
                    //"s=24"
            };
        for (String s : _s) {



            String[] _loops;
            _loops = new String[]{
                    // m; rho; runtime
                    //"m=500; rho=0.85; runtime=10",
                    "m=500; rho=0.80; runtime=20"
            };
        for (String loops : _loops) {



            String[] _tmax = new String[]{
                    "tmax=50"
            };
        for (String tmax : _tmax) {



            String[] _tmin = new String[]{
                    "tmin=1"
            };
        for (String tmin : _tmin) {


            String[] _prep = new String[]{
                    //"prepMode=LEAST_CPU",
                    "prepMode=LEAST_DELAY",
                    //"prepMode=SHORT_PSA",
                    //"prepMode=RAND"
            };
        for (String prep : _prep) {



            String[] _reassign = new String[]{
                    //"pReassignVnf=0",
                    //"pReassignVnf=0.1",

                    "\npmin = 0.0\n" +
                            "pmax = 1.0\n" +
                            "i1 = 0.2*numberOfTemperatureLevels\n" +
                            "i2 = 0.8*numberOfTemperatureLevels\n" +
                            "pReassignVnf = (i2 - i)/(i2 - i1) * (pmax - pmin) + pmin\n" +
                            "pReassignVnf = Math.max(pmin, Math.min(pmax, pReassignVnf))\n"
            };
        for (int reassignIndex = 0; reassignIndex < _reassign.length; reassignIndex++) {
            String reassign = _reassign[reassignIndex];



            String[] _newInstance = new String[]{
                    //"pNewInstance=0",
                    "pNewInstance=0.5*pReassignVnf",
                    //"pNewInstance=pReassignVnf"
            };
        for (String newInstance : _newInstance) {



            String[] _weights = new String[]{
                    "useWeights=true",
                    //"useWeights=false"
            };
        for (String weights : _weights) {

            String varContent = s + "\n"
                    + loops + "\n"
                    + tmax + "\n"
                    + tmin + "\n"
                    + prep + "\n"
                    + reassign + "\n"
                    + newInstance + "\n"
                    + weights;
            System.out.println("\n\n\n\n");
            System.out.println("--- " + iterations + " new PSA runs with the following settings:");
            System.out.println(varContent);
            System.out.println("\n\n\n\n");



        for (int i = 0; i < iterations; i++) {
            String reassignTxt = (reassign.split("\n").length > 1 ? "pReassignVnf=linear" : reassign);
            String mTxt = loops.split("=")[1].split(";")[0];
            String rhoTxt = loops.split("=")[2].split(";")[0];
            String runtimeTxt = loops.split("=")[3];
            String outFolder = "/home/alex/w/misc/ma/eval/out/" + prefix + "/" + problem + "/"
            //String outFolder = "/home/debian/eirene/home/alex/MA/eval/out/" + prefix + "/" + problem + "/"
                    + s.split("=", 2)[1].replace("_", "").replace("*", "")
                    + "_" + mTxt.replace("_", "").replace("*", "")
                    + "_" + rhoTxt.replace("_", "").replace("*", "")
                    + "_" + runtimeTxt.replace("_", "").replace("*", "")
                    + "_" + tmax.split("=", 2)[1].replace("_", "").replace("*", "")
                    + "_" + tmin.split("=", 2)[1].replace("_", "").replace("*", "")
                    + "_" + prep.split("=", 2)[1].replace("_", "").replace("*", "")
                    + "_" + reassignTxt.split("=", 2)[1].replace("_", "").replace("*", "")
                    + "_" + newInstance.split("=", 2)[1].replace("_", "").replace("*", "")
                    + "_" + weights.split("=", 2)[1].replace("_", "").replace("*", "")
                    + "/" + i;

            Files.createDirectories(Paths.get(outFolder));

            String outputFiles = "paretoFrontier = \"" + outFolder + "/pareto_frontier\"\n"
                    //+ "results = \"" + outFolder + "/results\"\n"
                    + "vnfLoads = \"" + outFolder + "/vnf_loads\"\n"
                    + "vnfDetails = \"" + outFolder + "/vnf_details\"\n"
                    + "solutionSets = \"" + outFolder + "/solution_sets\"\n"
                    + "placementNodes = \"" + outFolder + "/placementNodes\"\n"
                    + "placementLinks = \"" + outFolder + "/placementLinks\"\n"
                    + "placementVnfs = \"" + outFolder + "/placementVnfs\"\n"
                    + "placementFlows = \"" + outFolder + "/placementFlows\"\n";

            String configContent = defaultConfigContent +"\n"
                    + "showGui = false\n"
                    + "\n\n\n"
                    + "// Variable config from evaluation:\n"
                    + varContent + "\n"
                    + "numberOfTemperatureLevels = Math.ceil(Math.log(tmin / tmax) / Math.log(rho))\n"
                    + basePath + "\n"
                    + outputFiles;
            Config c = Config.getInstance(new ByteArrayInputStream(configContent.getBytes()));
            c.writeConfig(new FileOutputStream(outFolder + "/config.js"));

            NetworkGraph ng = TopologyFileReader.readFromFile(c.topologyFile);
            VnfLib vnfLib = VnfLibReader.readFromFile(c.vnfLibFile);
            TrafficRequest[] reqs = TrafficRequestsReader.readFromFile(c.requestsFile, ng, vnfLib);

            PSA psa = new PSA(ng, reqs, c.s, c.m, c.tmax, c.tmin, c.rho, c.runtime);
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

        }}}}}}}}}}
    }
}
