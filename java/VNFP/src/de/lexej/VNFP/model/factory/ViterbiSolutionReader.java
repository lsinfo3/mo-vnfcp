package de.lexej.VNFP.model.factory;

import de.lexej.VNFP.algo.FlowUtils;
import de.lexej.VNFP.model.NetworkGraph;
import de.lexej.VNFP.model.Node;
import de.lexej.VNFP.model.TrafficRequest;
import de.lexej.VNFP.model.solution.Solution;
import de.lexej.VNFP.model.solution.TrafficAssignment;

import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;

/**
 * This class allows to load the Viterbi solution from [BCAB15]
 * and express its performance with our own data structures.
 *
 * @author alex
 */
public class ViterbiSolutionReader {
    private static Pattern pNodeList = Pattern.compile("\\d+(?:,\\d+)*");

    /**
     * Reads the paths and VNF sequences from the Viterbi solution.
     * <b>The order of the TrafficRequest array must match the order of both CPLEX log files!</b>
     *
     * @param ng                   Network topology graph.
     * @param reqs                 The traffic demands. <b>The order must match the order of the Viterbi log file!</b>
     * @param viterbiSequencesFile Path towards "log.sequences".
     * @return A Solution object for the given placement.
     * @throws IOException If any errors during file reads occur.
     */
    public static Solution readFromCsv(NetworkGraph ng, TrafficRequest[] reqs, Path viterbiSequencesFile) throws IOException {
        return readFromCsv(ng, reqs, viterbiSequencesFile.toAbsolutePath().toString());
    }

    /**
     * Reads the paths and VNF sequences from the Viterbi solution.
     * <b>The order of the TrafficRequest array must match the order of both CPLEX log files!</b>
     *
     * @param ng                   Network topology graph.
     * @param reqs                 The traffic demands. <b>The order must match the order of the Viterbi log file!</b>
     * @param viterbiSequencesFile Path towards "log.sequences".
     * @return A Solution object for the given placement.
     * @throws IOException If any errors during file reads occur.
     */
    public static Solution readFromCsv(NetworkGraph ng, TrafficRequest[] reqs, String viterbiSequencesFile) throws IOException {
        LineNumberReader lnr = new LineNumberReader(new FileReader(viterbiSequencesFile));
        int nr = -1;
        ArrayList<TrafficAssignment> tAssigs = new ArrayList<>();

        String line;
        while ((line = lnr.readLine()) != null) {
            nr++;

            // Sanity-Check:
            if (!pNodeList.matcher(line).matches()) {
                lnr.close();
                throw new IOException("line " + (nr+1) + " does not match pattern " + pNodeList.pattern());
            }

            // Translate String to Node array:
            String[] sSeq = line.split(",");
            Node[] nSeq = new Node[sSeq.length-2];

            for (int i = 0; i < nSeq.length; i++) {
                nSeq[i] = ng.getNodes().get(sSeq[i+1]);
                if (nSeq[i] == null) {
                    lnr.close();
                    throw new IOException("node " + sSeq[i+1] + " not found in graph");
                }
            }

            // Check if ingress and egress match TrafficRequest:
            if (!reqs[nr].ingress.equals(ng.getNodes().get(sSeq[0]))) {
                lnr.close();
                throw new IOException("ingress on line " + (nr+1) + " does not match ingress of request["+nr+"]");
            }
            if (!reqs[nr].egress.equals(ng.getNodes().get(sSeq[sSeq.length-1]))) {
                lnr.close();
                throw new IOException("egress on line " + (nr+1) + " does not match egress of request["+nr+"]");
            }

            // Create partial solution from node order:
            TrafficAssignment tAssig = FlowUtils.fromVnfSequence(reqs[nr], nSeq, ng, ng.getDijkstraBackpointers());
            tAssigs.add(tAssig);
        }

        return Solution.getInstance(ng, reqs, tAssigs.toArray(new TrafficAssignment[tAssigs.size()]));
    }
}
