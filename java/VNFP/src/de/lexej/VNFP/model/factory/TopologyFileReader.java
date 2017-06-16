package de.lexej.VNFP.model.factory;

import de.lexej.VNFP.model.NetworkGraph;
import de.lexej.VNFP.model.Node;

import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads a {@link NetworkGraph} from a topology file.
 * <p>
 * File format:
 * <pre>
 *     [Number of nodes] [Number of links]
 *     [Node ID] [CPU capacity (Number of cores)] [RAM capacity (MB)] [HDD capacity (GB)]
 *     [Node ID] [CPU capacity (Number of cores)] [RAM capacity (MB)] [HDD capacity (GB)]
 *     [Node ID] [CPU capacity (Number of cores)] [RAM capacity (MB)] [HDD capacity (GB)]
 *     ...
 *     [Node ID] [Node ID] [Bandwidth (kb/s)] [Delay (μs)]
 *     [Node ID] [Node ID] [Bandwidth (kb/s)] [Delay (μs)]
 *     [Node ID] [Node ID] [Bandwidth (kb/s)] [Delay (μs)]
 *     ...
 * </pre>
 * <p>
 * Example:
 * <pre>
 *     # Number of nodes, Number of links
 *     3 2
 *
 *     # Node ID, Cores
 *     0 0
 *     1 16
 *     2 16
 *
 *     # Node ID, Node ID, Bandwidth, Delay
 *     0 1 10000000 1
 *     1 2 10000000 117
 * </pre>
 *
 * @author alex
 */
public class TopologyFileReader {
    // (Anzahl Nodes) (Anzahl Links)
    private static Pattern pHeader = Pattern.compile("(\\d+) +(\\d+)");
    // (Node-ID) (Cores) (RAM) (HDD)
    private static Pattern pNode = Pattern.compile("([^ ;,\\]\\[]+) +((?:-1)|(?:\\d+(?:\\.\\d+)?)) +((?:-1)|(?:\\d+(?:\\.\\d+)?)) +((?:-1)|(?:\\d+(?:\\.\\d+)?))");
    // (Node-ID) (Node-ID) (Bandwidth) (Delay)
    private static Pattern pLink = Pattern.compile("([^ ;,\\]\\[]+) +([^ ;,\\]\\[]+) +((?:-1)|(?:\\d+(?:\\.\\d+)?)) +(\\d+(?:\\.\\d+)?)");

    /**
     * Reads a {@link NetworkGraph} from a topology file.
     * <p>
     * File format:
     * <pre>
     *     [Number of nodes] [Number of links]
     *     [Node ID] [CPU capacity (Number of cores)] [RAM capacity (MB)] [HDD capacity (GB)]
     *     [Node ID] [CPU capacity (Number of cores)] [RAM capacity (MB)] [HDD capacity (GB)]
     *     [Node ID] [CPU capacity (Number of cores)] [RAM capacity (MB)] [HDD capacity (GB)]
     *     ...
     *     [Node ID] [Node ID] [Bandwidth (kb/s)] [Delay (μs)]
     *     [Node ID] [Node ID] [Bandwidth (kb/s)] [Delay (μs)]
     *     [Node ID] [Node ID] [Bandwidth (kb/s)] [Delay (μs)]
     *     ...
     * </pre>
     * <p>
     * Example:
     * <pre>
     *     # Number of nodes, Number of links
     *     3 2
     *
     *     # Node ID, Cores
     *     0 0
     *     1 16
     *     2 16
     *
     *     # Node ID, Node ID, Bandwidth, Delay
     *     0 1 10000000 1
     *     1 2 10000000 117
     * </pre>
     *
     * @param path Path to the topology file.
     * @return NetworkGraph object with all read content.
     * @throws IOException If any errors during file reads occur.
     */
    public static NetworkGraph readFromFile(Path path) throws IOException {
        return readFromFile(path.toAbsolutePath().toString());
    }

    /**
     * Reads a {@link NetworkGraph} from a topology file.
     * <p>
     * File format:
     * <pre>
     *     [Number of nodes] [Number of links]
     *     [Node ID] [CPU capacity (Number of cores)] [RAM capacity (MB)] [HDD capacity (GB)]
     *     [Node ID] [CPU capacity (Number of cores)] [RAM capacity (MB)] [HDD capacity (GB)]
     *     [Node ID] [CPU capacity (Number of cores)] [RAM capacity (MB)] [HDD capacity (GB)]
     *     ...
     *     [Node ID] [Node ID] [Bandwidth (kb/s)] [Delay (μs)]
     *     [Node ID] [Node ID] [Bandwidth (kb/s)] [Delay (μs)]
     *     [Node ID] [Node ID] [Bandwidth (kb/s)] [Delay (μs)]
     *     ...
     * </pre>
     * <p>
     * Example:
     * <pre>
     *     # Number of nodes, Number of links
     *     3 2
     *
     *     # Node ID, Cores
     *     0 0
     *     1 16
     *     2 16
     *
     *     # Node ID, Node ID, Bandwidth, Delay
     *     0 1 10000000 1
     *     1 2 10000000 117
     * </pre>
     *
     * @param path Path to the topology file.
     * @return NetworkGraph object with all read content.
     * @throws IOException If any errors during file reads occur.
     */
    public static NetworkGraph readFromFile(String path) throws IOException {
        LineNumberReader lnr = new LineNumberReader(new FileReader(path));
        String line;
        int nr = 0;
        int anzNodes = 0;
        int anzLinks = 0;
        NetworkGraph ng = new NetworkGraph();

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

                ng.addNode(mNode.group(1),
                        parseOrInfty(mNode.group(2)),
                        parseOrInfty(mNode.group(3)),
                        parseOrInfty(mNode.group(4)));
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

                ng.addLink(n1, n2, parseOrInfty(mLink.group(3)) / 1000.0, Double.parseDouble(mLink.group(4)));
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
