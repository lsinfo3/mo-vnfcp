package de.uniwue.VNFP.model;

import java.util.Objects;

/**
 * Objects of this class represent links in the network, or edges in the graph,
 * between two {@link Node} objects. The default case is undirected.
 *
 * @author alex
 */
public class Link {
    /**
     * First node of this link.
     */
    public final Node node1;
    /**
     * Second node of this link.
     */
    public final Node node2;
    /**
     * Available bandwidth. (Mbps)
     */
    public final double bandwidth;
    /**
     * Latency. (μs)
     */
    public final double delay;

    /**
     * Creates a new instance.
     *
     * @param node1     First node of this link.
     * @param node2     Second node of this link.
     * @param bandwidth Available bandwidth. (Mbps)
     * @param delay     Latency. (μs)
     */
    public Link(Node node1, Node node2, double bandwidth, double delay) {
        if (node1.equals(node2)) {
            throw new IllegalArgumentException("node linked to itself");
        }

        this.node1 = Objects.requireNonNull(node1);
        this.node2 = Objects.requireNonNull(node2);
        this.bandwidth = bandwidth;
        this.delay = delay;
    }

    /**
     * Returns the other node, that does *not* equal the given argument {@code n}.
     * This is useful to access neighbors of a node.
     *
     * @param n The node whose neighbor is desired.
     * @return {@code node1.equals(n) ? node2 : node1}
     */
    public Node getOther(Node n) {
        return node1.equals(n) ? node2 : node1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Link l = (Link) o;

        return (node1.equals(l.node1) && node2.equals(l.node2))
                || (node1.equals(l.node2) && node2.equals(l.node1));

    }

    @Override
    public int hashCode() {
        return node1.hashCode() * node2.hashCode();
    }

    @Override
    public String toString() {
        return "Link{" +
                "node1=" + node1.name +
                ", node2=" + node2.name +
                ", bandwidth=" + bandwidth +
                ", delay=" + delay +
                '}';
    }
}
