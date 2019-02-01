package de.uniwue.VNFP.model;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Objects of this class represent traffic demands.
 * They include ingress and egress nodes, required bandwidth,
 * requested maximum delays and an array of requested {@link VNF}s,
 * which are to be applied on a path between ingress and egress..
 *
 * @author alex
 */
public class TrafficRequest implements Comparable<TrafficRequest> {
    /**
     * Source of this demand's traffic.
     */
    public final Node ingress;
    /**
     * Destination of this demand's traffic.
     */
    public final Node egress;
    /**
     * Required bandwidth of this flow. (Mb/s)
     */
    public final double bandwidthDemand;
    /**
     * Maximum allowed latency of this flow's traffic. (μs)
     */
    public final double expectedDelay;
    /**
     * Array of {@link VNF}s which should be applied to this flow.
     */
    public final VNF[] vnfSequence;
    /**
     * Unique ID for equals/hashCode (in case of 2 distinct requests with equal ingress, egress, ...)
     */
    public final int id;
    private double shortestDelay;
    private double shortestHops;

    /**
     * Erzeugt ein neues TrafficRequest-Objekt.
     *
     * @param id              Unique ID for equals/hashCode (in case of 2 distinct requests with equal ingress, egress, ...)
     * @param ingress         Source of this demand's traffic.
     * @param egress          Destination of this demand's traffic.
     * @param bandwidthDemand Required bandwidth of this flow. (Mb/s)
     * @param expectedDelay   Maximum allowed latency of this flow's traffic. (μs)
     * @param vnfSequence     Array of {@link VNF}s which should be applied to this flow.
     */
    public TrafficRequest(int id, Node ingress, Node egress, double bandwidthDemand, double expectedDelay, VNF[] vnfSequence) {
        this.id = id;
        this.ingress = Objects.requireNonNull(ingress);
        this.egress = Objects.requireNonNull(egress);
        this.bandwidthDemand = bandwidthDemand;
        this.expectedDelay = expectedDelay;
        this.vnfSequence = Objects.requireNonNull(vnfSequence);
        this.shortestDelay = Double.POSITIVE_INFINITY;
        this.shortestHops = Double.POSITIVE_INFINITY;

        // Sanity-Check:
        for (VNF vnf : vnfSequence) {
            if (vnf == null) {
                throw new NullPointerException("vnfSequence contains null");
            }
            if (vnf.processingCapacity < bandwidthDemand) {
                throw new IllegalArgumentException("processingCapacity of " + vnf + " too small for bandwidth " + bandwidthDemand);
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TrafficRequest that = (TrafficRequest) o;

        return id == that.id;
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public String toString() {
        return "TrafficRequest{" +
                "ingress=" + ingress +
                ", egress=" + egress +
                ", bandwidthDemand=" + bandwidthDemand +
                ", expectedDelay=" + expectedDelay +
                ", vnfSequence=" + Arrays.toString(vnfSequence) +
                '}';
    }

    /**
     * Alternative String representation of [BCAB15].
     *
     * @return CSV-Darstellung dieses Requests.
     */
    public String toOldCsvFormat() {
        String ret = "0,"+ingress.name+","+egress.name+","+ NetworkGraph.noDigits(bandwidthDemand*1000.0)+","+ NetworkGraph.noDigits(expectedDelay)+",0.00000010";
        for (VNF vnf : vnfSequence) {
            ret += "," + vnf.name.toLowerCase();
        }
        return ret;
    }

    /**
     * Returns the latency of a shortest possible (ingress -> VNF -> egress) path.
     *
     * @param backpointer Pointer towards previous nodes on a Dijkstra path.
     * @return delay(shortest path) without the VNFs' latencies
     */
    public double getShortestDelay(HashMap<Node, HashMap<Node, Node.Att>> backpointer) {
        if (shortestDelay == Double.POSITIVE_INFINITY) {
            shortestDelay = getShortest(backpointer);
        }
        return shortestDelay;
    }

    /**
     * Returns the hop count of a shortest possible (ingress -> VNF -> egress) path.
     *
     * @param backpointer Pointer towards previous nodes on a BFS path.
     * @return number_of_hops(shortest path)
     */
    public double getShortestHops(HashMap<Node, HashMap<Node, Node.Att>> backpointer) {
        if (shortestHops == Double.POSITIVE_INFINITY) {
            shortestHops = getShortest(backpointer);
        }
        return shortestHops;
    }

    /**
     * Returns the distance of a shortest possible (ingress -> VNF -> egress) path,
     * depending on the given backpointer.
     *
     * @param backpointer Pointer towards previous nodes (BFS or Dijkstra).
     * @return distance(shortest path)
     */
    private double getShortest(HashMap<Node, HashMap<Node, Node.Att>> backpointer) {
        if (vnfSequence.length == 0) {
            return backpointer.get(ingress).get(egress).d;
        }

        double ret = Double.POSITIVE_INFINITY;

        // Find a node between ingress and egress that can host VNFs:
        for (Map.Entry<Node, HashMap<Node, Node.Att>> e : backpointer.entrySet()) {
            if (Arrays.stream(e.getKey().resources).anyMatch(d -> d > 0.0)) {
                double d = backpointer.get(ingress).get(e.getKey()).d + e.getValue().get(egress).d;
                if (d < ret) {
                    ret = d;
                }
            }
        }
        return ret;
    }

    @Override
    public int compareTo(TrafficRequest o) {
        return Double.compare(bandwidthDemand, o.bandwidthDemand);
    }
}
