package de.uniwue.VNFP.model.factory;

import de.uniwue.VNFP.model.NetworkGraph;
import de.uniwue.VNFP.model.Node;
import de.uniwue.VNFP.model.VnfLib;
import de.uniwue.VNFP.util.Point;

import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.nio.file.Path;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads a {@link NetworkGraph} from a topology file.
 * <p>
 * File format:
 * <pre>
 *     Number-of-nodes, Number-of-links
 *     Node-ID-1, Resources-1.1, Resources-1.2, ...
 *     Node-ID-2, Resources-2.1, Resources-2.2, ...
 *     Node-ID-3, Resources-3.1, Resources-3.2, ...
 *     ...
 *     Node-ID, Node-ID, Bandwidth[kb/s], Delay[μs]
 *     Node-ID, Node-ID, Bandwidth[kb/s], Delay[μs]
 *     Node-ID, Node-ID, Bandwidth[kb/s], Delay[μs]
 *     ...
 * </pre>
 * <p>
 * Example:
 * <pre>
 *     # Number-of-nodes, Number-of-links
 *     3,2
 *
 *     # Node-ID, Cores, RAM, HDD
 *     0,         0,     0,   0
 *     1,         16,    4,   40
 *     2,         16,    4,   40
 *
 *     # Node-ID, Node-ID, Bandwidth, Delay
 *     0,         1,       10000000,  1
 *     1,         2,       10000000,  117
 * </pre>
 *
 * @author alex
 */
public class TopologyFileReader {
    private static String FLOATPATTERN = "\\d+(?:\\.\\d+)?";
    private static String NODEPATTERN = "[^ ;,\\[\\]\\(\\)]+";

    // (Anzahl Nodes) (Anzahl Links)
    private static Pattern pHeader = Pattern.compile("(\\d+),+(\\d+)");
    // (Node-ID) (Cores) (RAM) (HDD)
    private static String pNodeS = "(%n%)(?: *\\((-?%f%) *, *(-?%f%)\\))?";
    private static String pNodeSResources = " *, *+((?:-1)|(?:%f%))";
    // (Node-ID) (Node-ID) (Bandwidth) (Delay)
    private static Pattern pLink = Pattern.compile("(%n%) *, *+(%n%) *, *+((?:-1)|(?:%f%(?:E\\d+)?)) *, *+(%f%)"
            .replace("%f%", FLOATPATTERN)
            .replace("%n%", NODEPATTERN));

    /**
     * Reads a {@link NetworkGraph} from a topology file.
     * Format: See {@link TopologyFileReader}.
     *
     * @param path Path to the topology file.
     * @param vnfLib The VNF library containing all instance and resource types.
     * @return NetworkGraph object with all read content.
     * @throws IOException If any errors during file reads occur.
     */
    public static NetworkGraph readFromFile(Path path, VnfLib vnfLib) throws IOException {
        return readFromFile(path.toAbsolutePath().toString(), vnfLib);
    }

    /**
     * Reads a {@link NetworkGraph} from a topology file.
     * For the file format, cf. the main class documentation {@link TopologyFileReader}.
     *
     * @param path Path to the topology file.
     * @param vnfLib The VNF library containing all instance and resource types.
     * @return NetworkGraph object with all read content.
     * @throws IOException If any errors during file reads occur.
     */
    public static NetworkGraph readFromFile(String path, VnfLib vnfLib) throws IOException {
        Objects.requireNonNull(vnfLib);

        StringBuilder pattern = new StringBuilder(pNodeS);
        for (int i = 0; i < vnfLib.res.length; i++) {
            pattern.append(pNodeSResources);
        }
        Pattern pNode = Pattern.compile(pattern.toString().replace("%f%", FLOATPATTERN).replace("%n%", NODEPATTERN));

        LineNumberReader lnr = new LineNumberReader(new FileReader(path));
        String line;
        int nr = 0;
        int anzNodes = 0;
        int anzLinks = 0;
        // TODO: Select dynamically
        NetworkGraph ng = new NetworkGraph(false);
        ng.hasGeoCoordinates = true;

        while ((line = lnr.readLine()) != null) {
            // Skip: empty lines and comments (# hash)
            if (line.trim().isEmpty() || line.trim().startsWith("#")) {
                continue;
            }

            // First line
            if (nr == 0) {
                Matcher mHeader = pHeader.matcher(line);
                if (!mHeader.matches()) {
                    lnr.close();
                    throw new IOException("wrong header; expected: "+ pHeader.pattern() + "; got: " + line);
                }

                anzNodes = Integer.parseInt(mHeader.group(1));
                anzLinks = Integer.parseInt(mHeader.group(2));
            }

            // Node lines
            else if (nr <= anzNodes) {
                Matcher mNode = pNode.matcher(line);
                if (!mNode.matches()) {
                    lnr.close();
                    throw new IOException("node line '" + line + "' does not match " + pNode.pattern());
                }

                double[] res = new double[vnfLib.res.length];
                for (int i = 0; i < res.length; i++) {
                    res[i] = parseOrInfty(mNode.group(4+i));
                }
                Node node = ng.addNode(mNode.group(1), res);

                // Geo coordinates
                if (mNode.group(2) == null) {
                    ng.hasGeoCoordinates = false;
                }
                else {
                    node.geo = new Point(mNode.group(2), mNode.group(3));
                }
            }

            // Link lines
            else if (nr <= anzNodes + anzLinks) {
                Matcher mLink = pLink.matcher(line);
                if (!mLink.matches()) {
                    lnr.close();
                    throw new IOException("link line '" + line + "' does not match " + pLink.pattern());
                }

                Node n1 = ng.getNodes().get(mLink.group(1));
                Node n2 = ng.getNodes().get(mLink.group(2));
                if (n1 == null) {
                    lnr.close();
                    throw new IOException("node '" + mLink.group(1) + "' not found for link: " + line);
                }
                if (n2 == null) {
                    lnr.close();
                    throw new IOException("node '" + mLink.group(2) + "' not found for link: " + line);
                }

                // TODO: Select dynamically
                ng.addLink(n1, n2, parseOrInfty(mLink.group(3)) / 1000.0, Double.parseDouble(mLink.group(4)));
                //ng.addBothDirectedLinks(n1, n2, parseOrInfty(mLink.group(3)) / 1000.0, Double.parseDouble(mLink.group(4)));
            }

            // nr > expected number of lines
            else {
                lnr.close();
                throw new IOException("too many lines; should be 1 + " + anzNodes + " + " + anzLinks);
            }

            nr++;
        }

        if (nr != anzNodes + anzLinks + 1) {
            lnr.close();
            throw new IOException("too few lines; should be 1 + " + anzNodes + " + " + anzLinks + " = " + (anzNodes + anzLinks + 1));
        }

        lnr.close();

        return ng;
    }

    /**
     * This function is a wrapper for <tt>Double.parseDouble(s)</tt>
     * that returns <tt>Double.POSITIVE_INFINITY</tt> if s is "-1".
     *
     * @param s String to be converted.
     * @return Double.parseDouble(s), or Double.POSITIVE_INFINITY if it's -1.
     */
    private static double parseOrInfty(String s) {
        if (s.equals("-1")) return Double.POSITIVE_INFINITY;
        return Double.parseDouble(s);
    }
}
