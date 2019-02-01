package de.uniwue.VNFP.model;

import java.util.*;
import java.util.stream.Collectors;

/**
 * This class serves as a library, knowing all different types of VNF instances,
 * their abbreviations and their relations (which VNFs should be placed close together).
 * It also aids ifinding VNF subchains by name.
 *
 * @author alex
 */
public class VnfLib {
    private final ArrayList<String> resources;
    private final HashMap<String, VNF[]> vnfs;
    private final HashMap<String, VnfPair> pairs;

    /**
     * This variable is meant as a shortcut for {@link VnfLib#getResources}.
     * Its contents can be modified, so use it with caution.
     */
    public String[] res;

    /**
     * Number of known VNFs.
     */
    public int size;

    /**
     * Initializes a new library.
     */
    public VnfLib() {
        resources = new ArrayList<>();
        vnfs = new HashMap<>();
        pairs = new HashMap<>();
        res = new String[0];
    }

    /**
     * Adds the given resource String to the collection of known computational resources.
     *
     * @param res The name of the new resource (CPU, RAM, ...)
     */
    public void addResource(String res) {
        Objects.requireNonNull(res);
        for (String res2 : resources) {
            if (res.toLowerCase().trim().equals(res2.toLowerCase().trim())) {
                throw new IllegalArgumentException("Resource '" + res + "' is already added");
            }
        }
        resources.add(res);

        this.res = getResources();
    }

    /**
     * @return An array-representation of the internal resources set.1
     */
    public String[] getResources() {
        return resources.toArray(new String[resources.size()]);
    }

    /**
     * Adds the given VNF-subchain to the library under the given name.
     *
     * @param name  String representation of this chain.
     * @param chain All VNFs to be found under this name (usually only consisting of 1 instance)
     */
    public void addVnf(String name, VNF[] chain) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(chain);
        if (chain.length == 0) {
            throw new IllegalArgumentException("chain.length == 0");
        }
        if (name.trim().isEmpty()) {
            throw new IllegalArgumentException("name is empty");
        }

        vnfs.put(name.trim().toLowerCase(), chain);

        // backpointers to this library & size
        for (VNF[] vnfArray : vnfs.values()) {
            for (VNF vnf : vnfArray) {
                vnf.vnfLib = this;
            }
            size += vnfArray.length;
        }
    }

    /**
     * Adds the given pair to this library,
     * so their relationship can quickly be retrieved by {@code getPair()} later.
     *
     * @param vnf_a first VNF of the pair
     * @param vnf_b second VNF of the pair
     * @param pair  VnfPair instance, knowing the max acceptable latency on subchains VNF_A -> VNF_B
     */
    public void addPair(VNF vnf_a, VNF vnf_b, VnfPair pair) {
        Objects.requireNonNull(vnf_a);
        Objects.requireNonNull(vnf_b);
        Objects.requireNonNull(pair);

        pairs.put(vnf_a.name + "," + vnf_b.name, pair);
    }

    /**
     * Retrieves the corresponding VNF instance for the given String.
     * If the String belongs to an abbreviation, the entire sub-chain is returned.
     *
     * @param name name of the desired VNF instance, or name of their abbreviation
     * @return an array containing the wanted sub-chain, or {@code null}, if no matches are found.
     */
    public VNF[] fromString(String name) {
        Objects.requireNonNull(name);
        if (name.trim().isEmpty()) {
            throw new IllegalArgumentException("name is empty");
        }

        return vnfs.get(name.trim().toLowerCase());
    }

    /**
     * If there is a max latency defined for sub-chain [VNF_A, VNF_B],
     * the corresponding VnfPair instance is returned.
     *
     * @param vnf_a first VNF of the pair
     * @param vnf_b second VNF of the pair
     * @return VnfPair instance, knowing the max acceptable latency on subchains VNF_A -> VNF_B,
     * or {@code null}, if none is found
     */
    public VnfPair getPair(VNF vnf_a, VNF vnf_b) {
        return pairs.get(vnf_a.name + "," + vnf_b.name);
    }

    /**
     * @return A set containing all known VNF types.
     */
    public HashSet<VNF> getAllVnfs() {
        return vnfs.values().stream().flatMap(Arrays::stream).collect(Collectors.toCollection(HashSet::new));
    }

    /**
     * This data class acts as a container for the triple <tt>(VNF_A, VNF_B, max_latency)</tt>.
     */
    public static class VnfPair {
        /**
         * first VNF of the pair
         */
        public final VNF vnf_a;
        /**
         * second VNF of the pair
         */
        public final VNF vnf_b;
        /**
         * maximum allowed latency for sub-chains VNF_A -> VNF_B
         */
        public final double latency;

        /**
         * Initializes a new VnfPair-instance.
         *
         * @param vnf_a   first VNF of the pair
         * @param vnf_b   second VNF of the pair
         * @param latency maximum allowed latency for sub-chains VNF_A -> VNF_B
         */
        public VnfPair(VNF vnf_a, VNF vnf_b, double latency) {
            this.vnf_a = Objects.requireNonNull(vnf_a);
            this.vnf_b = Objects.requireNonNull(vnf_b);
            this.latency = latency;

            if (latency < 0) {
                throw new IllegalArgumentException("latency = " + latency);
            }
        }
    }
}
