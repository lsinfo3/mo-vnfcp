package de.lexej.VNFP.model.solution.overview;

import de.lexej.VNFP.model.Node;
import de.lexej.VNFP.model.VNF;
import de.lexej.VNFP.model.solution.VnfInstances;

import java.util.HashMap;
import java.util.Objects;

/**
 * Utility-class to summarize information about different types of VNFs.
 *
 * @author alex
 */
public class VnfTypeOverview {
    /**
     * VNF type that will be summarized.
     */
    public final VNF vnf;
    /**
     * Node -> Instances mapping containing further information in VNF loads.
     */
    public final HashMap<Node, VnfInstances> locations;

    private int total;

    /**
     * Creates a new overview-object for a specific type of VNF.
     *
     * @param vnf VNF type that will be summarized.
     */
    public VnfTypeOverview(VNF vnf) {
        this.vnf = Objects.requireNonNull(vnf);
        this.locations = new HashMap<>();
        this.total = 0;
    }

    /**
     * Creates a new overview-object for a specific type of VNF.
     * Copys the given locations for its internal set (used by the copy() method).
     *
     * @param vnf                    VNF type that will be summarized.
     * @param locations              Initial set of VNF Locations (e.g. when copied from another VnfTypeOverfiew-Object).
     * @param totalNumberOfInstances Number of Instances in the given locations.
     */
    private VnfTypeOverview(VNF vnf, HashMap<Node, VnfInstances> locations, int totalNumberOfInstances) {
        this.vnf = Objects.requireNonNull(vnf);
        this.locations = new HashMap<>(Objects.requireNonNull(locations));
        this.total = totalNumberOfInstances;
    }

    /**
     * Adds a new location for this type of VNF to the overview.
     *
     * @param node      Location of the new VNF instances.
     * @param instances VnfInstances-object containing further information.
     */
    public void addLocation(Node node, VnfInstances instances) {
        Objects.requireNonNull(node);
        Objects.requireNonNull(instances);

        locations.put(node, instances);
        total += instances.loads.length;
    }

    /**
     * Removes a location from this overview.
     *
     * @param node Location of the VNF instances.
     */
    public void removeLocation(Node node) {
        Objects.requireNonNull(node);

        VnfInstances removed = locations.get(node);
        if (removed != null) {
            total -= removed.loads.length;
            locations.remove(node);
        }
    }

    /**
     * @return Total number of VNF instances that were added via {@code addLocation()}.
     */
    public int getTotal() {
        return total;
    }

    /**
     * @return A new VnfTypeOverview Object with the same contents.
     */
    public VnfTypeOverview copy() {
        return new VnfTypeOverview(vnf, locations, total);
    }
}
