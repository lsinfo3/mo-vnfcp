package de.uniwue.VNFP.model.factory;

import de.uniwue.VNFP.model.Link;
import de.uniwue.VNFP.model.NetworkGraph;
import de.uniwue.VNFP.model.Node;
import de.uniwue.VNFP.model.TrafficRequest;
import de.uniwue.VNFP.model.solution.NodeAssignment;
import de.uniwue.VNFP.model.solution.Solution;
import de.uniwue.VNFP.model.solution.TrafficAssignment;

import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 * This class allows to load the CPLEX solution from [BCAB15]
 * and express its performance with our own data structures.
 *
 * @author alex
 */
public class CplexSolutionReader {
    private static Pattern pNodeList = Pattern.compile("\\d+(?:,\\d+)*");

    /**
     * Reads the paths and VNF sequences from the CPLEX solution.
     * <b>The order of the TrafficRequest array must match the order of both CPLEX log files!</b>
     *
     * @param ng                 Network topology graph.
     * @param reqs               The traffic demands. <b>The order must match the order of both CPLEX log files!</b>
     * @param cplexPathsFile     Path towards "log.cplex.paths".
     * @param cplexSequencesFile Path towards "log.cplex.sequences".
     * @return A Solution object for the given placement.
     * @throws IOException If any errors during file reads occur.
     */
    public static Solution readFromCsv(NetworkGraph ng, TrafficRequest[] reqs, Path cplexPathsFile, Path cplexSequencesFile) throws IOException {
        return readFromCsv(ng, reqs, cplexPathsFile.toAbsolutePath().toString(), cplexSequencesFile.toAbsolutePath().toString());
    }

    /**
     * Reads the paths and VNF sequences from the CPLEX solution.
     * <b>The order of the TrafficRequest array must match the order of both CPLEX log files!</b>
     *
     * @param ng                 Network topology graph.
     * @param reqs               The traffic demands. <b>The order must match the order of both CPLEX log files!</b>
     * @param cplexPathsFile     Path towards "log.cplex.paths".
     * @param cplexSequencesFile Path towards "log.cplex.sequences".
     * @return A Solution object for the given placement.
     * @throws IOException If any errors during file reads occur.
     */
    public static Solution readFromCsv(NetworkGraph ng, TrafficRequest[] reqs, String cplexPathsFile, String cplexSequencesFile) throws IOException {
        LineNumberReader paths = new LineNumberReader(new FileReader(cplexPathsFile));
        LineNumberReader seqs = new LineNumberReader(new FileReader(cplexSequencesFile));
        int nr = -1;
        ArrayList<TrafficAssignment> tAssigs = new ArrayList<>();

        while (true) {
            nr++;

            // Sanity-Checks:
            String lPaths = paths.readLine();
            String lSeqs = seqs.readLine();

            boolean pathsEmpty = lPaths == null || lPaths.trim().isEmpty();
            boolean seqsEmpty = lSeqs == null || lSeqs.trim().isEmpty();

            if (pathsEmpty != seqsEmpty) {
                paths.close(); seqs.close();
                throw new IOException("files do not contain same amount of lines");
            }
            if (pathsEmpty) break;

            if (!pNodeList.matcher(lPaths).matches() || !pNodeList.matcher(lSeqs).matches()) {
                paths.close(); seqs.close();
                throw new IOException("line " + (nr+1) + " does not match pattern " + pNodeList.pattern());
            }

            // Translate String to Node array:
            String[] sPath = lPaths.split(",");
            String[] sSeq = lSeqs.split(",");
            Node[] nPath = new Node[sPath.length];
            Node[] nSeq = new Node[sSeq.length];

            for (int i = 0; i < sPath.length; i++) {
                nPath[i] = ng.getNodes().get(sPath[i]);
                if (nPath[i] == null) {
                    paths.close(); seqs.close();
                    throw new IOException("node " + sPath[i] + " not found in graph");
                }
            }
            for (int i = 0; i < sSeq.length; i++) {
                nSeq[i] = ng.getNodes().get(sSeq[i]);
                if (nSeq[i] == null) {
                    paths.close(); seqs.close();
                    throw new IOException("node " + sSeq[i] + " not found in graph");
                }
            }

            // Check, if ingress and egress match TrafficRequest:
            if (!nPath[0].equals(nSeq[0])) {
                paths.close(); seqs.close();
                throw new IOException("different ingress-nodes on line " + (nr+1));
            }
            if (!nPath[nPath.length-1].equals(nSeq[nSeq.length-1])) {
                paths.close(); seqs.close();
                throw new IOException("different egress-nodes on line " + (nr+1));
            }
            if (!nPath[0].equals(reqs[nr].ingress)) {
                paths.close(); seqs.close();
                throw new IOException("ingress on line " + (nr+1) + " does not match ingress of request["+nr+"]");
            }
            if (!nPath[nPath.length-1].equals(reqs[nr].egress)) {
                paths.close(); seqs.close();
                throw new IOException("egress on line " + (nr+1) + " does not match egress of request["+nr+"]");
            }

            ArrayList<NodeAssignment> nAssigs = new ArrayList<>();

            // Loop through both arrays:
            int iSeq = 1;
            for (int iPath = 0; iPath < nPath.length; iPath++) {
                // Find Link towards previous element:
                Link link = null;
                if (iPath != 0) {
                    int _iPath = iPath;
                    link = ng.getNodes().get(sPath[iPath]).getNeighbours().stream()
                            .filter(l -> l.getOther(nPath[_iPath]).equals(nPath[_iPath-1]))
                            .findFirst().get();
                }

                // Shall VNF instances be placed?
                if (iSeq < nSeq.length - 1 && nSeq[iSeq].equals(nPath[iPath])) {
                    while (iSeq < nSeq.length - 1 && nSeq[iSeq].equals(nPath[iPath])) {
                        nAssigs.add(new NodeAssignment(nPath[iPath], reqs[nr].vnfSequence[iSeq-1], link));
                        link = null;
                        iSeq++;
                    }
                }
                else {
                    nAssigs.add(new NodeAssignment(nPath[iPath], null, link));
                }
            }

            TrafficAssignment tAssig = new TrafficAssignment(reqs[nr], nAssigs.toArray(new NodeAssignment[nAssigs.size()]), ng);
            tAssigs.add(tAssig);
        }

        paths.close();
        seqs.close();

        return Solution.getInstance(ng, reqs, tAssigs.toArray(new TrafficAssignment[tAssigs.size()]));
    }
}
