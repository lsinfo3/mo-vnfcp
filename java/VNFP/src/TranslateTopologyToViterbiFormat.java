import de.uniwue.VNFP.model.*;
import de.uniwue.VNFP.model.factory.TopologyFileReader;
import de.uniwue.VNFP.model.factory.TrafficRequestsReader;
import de.uniwue.VNFP.model.factory.ViterbiSolutionReader;
import de.uniwue.VNFP.model.factory.VnfLibReader;
import de.uniwue.VNFP.model.solution.Solution;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.Collectors;

/**
 * Epic class for epic things.
 * (This generic message was probably left here because
 * this file's name is self-explanatory.)
 *
 * @author alex
 */
public class TranslateTopologyToViterbiFormat {
    public static void main(String[] args) throws Exception {
        String baseFolder = "/home/alex/w/17/benchmark-vnfcp-generator/eval/dynamic/1508688107069/";

        File[] folders = new File(baseFolder).listFiles(File::isDirectory);
        for (int i = 0; i < folders.length; i++) {
            File f = folders[i];
            File[] folders2 = f.listFiles(File::isDirectory);
            for (int j = 0; j < folders2.length; j++) {
                File f2 = folders2[j];

                String base = f2.getAbsolutePath() + "/";

                String inTopo = base + "outTopo";
                String outTopoPSA = base + "topology-fixed-psa";
                String outTopoViterbi = base + "topology-fixed-viterbi";

                String inVnfs = base + "outVnfs";
                String outVnfPSA = base + "vnfs-fixed-psa";
                String outVnfViterbi = base + "vnfs-fixed-viterbi";

                String inReq = base + "outReqs";
                String outReqPSA = base + "requests-fixed-psa";
                String outReqViterbi = base + "requests-fixed-viterbi";

                System.out.println("Converting " + base + " ...");
                translate(inTopo, outTopoPSA, outTopoViterbi, inVnfs, outVnfPSA, outVnfViterbi, inReq, outReqPSA, outReqViterbi);
            }
        }

        System.out.println("Running Viterbi Algorithm...");
        runViterbiOnFolder(baseFolder);
    }

    public static void translate(String inTopo, String outTopoPSA, String outTopoViterbi, String inVnfs, String outVnfPSA, String outVnfViterbi, String inReq, String outReqPSA, String outReqViterbi) throws IOException {
        LineNumberReader lnr = new LineNumberReader(new FileReader(inTopo));
        BufferedWriter wPsa = Files.newBufferedWriter(Paths.get(outTopoPSA));
        BufferedWriter wVit = Files.newBufferedWriter(Paths.get(outTopoViterbi));

        HashMap<String, Integer> nameToNum = new HashMap<>();
        int nMax = 0;

        String l;
        int n = 0;
        while ((l = lnr.readLine()) != null) {
            if (l.trim().isEmpty() || l.trim().startsWith("#")) {
                continue;
            }
            l = l.trim().replaceAll(" +", " ");

            n++;
            if (n == 1) {
                wPsa.write(l + "\n");
                wVit.write(l + "\n");
                nMax = Integer.parseInt(l.split(" ")[0]);
            }
            else if (n > 1 && n <= nMax+1) {
                String[] s = l.split(" ");
                nameToNum.put(s[0], n-2);
                wPsa.write(nameToNum.get(s[0]) + " " + s[1] + " " + s[2] + " " + s[3] + "\n");
                wVit.write(nameToNum.get(s[0]) + " " + (int) Math.floor(Double.parseDouble(s[1])) + "\n");
            }
            else {
                String[] s = l.split(" ");
                wPsa.write(nameToNum.get(s[0])
                        + " " + nameToNum.get(s[1])
                        + " " + s[2]
                        + " " + s[3]
                        + "\n");
                wVit.write(nameToNum.get(s[0])
                        + " " + nameToNum.get(s[1])
                        + " " + String.format("%.0f", Double.parseDouble(s[2]))
                        + " " + String.format("%.0f", Double.parseDouble(s[3]))
                        + "\n");
            }
        }
        wPsa.close();
        wVit.close();

        lnr = new LineNumberReader(new FileReader(inVnfs));
        wPsa = Files.newBufferedWriter(Paths.get(outVnfPSA));
        wVit = Files.newBufferedWriter(Paths.get(outVnfViterbi));
        wPsa.write("[vnfs]\n");

        int mode = 0;
        while ((l = lnr.readLine()) != null) {
            if (l.trim().isEmpty() || l.trim().startsWith("#")) {
                continue;
            }
            l = l.trim().replaceAll(" +", " ").toLowerCase();

            if (l.equals("[vnfs]")) {
                mode = 1;
            }
            else if (l.equals("[abbrev]") || l.equals("[pairs]")) {
                mode = 2;
            }
            else if (mode == 1) {
                String[] s = l.split(",");
                s[1] = "" + (long) Math.floor(Double.parseDouble(s[1]));
                s[4] = "" + (long) Math.floor(Double.parseDouble(s[4]));
                s[5] = "" + (long) Math.floor(Double.parseDouble(s[5]));
                wPsa.write(Arrays.stream(s).collect(Collectors.joining(",")) + "\n");
                wVit.write(s[0]
                        + "," + s[1]
                        + "," + s[4]
                        + "," + s[5]
                        + ",0.0\n");
            }
        }
        wVit.write("nix,0,0,9999999999999,0.0\n");
        wPsa.close();
        wVit.close();

        lnr = new LineNumberReader(new FileReader(inReq));
        wPsa = Files.newBufferedWriter(Paths.get(outReqPSA));
        wVit = Files.newBufferedWriter(Paths.get(outReqViterbi));

        while ((l = lnr.readLine()) != null) {
            if (l.trim().isEmpty() || l.trim().startsWith("#")) {
                continue;
            }
            l = l.trim().replaceAll(" +", " ");

            String[] s = l.split(",", 5);
            wPsa.write(nameToNum.get(s[0].trim())
                    + "," + nameToNum.get(s[1].trim())
                    + "," + s[2].trim()
                    + "," + s[3].trim()
                    + "," + s[4].trim().toLowerCase()
                    + "\n");
            wVit.write("0"
                    + "," + nameToNum.get(s[0].trim())
                    + "," + nameToNum.get(s[1].trim())
                    + "," + s[2].trim()
                    + "," + s[3].trim()
                    + ",0.00000010"
                    + "," + (s[4].trim().isEmpty() ? "nix" : s[4].trim()).toLowerCase()
                    + "\n");
        }
        wPsa.close();
        wVit.close();
    }

