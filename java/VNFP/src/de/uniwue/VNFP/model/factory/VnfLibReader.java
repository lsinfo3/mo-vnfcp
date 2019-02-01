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
 * <pre>
 *     [resources]
 *     Resource-Name-1
 *     Resource-Name-2
 *     ...
 *
 *     [vnfs]
 *     Vnf-Name-1, Delay-1, Capacity-1, Max-Instances-1, Flow-Migration-Penalty-1, Resource-1.1, Resource-1.2, ...
 *     Vnf-Name-2, Delay-2, Capacity-2, Max-Instances-2, Flow-Migration-Penalty-2, Resource-2.1, Resource-2.2, ...
 *     ...
 *
 *     [abbrev]
 *     Vnf-Alias-1, Vnf-Replacement-1.1, Vnf-Replacement-1.2, ...
 *     Vnf-Alias-2, Vnf-Replacement-2.1, Vnf-Replacement-2.2, ...
 *     ...
 *
 *     [pairs]
 *     Vnf-1.1, Vnf-1.2, Max-Latency-1
 *     Vnf-2.1, Vnf-2.2, Max-Latency-2
 *     ...
 * </pre>
 * <p>
 * Example:
 * <pre>
 *     [resources]
 *     CPU_Cores
 *     RAM
 *     HDD_Size
 *
 *     [vnfs]
 *     # VNF Name,   Delay, Capacity, Max Instances, Flow Migration Penalty, Cores, RAM,   HDD
 *     Firewall,     45,    900000,   -1,            10,                     4,     4000,  1
 *     Proxy,        40,    900000,   -1,            0,                      4,     4000,  1
 *     IDS,          1,     600000,   -1,            0,                      8,     8000,  1
 *     NAT,          10,    900000,   -1,            2,                      2,     2000,  1
 *
 *     [abbrev]
 *     # Define abbreviations: use predefined sub-chains in requests
 *     # VNF-Alias, VNF1, VNF2, VNF3, ...
 *     # --empty
 *
 *     [pairs]
 *     # Define VNF pairs that should be closely connected:
 *     # VNF1, VNF2, Max Latency between them (μs)
 *     # --empty
 * </pre>
 *
 * @author alex
 */
public class VnfLibReader {
    // Patterns for the lines:
    private static Pattern resourcePattern = Pattern.compile("([^;,]+)");
    private static String vnfPatternS = "([^;,]+),\\s*(\\d+(?:\\.\\d+)?)\\s*,\\s*((?:-1)|(?:\\d+(?:\\.\\d+)?))\\s*,\\s*((?:-1)|(?:\\d+))\\s*,\\s*(\\d+(?:\\.\\d+)?)\\s*";
    private static String vnfPatternResourceSuffixS = ",\\s*(\\d+(?:\\.\\d+)?)\\s*";
    private static Pattern abbrevPattern = Pattern.compile("([^;,]+),([^;,]+(?:,[^;,]+)*)");
    private static Pattern pairsPattern = Pattern.compile("([^;,]+),([^;,]+),\\s*(\\d+(?:\\.\\d+)?)\\s*");

    /**
     * This method reads a VNF specification file.
     * For the file format, cf. the main class documentation {@link VnfLibReader}.
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
     * For the file format, cf. the main class documentation {@link VnfLibReader}.
     *
     * @param path Path towards the VNF specification file.
     * @return {@link VnfLib} object with all read content..
     * @throws IOException If any errors during file reads occur.
     */
    public static VnfLib readFromFile(String path) throws IOException {
        LineNumberReader lnr = new LineNumberReader(new FileReader(path));
        VnfLib lib = new VnfLib();
        int mode = 0; // 0=resources, 1=vnfs, 2=abbrev, 3=pairs
        Pattern vnfPattern = null;
        String line;

        while ((line = lnr.readLine()) != null) {
            // Skip: empty lines and comments (# hash)
            if (line.trim().isEmpty() || line.trim().startsWith("#")) {
                continue;
            }

            // Mode:
            if (line.trim().toLowerCase().equals("[resources]")) {
                if (lib.res.length != 0) {
                    lnr.close();
                    throw new IOException("Only one resource declaration in VnfLib possible (line '" + line + "')");
                }

                mode = 0;
                continue;
            }
            if (line.trim().toLowerCase().equals("[vnfs]")) {
                if (lib.res.length == 0) {
                    lnr.close();
                    throw new IOException("Resources must be declared before VNFs in VnfLib (line '" + line + "')");
                }

                mode = 1;
                StringBuilder pattern = new StringBuilder(vnfPatternS);
                for (int i = 0; i < lib.res.length; i++) {
                    pattern.append(vnfPatternResourceSuffixS);
                }
                vnfPattern = Pattern.compile(pattern.toString());
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

            // New resources:
            if (mode == 0) {
                Matcher m = resourcePattern.matcher(line);
                if (!m.matches()) {
                    lnr.close();
                    throw new IOException("line '" + line + "' does not match pattern '" + resourcePattern.pattern() + "'");
                }
                lib.addResource(m.group(1));
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
                double delay = Double.parseDouble(m.group(2).trim());
                double capacity = parseOrInfty(m.group(3).trim()) / 1000.0;
                long maxInstances = Long.parseLong(m.group(4).trim());
                double flowMigrationPenalty = Double.parseDouble(m.group(5).trim());

                double[] res = new double[lib.res.length];
                for (int i = 0; i < res.length; i++) {
                    res[i] = Double.parseDouble(m.group(6 + i).trim());
                }

                // Create object and save it:
                VNF vnf = new VNF(vnfName, delay, capacity, maxInstances, flowMigrationPenalty, res);
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

            // New pair:
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
