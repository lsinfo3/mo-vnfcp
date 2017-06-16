package de.lexej.VNFP.model;

import java.util.HashSet;
import java.util.Objects;

import de.lexej.VNFP.model.solution.VnfInstances;

/**
 * Objects from this class represent points of presence in the network.
 * They can be ingress and egress point of a {@link TrafficRequest},
 * host one or more {@link VnfInstances}, or simply forward traffic as an intermediate node.
 *
 * @author alex
 */
public class Node implements Comparable<Node> {
    /**
     * Name / ID of this node.
     */
    public final String name;
    /**
     * Number of available CPU cores.
     */
    public double cpuCapacity;
    /**
     * Amount of available RAM (Mb).
     */
    public double ramCapacity;
    /**
     * Amount of available HDD capacities (Gb).
     */
    public double hddCapacity;

    private HashSet<Link> neighbours;

    /**
     * Creates a new instance with the given contents.
     *
     * @param name        Name / ID of this node.
     * @param cpuCapacity Number of available CPU cores.
     * @param ramCapacity Amount of available RAM (Mb).
     * @param hddCapacity Amount of available HDD capacities (Gb).
     */
    public Node(String name, double cpuCapacity, double ramCapacity, double hddCapacity) {
        this.name = Objects.requireNonNull(name);
        this.cpuCapacity = cpuCapacity;
        this.ramCapacity = ramCapacity;
        this.hddCapacity = hddCapacity;
        neighbours = new HashSet<>();
    }

    /**
     * Adds the given node to this node's neighbors.
     * Creates a new {@link Link} object for this matter.
     * Also adds this node to the given node's neighbors (undirected link).
     *
     * @param neigh     New neighbor of this node.
     * @param bandwidth Available bandwidth of this link. (Mbps)
     * @param delay     Latency of the link. (μs)
     * @return Newly created Link object.
     */
    public Link addNeighbour(Node neigh, double bandwidth, double delay) {
        if (this.equals(neigh)) {
            throw new IllegalArgumentException("node linked to itself");
        }

        Link link = new Link(this, neigh, bandwidth, delay);

        if (neighbours.contains(link) || neigh.neighbours.contains(link)) {
            throw new IllegalArgumentException("link " + link.node1.name + " - " + link.node2.name + " added twice");
        }

        neighbours.add(link);
        neigh.neighbours.add(link);
        return link;
    }

    /**
     * Adds the given node to this node's neighbors.
     * Creates a new {@link Link} object for this matter.
     * Does not add this node to the given node's neighbors (directed link).
     *
     * @param neigh     New neighbor of this node.
     * @param bandwidth Available bandwidth of this link. (Mbps)
     * @param delay     Latency of the link. (μs)
     * @return Newly created Link object.
     */
    public Link addNeighbourDirected(Node neigh, double bandwidth, double delay) {
        if (this.equals(neigh)) {
            throw new IllegalArgumentException("node linked to itself");
        }

        Link link = new Link(this, neigh, bandwidth, delay);

        if (neighbours.contains(link)) {
            throw new IllegalArgumentException("link " + link.node1.name + " -> " + link.node2.name + " added twice");
        }

        neighbours.add(link);
        return link;
    }

    /**
     * Returns the Collection of this node's neighbors.
     *
     * @return A HashSet with all neighbor nodes.
     */
    public HashSet<Link> getNeighbours() {
        return neighbours;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Node node = (Node) o;

        return name.equals(node.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return "Node{" +
                "name='" + name + "'" +
                ", cpuCapacity=" + cpuCapacity +
                ", ramCapacity=" + ramCapacity +
                ", hddCapacity=" + hddCapacity +
                '}';
    }

    @Override
    public int compareTo(Node o) {
        return name.compareTo(o.name);
    }

    /**
     * Utility class with Dijkstra node attributes:
     * <pre>
     *     d (distance to the start node)
     *     pi (pointer towards the previous node)
     *     color (private access)
     * </pre>
     */
    public static class Att implements Comparable<Att> {
        public final Node node;
        public final double d;
        public final Link pi;
        public int color;

        public Att(Node node, int color, double d, Link pi) {
            this.node = node;
            this.color = color;
            this.d = d;
            this.pi = pi;
        }

        @Override
        public int compareTo(Att o) {
            return Double.compare(d, o.d);
        }
    }
}
