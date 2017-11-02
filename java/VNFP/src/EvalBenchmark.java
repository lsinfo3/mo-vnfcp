import de.uniwue.VNFP.algo.PSA;
import de.uniwue.VNFP.model.NetworkGraph;
import de.uniwue.VNFP.model.TrafficRequest;
import de.uniwue.VNFP.model.VnfLib;
import de.uniwue.VNFP.model.factory.TopologyFileReader;
import de.uniwue.VNFP.model.factory.TrafficRequestsReader;
import de.uniwue.VNFP.model.factory.VnfLibReader;
import de.uniwue.VNFP.util.Config;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.SequenceInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Epic class for epic things.
 * (This generic message was probably left here because
 * this file's name is self-explanatory.)
 *
 * @author alex
 */
public class EvalBenchmark {
    public static void main(String[] args) throws Exception {
        Locale.setDefault(Locale.US);

        for (String baseDir : new String[]{
                "/home/alex/w/17/benchmark-vnfcp-generator/eval/dynamic/1508688107069"
        }) {
            System.out.println("Starting with " + baseDir + " ...");

            File[] folders = new File(baseDir).listFiles(File::isDirectory);
            for (int i = 0; i < folders.length; i++) {
                File f = folders[i];
                File[] folders2 = f.listFiles(File::isDirectory);
                for (int j = 0; j < folders2.length; j++) {
                    File f2 = folders2[j];
                    solveProblem(f2.toPath(), args[0], i + 1, folders.length);
                }
            }
        }

        TranslateTopologyToViterbiFormat.main(args);
    }

    public static void solveProblem(Path baseFolder, String baseConfig, int current, int all) {
        try {
            NetworkGraph ng = TopologyFileReader.readFromFile(baseFolder.resolve("outTopo"));
            VnfLib vnfLib = VnfLibReader.readFromFile(baseFolder.resolve("outVnfs"));
            TrafficRequest[] reqs = TrafficRequestsReader.readFromFile(baseFolder.resolve("outReqs"), ng, vnfLib);

            System.out.println("Current problem: " + baseFolder.getParent().getFileName() + "/" + baseFolder.getFileName() + " (number " + current + "/" + all + ")");

            String suffix = "_2";
            Config c = Config.getInstance(new SequenceInputStream(
                    new FileInputStream(baseConfig),
                    new ByteArrayInputStream(("\n" +
                            "baseFolder = \"" + baseFolder.toAbsolutePath().toString() + "/\"\n" +
                            "paretoFrontier = baseFolder + \"psa_pareto_frontier"+suffix+"\"\n" +
                            "placementNodes = baseFolder + \"psa_placement_nodes"+suffix+"\"\n" +
                            "placementLinks = baseFolder + \"psa_placement_links"+suffix+"\"\n" +
                            "placementVnfs = baseFolder + \"psa_placement_vnfs"+suffix+"\"\n" +
                            "placementFlows = baseFolder + \"psa_placement_flows"+suffix+"\""
                    ).getBytes())
            ));
            Path runtimePath = baseFolder.resolve("psa_runtime_ms"+suffix);
            PSA psa = new PSA(ng, reqs, c.s, c.m, c.tmax, c.tmin, c.rho, c.runtime);
            c.createAllEventLoggers().forEach(psa::addEventLogger);

            long start = System.currentTimeMillis();
            switch (Config.getInstance().prepMode) {
                case LEAST_CPU:
                    psa.runPSAPrepCpu();
                    break;
                case LEAST_DELAY:
                    psa.runPSAPrepDelay();
                    break;
                case SHORT_PSA:
                    psa.runPSAPrepPSA();
                    break;
                default:
                    psa.runPSARand();
            }
            long diff = System.currentTimeMillis() - start;
            Files.write(runtimePath, (""+diff).getBytes());
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
