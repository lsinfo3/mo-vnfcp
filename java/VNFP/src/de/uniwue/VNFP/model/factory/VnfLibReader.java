package de.uniwue.VNFP.model.factory;

import de.uniwue.VNFP.model.VNF;
import de.uniwue.VNFP.model.VnfLib;

import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class reads a VNF specification file.
 * File format:
 * <p>
 * <pre>
 *     [VNF Name], [Cores], [RAM (MB)], [HDD (GB)], [Delay (μs)], [Capacity (kb/s)], [Max Instances]
 *     [VNF Name], [Cores], [RAM (MB)], [HDD (GB)], [Delay (μs)], [Capacity (kb/s)], [Max Instances]
 *     [VNF Name], [Cores], [RAM (MB)], [HDD (GB)], [Delay (μs)], [Capacity (kb/s)], [Max Instances]
 *     ...
 * </pre>
 * <p>
 * Example:
 * <pre>
 *     # VNF Name, Cores, RAM,   HDD, Delay, Capacity, Max Instances
 *     Firewall,   4,     4000,  1,   45,    900000,   15
 *     Proxy,      4,     4000,  1,   40,    900000,   7
 *     IDS,        8,     8000,  1,   1,     600000,   15
 *     NAT,        2,     2000,  1,   10,    900000,   -1
 * </pre>
 *
 * @author alex
 */
public class VnfLibReader {
    // Patterns for the lines:
    private static Pattern vnfPattern = Pattern.compile("([^;,]+),\\s*(\\d+(?:\\.\\d+)?)\\s*,\\s*(\\d+(?:\\.\\d+)?)\\s*,\\s*(\\d+(?:\\.\\d+)?)\\s*,\\s*(\\d+(?:\\.\\d+)?)\\s*,\\s*((?:-1)|(?:\\d+(?:\\.\\d+)?))\\s*,\\s*((?:-1)|(?:\\d+))\\s*");
    private static Pattern abbrevPattern = Pattern.compile("([^;,]+),([^;,]+(?:,[^;,]+)*)");
    private static Pattern pairsPattern = Pattern.compile("([^;,]+),([^;,]+),\\s*(\\d+(?:\\.\\d+)?)\\s*");

    /**
     * This method reads a VNF specification file.
     * File format:
     * <p>
     * <pre>
     *     [VNF Name], [Cores], [RAM (MB)], [HDD (GB)], [Delay (μs)], [Capacity (kb/s)], [Max Instances]
     *     [VNF Name], [Cores], [RAM (MB)], [HDD (GB)], [Delay (μs)], [Capacity (kb/s)], [Max Instances]
     *     [VNF Name], [Cores], [RAM (MB)], [HDD (GB)], [Delay (μs)], [Capacity (kb/s)], [Max Instances]
     *     ...
     * </pre>
     * <p>
     * Example:
     * <pre>
     *     # VNF Name, Cores, RAM,   HDD, Delay, Capacity, Max Instances
     *     Firewall,   4,     4000,  1,   45,    900000,   15
     *     Proxy,      4,     4000,  1,   40,    900000,   7
     *     IDS,        8,     8000,  1,   1,     600000,   15
     *     NAT,        2,     2000,  1,   10,    900000,   -1
     * </pre>
     *
     * @param path Path towards the VNF specification file.
     * @return {@link VnfLib} object with all read content..
     * @throws IOException If any errors during file reads occur.
     */
    public static VnfLib readFromFile(Path path) throws IOException {
        return readFromFile(path.toAbsolutePath().toString());
    }

