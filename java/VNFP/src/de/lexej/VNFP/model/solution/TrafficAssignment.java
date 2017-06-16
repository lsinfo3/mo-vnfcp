package de.lexej.VNFP.model.solution;

import de.lexej.VNFP.model.*;

import java.util.Arrays;
import java.util.Objects;

/**
 * This class represents a solution for a single {@link TrafficRequest} {@code req}.
 * It contains a path from {@code req.ingress} to {@code req.egress}, including an assignment of {@link VNF}s
 * to nodes. The internal representation of hops and assignments is given by an array of {@link NodeAssignment}s.
 *
 * @author alex
 */
public class TrafficAssignment {
    /**
     * The corresponding TrafficRequest that is solved by this object.
     */
    public final TrafficRequest request;
    /**
     * Contains NodeAssignments representing tuples {@code (Node, VNF)}
     * which indicate the path from {@code request.ingress} to {@code request.egress}
     * (inclusive), as well as applied {@link VNF}s on the nodes.
     * Can contain a single node multiple times if multiple VNFs are applied there.
     */
    public final NodeAssignment[] path;
    /**
     * The network topology.
     */
    public final NetworkGraph ng;
    /**
     * Sum of all VNF and link delays for this partial solution.
     */
    public final double delay;
    /**
     * Number of hops for this partial solution.
     */
    public final double numberOfHops;
    /**
     * Relation between this solution's delay and the shortest possible.
     */
    public final double delayIndex;
    /**
     * Relation between this solution's hop count and the shortest possible.
     */
    public final double hopsIndex;

    /**
     * Creates a new TrafficAssignment instance with the given content.
     *
     * @param request The corresponding TrafficRequest that is solved by this object.
     * @param path    Contains NodeAssignments representing tuples {@code (Node, VNF)}
     *                which indicate the path from {@code request.ingress} to {@code request.egress}.
     * @param ng      The network topology.
     */
    public TrafficAssignment(TrafficRequest request, NodeAssignment[] path, NetworkGraph ng) {
        this.request = Objects.requireNonNull(request);
        this.path = Objects.requireNonNull(path);
        this.ng = Objects.requireNonNull(ng);
        double d = 0.0;
        double hops = 0.0;

        for (int i = 0; i < path.length; i++) {
            NodeAssignment nAssig = path[i];

            // Set reverse pointers:
            nAssig.traffReq = request;
            nAssig.traffAss = this;

            if (nAssig.vnf != null){
                d += nAssig.vnf.delay;
            }
            if (nAssig.prev != null) {
                d += nAssig.prev.delay;
                hops++;
            }

            // Check links:
            if (i != 0) {
                int _i = i;
                if (nAssig.node.equals(path[i-1].node)) continue;
                if (path[i-1].node.getNeighbours().stream()
                        .anyMatch(l -> l.getOther(path[_i-1].node).equals(nAssig.node))) continue;

                throw new IllegalArgumentException("no link exists between " + path[i-1].node.name + " and " + nAssig.node.name);
            }
        }
        this.delay = d;
        this.numberOfHops = hops;
        d = d - Arrays.stream(request.vnfSequence).mapToDouble(vnf -> vnf.delay).sum();
        this.delayIndex = d / request.getShortestDelay(ng.getDijkstraBackpointers());
        this.hopsIndex = hops / request.getShortestHops(ng.getBfsBackpointers());

        if (!path[0].node.equals(request.ingress)) {
            throw new IllegalArgumentException("first node in path ("+path[0].node.name+") does not equal ingress ("+request.ingress.name+")");
        }
        if (!path[path.length - 1].node.equals(request.egress)) {
            throw new IllegalArgumentException("last node in path ("+path[path.length - 1].node.name+") does not equal egress ("+request.egress.name+")");
        }
    }

    @Override
    public String toString() {
        return "TrafficAssignment{" +
                "request=" + request +
                ", path=" + Arrays.toString(path) +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TrafficAssignment that = (TrafficAssignment) o;

        if (!request.equals(that.request)) return false;
        return Arrays.equals(path, that.path);
    }

    @Override
    public int hashCode() {
        return 31 * request.hashCode() + Arrays.hashCode(path);
    }
}
