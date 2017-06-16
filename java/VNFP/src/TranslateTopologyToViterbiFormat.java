import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Epic class for epic things.
 * (This generic message was probably left here because
 * this file's name is self-explanatory.)
 *
 * @author alex
 */
public class TranslateTopologyToViterbiFormat {
    public static void main(String[] args) throws Exception {
        // Test:
        String inTopo = "res/problem_instances/geant2/topology";
        String outTopoPSA = "res/problem_instances/geant2/topology-fixed-psa";
        String outTopoViterbi = "res/problem_instances/geant2/topology-fixed-viterbi";

        String inReq = "res/problem_instances/geant2/requests";
        String outReqPSA = "res/problem_instances/geant2/requests-fixed-psa";
        String outReqViterbi = "res/problem_instances/geant2/requests-fixed-viterbi";

        translate(inTopo, outTopoPSA, outTopoViterbi, inReq, outReqPSA, outReqViterbi);
    }

    public static void translate(String inTopo, String outTopoPSA, String outTopoViterbi, String inReq, String outReqPSA, String outReqViterbi) throws IOException {
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
                wVit.write(nameToNum.get(s[0]) + " " + s[1] + "\n");
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
                        + " " + s[2]
                        + " " + s[3]
                        + "\n");
            }
        }
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
}