    public static void runViterbiOnFolder(String baseFolder) throws Exception {
        File f = new File(baseFolder);
        String pathToExec = "/home/alex/w/misc/ma/reference-implementation/middlebox-placement/clean/middleman";

        int n = 0;
        File[] files = f.listFiles();
        for (File f2 : files) {
            if (f2.isDirectory()) {
                File[] files2 = f2.listFiles();
                int i = 0;
                for (File f3 : files2) {
                    if (f3.isDirectory()) {
                        File topoFile = f3.toPath().resolve("topology-fixed-viterbi").toFile();
                        File vnfFile = f3.toPath().resolve("vnfs-fixed-viterbi").toFile();
                        File reqFile = f3.toPath().resolve("requests-fixed-viterbi").toFile();
                        String exec = pathToExec+" --per_core_cost=0.01 --per_bit_transit_cost=3.626543209876543e-7 --topology_file="+topoFile.getAbsolutePath()+" --middlebox_spec_file="+vnfFile.getAbsolutePath()+" --traffic_request_file="+reqFile.getAbsolutePath()+" --max_time=1440 --algorithm=viterbi";
                        System.out.println("    - Executing " + exec);
                        Process proc = Runtime.getRuntime().exec(exec);

                        proc.waitFor();
                        if (!new File("log.sequences").exists()) {
                            throw new RuntimeException("Result file log.sequences not found");
                        }

                        Path moveFrom = Paths.get("log.sequences");
                        Path moveTo = Paths.get(f3.getAbsolutePath() + "/log.sequences");
                        Files.move(moveFrom, moveTo, StandardCopyOption.REPLACE_EXISTING);

                        String input_topology = f3.toPath().resolve("topology-fixed-psa").toAbsolutePath().toString();
                        String input_vnfs = f3.toPath().resolve("vnfs-fixed-psa").toAbsolutePath().toString();
                        String input_requests = f3.toPath().resolve("requests-fixed-psa").toAbsolutePath().toString();
                        String input_viterbi = moveTo.toString();

                        VnfLib vnfLib = VnfLibReader.readFromFile(input_vnfs);
                        NetworkGraph ng = TopologyFileReader.readFromFile(input_topology, vnfLib);
                        TrafficRequest[] reqs = TrafficRequestsReader.readFromFile(input_requests, ng, vnfLib);
                        ProblemInstance pi = new ProblemInstance(ng, vnfLib, reqs, new Objs(vnfLib.getResources()));

                        Solution s = ViterbiSolutionReader.readFromCsv(pi, input_viterbi);
                        String[] csv = s.toStringCsv();
                        BufferedWriter w = Files.newBufferedWriter(Paths.get(f3.getAbsolutePath() + "/pareto_frontier_viterbi"));
                        w.write(csv[0] + "\n");
                        w.write(csv[1] + "\n");
                        w.close();

                        System.out.println("  [" + (++i+"/"+files2.length) + "] Problem number " + f3.getName() + " done.");
                    }
                }
                System.out.println("[" + (++n+"/"+files.length) + "] Folder " + f2.getName() + " done.");
            }
        }
    }
}
