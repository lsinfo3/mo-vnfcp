package de.lexej.VNFP.model.solution.overview;

import de.lexej.VNFP.model.Link;
import de.lexej.VNFP.model.TrafficRequest;

import java.util.HashMap;

/**
 * This class can be used to acquire information about a link's usage
 * and its capacities.
 *
 * @author alex
 */
public class LinkOverview {
    /**
     * The corresponding link instance.
     */
    public final Link link;
    /**
     * Includes flows that utilize this link (added via <code>addRequest()</code>).
     * The value of the map indicates how often these requests use the link.
     */
    public final HashMap<TrafficRequest, Integer> requests;

    private double remainingBandwidth;

    /**
     * Creates a new LinkOverview instance.
     *
     * @param link The corresponding link instance.
     */
    public LinkOverview(Link link) {
        this.link = link;
        this.remainingBandwidth = link.bandwidth;
        this.requests = new HashMap<>();
    }

    /**
     * Adds a request whose assigned path utilizes this link.
     *
     * @param req The TrafficRequest object whose TrafficAssignment uses this link.
     */
    public void addRequest(TrafficRequest req) {
        remainingBandwidth -= req.bandwidthDemand;

        if (requests.get(req) == null) {
            requests.put(req, 1);
        }
        else {
            requests.put(req, requests.get(req) + 1);
        }
    }

    /**
     * Removes a previously added TrafficRequest.
     *
     * @param req The request whose path was changed.
     */
    public void removeRequest(TrafficRequest req) {
        remainingBandwidth += req.bandwidthDemand;

        requests.put(req, requests.get(req) - 1);
        if (requests.get(req) == 0) {
            requests.remove(req);
        }
    }

    /**
     * Calculates the remaining bandwidth after subtract all used
     * bandwidths from the initial capacity.
     * Can be negative, which indicates that constraints are not satisfied.
     *
     * @return Remaining bandwidth for this link.
     */
    public double remainingBandwidth() {
        return remainingBandwidth;
    }

    /**
     * Creates a copy of this instance.
     *
     * @return A new LinkOverview object with the same content.
     */
    public LinkOverview copy() {
        LinkOverview other = new LinkOverview(link);
        other.remainingBandwidth = remainingBandwidth;
        other.requests.putAll(requests);
        return other;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LinkOverview that = (LinkOverview) o;

        return link.equals(that.link);
    }

    @Override
    public int hashCode() {
        return link.hashCode();
    }
}