    /**
     * This method reads a VNF specification file.
     * File format:
     * <p>
     * <pre>
     *     [VNF Name], [Cores], [RAM (MB)], [HDD (GB)], [Delay (μs)], [Capacity (kb/s)], [Max Instances]
     *     [VNF Name], [Cores], [RAM (MB)], [HDD (GB)], [Delay (μs)], [Capacity (kb/s)], [Max Instances]
     *     [VNF Name], [Cores], [RAM (MB)], [HDD (GB)], [Delay (μs)], [Capacity (kb/s)], [Max Instances]
     *     ...
     * </pre>
     * <p>
     * Example:
     * <pre>
     *     # VNF Name, Cores, RAM,   HDD, Delay, Capacity, Max Instances
     *     Firewall,   4,     4000,  1,   45,    900000,   15
     *     Proxy,      4,     4000,  1,   40,    900000,   7
     *     IDS,        8,     8000,  1,   1,     600000,   15
     *     NAT,        2,     2000,  1,   10,    900000,   -1
     * </pre>
     *
     * @param path Path towards the VNF specification file.
     * @return {@link VnfLib} object with all read content..
     * @throws IOException If any errors during file reads occur.
     */
    public static VnfLib readFromFile(String path) throws IOException {
        LineNumberReader lnr = new LineNumberReader(new FileReader(path));
        VnfLib lib = new VnfLib();
        int mode = 1;
        String line;

        while ((line = lnr.readLine()) != null) {
            // Skip: empty lines and comments (# hash)
            if (line.trim().isEmpty() || line.trim().startsWith("#")) {
                continue;
            }

            // Mode:
            if (line.trim().toLowerCase().equals("[vnfs]")) {
                mode = 1;
                continue;
            }
            if (line.trim().toLowerCase().equals("[abbrev]")) {
                mode = 2;
                continue;
            }
            if (line.trim().toLowerCase().equals("[pairs]")) {
                mode = 3;
                continue;
            }

            // New VNFs:
            if (mode == 1) {
                Matcher m = vnfPattern.matcher(line);
                if (!m.matches()) {
                    lnr.close();
                    throw new IOException("line '" + line + "' does not match pattern '" + vnfPattern.pattern() + "'");
                }

                // Turn String groups into objects:
                String vnfName = m.group(1).trim();
                double cores = Double.parseDouble(m.group(2).trim());
                double ram = Double.parseDouble(m.group(3).trim());
                double hdd = Double.parseDouble(m.group(4).trim());
                double delay = Double.parseDouble(m.group(5).trim());
                double capacity = parseOrInfty(m.group(6).trim()) / 1000.0;
                long maxInstances = Long.parseLong(m.group(7).trim());

                // Create object and save it:
                VNF vnf = new VNF(vnfName, cores, ram, hdd, delay, capacity, maxInstances);
                lib.addVnf(vnfName, new VNF[]{vnf});
            }

            // New abbreviation:
            else if (mode == 2) {
                Matcher m = abbrevPattern.matcher(line);
                if (!m.matches()) {
                    lnr.close();
                    throw new IOException("line '" + line + "' does not match pattern '" + abbrevPattern.pattern() + "'");
                }

                String stringAbbrev = m.group(1).trim().toLowerCase();
                String[] stringChain = m.group(2).split(",");
                ArrayList<VNF> vnfChain = new ArrayList<>();
                for (int i = 0; i < stringChain.length; i++) {
                    String current = stringChain[i].trim().toLowerCase();
                    VNF[] treffer = lib.fromString(current);
                    if (treffer == null) {
                        throw new IOException("VNF '" + stringAbbrev + "' not found for line: " + line);
                    }

                    vnfChain.addAll(Arrays.asList(treffer));
                }
                lib.addVnf(stringAbbrev, vnfChain.toArray(new VNF[vnfChain.size()]));
            }

            // Neue pair:
            else if (mode == 3) {
                Matcher m = pairsPattern.matcher(line);
                if (!m.matches()) {
                    lnr.close();
                    throw new IOException("line '" + line + "' does not match pattern '" + pairsPattern.pattern() + "'");
                }

                String vnf1 = m.group(1).trim().toLowerCase();
                String vnf2 = m.group(2).trim().toLowerCase();
                double latency = Double.parseDouble(m.group(3));

                // Search for VNF1:
                VNF[] treffer = lib.fromString(vnf1);
                if (treffer == null) {
                    throw new IOException("VNF '" + vnf1 + "' not found for line: " + line);
                }
                if (treffer.length != 1) {
                    throw new IOException("VnfPairs may not be defined for sub-chains/abbreviations, only single VNFs (attempted " + vnf1 + ")");
                }
                VNF vnf_a = treffer[0];

                // Search for VNF2:
                treffer = lib.fromString(vnf2);
                if (treffer == null) {
                    throw new IOException("VNF '" + vnf2 + "' not found for line: " + line);
                }
                if (treffer.length != 1) {
                    throw new IOException("VnfPairs may not be defined for sub-chains/abbreviations, only single VNFs (attempted " + vnf2 + ")");
                }
                VNF vnf_b = treffer[0];

                // Add VnfPair:
                lib.addPair(vnf_a, vnf_b, new VnfLib.VnfPair(vnf_a, vnf_b, latency));
            }
        }

        return lib;
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
