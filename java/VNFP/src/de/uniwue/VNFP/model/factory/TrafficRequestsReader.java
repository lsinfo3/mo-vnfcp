package de.uniwue.VNFP.model.factory;

import de.uniwue.VNFP.model.*;

import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class reads {@link TrafficRequest}s from a file and
 * returns them in an array.
 * It requires the underlying {@link NetworkGraph}.
 * <p>
 * File format:
 * <pre>
 *     [Ingress ID],[Egress ID],[Min. Bandwidth (kbps)],[Max. Delay (μs)],[VNF,VNF,VNF,...]
 *     [Ingress ID],[Egress ID],[Min. Bandwidth (kbps)],[Max. Delay (μs)],[VNF,VNF,VNF,...]
 *     [Ingress ID],[Egress ID],[Min. Bandwidth (kbps)],[Max. Delay (μs)],[VNF,VNF,VNF,...]
 *     ...
 * </pre>
 * <p>
 * Example:
 * <pre>
 *     # Ingress-ID, Egress-ID, Min-Bandwidth, Max-Delay, VNF, VNF, VNF, ...
 *     0,1,54021,721,proxy,ids,firewall
 *     0,2,78440,735,ids,proxy,firewall
 *     0,3,49523,715,ids,proxy,firewall
 * </pre>
 *
 * @author alex
 */
public class TrafficRequestsReader {
    // Pattern for the lines:
    private static Pattern linePattern = Pattern.compile("([^ ,]+) *, *([^ ,]+) *, *(\\d+(?:\\.\\d+)?) *, *((?:-1)|(?:\\d+(?:\\.\\d+)?))(?: *, *([^ ,]+(?: *, *[^ ,]+)*)?)?");

    /**
     * This method reads {@link TrafficRequest}s from a file and
     * returns them in an array.
     * It requires the underlying {@link NetworkGraph}.
     * <p>
     * File format:
     * <pre>
     *     [Ingress ID],[Egress ID],[Min. Bandwidth (kbps)],[Max. Delay (μs)],[VNF,VNF,VNF,...]
     *     [Ingress ID],[Egress ID],[Min. Bandwidth (kbps)],[Max. Delay (μs)],[VNF,VNF,VNF,...]
     *     [Ingress ID],[Egress ID],[Min. Bandwidth (kbps)],[Max. Delay (μs)],[VNF,VNF,VNF,...]
     *     ...
     * </pre>
     * <p>
     * Example:
     * <pre>
     *     # Ingress-ID, Egress-ID, Min-Bandwidth, Max-Delay, VNF, VNF, VNF, ...
     *     0,1,54021,721,proxy,ids,firewall
     *     0,2,78440,735,ids,proxy,firewall
     *     0,3,49523,715,ids,proxy,firewall
     * </pre>
     *
     * @param path  Path to the requests file.
     * @param graph The underlying network graph.
     * @return Array of {@link TrafficRequest} objects with all read content.
     * @throws IOException If any errors during file reads occur.
     */
    public static TrafficRequest[] readFromFile(Path path, NetworkGraph graph, VnfLib vnfLib) throws IOException {
        return readFromFile(path.toAbsolutePath().toString(), graph, vnfLib);
    }

    /**
     * This method reads {@link TrafficRequest}s from a file and
     * returns them in an array.
     * It requires the underlying {@link NetworkGraph}.
     * <p>
     * File format:
     * <pre>
     *     [Ingress ID],[Egress ID],[Min. Bandwidth (kbps)],[Max. Delay (μs)],[VNF,VNF,VNF,...]
     *     [Ingress ID],[Egress ID],[Min. Bandwidth (kbps)],[Max. Delay (μs)],[VNF,VNF,VNF,...]
     *     [Ingress ID],[Egress ID],[Min. Bandwidth (kbps)],[Max. Delay (μs)],[VNF,VNF,VNF,...]
     *     ...
     * </pre>
     * <p>
     * Example:
     * <pre>
     *     # Ingress-ID, Egress-ID, Min-Bandwidth, Max-Delay, VNF, VNF, VNF, ...
     *     0,1,54021,721,proxy,ids,firewall
     *     0,2,78440,735,ids,proxy,firewall
     *     0,3,49523,715,ids,proxy,firewall
     * </pre>
     *
     * @param path  Path to the requests file.
     * @param graph The underlying network graph.
     * @return Array of {@link TrafficRequest} objects with all read content.
     * @throws IOException If any errors during file reads occur.
     */
    public static TrafficRequest[] readFromFile(String path, NetworkGraph graph, VnfLib vnfLib) throws IOException {
        LineNumberReader lnr = new LineNumberReader(new FileReader(path));
        ArrayList<TrafficRequest> requests = new ArrayList<>();
        String line;
        int id = 0;

        while ((line = lnr.readLine()) != null) {
            // Skip: empty lines and comments (# hash)
            if (line.trim().isEmpty() || line.trim().startsWith("#")) {
                continue;
            }

            Matcher m = linePattern.matcher(line);
            if (!m.matches()) {
                lnr.close();
                throw new IOException("line '" + line + "' does not match pattern '" + linePattern.pattern() + "'");
            }

            // Turn String groups into objects:
            Node ingress = graph.getNodes().get(m.group(1));
            Node egress = graph.getNodes().get(m.group(2));
            if (ingress == null) {
                lnr.close();
                throw new IOException("Node '" + m.group(1) + "' does not exist");
            }
            if (egress == null) {
                lnr.close();
                throw new IOException("Node '" + m.group(2) + "' does not exist");
            }

            double minBandwidth = Double.parseDouble(m.group(3)) / 1000.0;
            double maxDelay = parseOrInfty(m.group(4));
            ArrayList<VNF> sequence = new ArrayList<>();
            if (m.group(5) != null) {
                String[] vnfs = m.group(5).split(",");
                for (String vnf : vnfs) {
                    VNF[] current = vnfLib.fromString(vnf);
                    if (current == null) {
                        lnr.close();
                        throw new IOException("VNF '" + vnf + "' unknown");
                    }
                    sequence.addAll(Arrays.asList(current));
                }
            }

            // Create object and save it:
            TrafficRequest req = new TrafficRequest(id, ingress, egress, minBandwidth, maxDelay, sequence.toArray(new VNF[sequence.size()]));
            requests.add(req);
            id++;
        }

        lnr.close();

        // ArrayList -> Array
        return requests.toArray(new TrafficRequest[requests.size()]);
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
