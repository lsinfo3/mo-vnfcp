package de.uniwue.VNFP.model;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.stream.Collectors;

import de.uniwue.VNFP.model.solution.VnfInstances;
import de.uniwue.VNFP.util.Point;

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
     * Amount of available computational resources.
     */
    public double[] resources;
    /**
     * Relative position to display this node in a GUI. May be null if not available.
     */
    public Point geo;

    private HashSet<Link> neighbors;
    private HashSet<Link> inNeighbors;

    /**
     * Creates a new instance with the given name and resources. This node will be created without geo coordinates.
     *
     * @param name      Name / ID of this node.
     * @param resources Amount of available computational resources.
     */
    public Node(String name, double[] resources) {
        this(name, resources, null);
    }

    /**
     * Creates a new instance with the given name, resources and geo coordinates.
     *
     * @param name      Name / ID of this node.
     * @param resources Amount of available computational resources.
     * @param geo       Relative position to display this node in a GUI.
     */
    public Node(String name, double[] resources, Point geo) {
        this.name = Objects.requireNonNull(name);
        this.resources = Objects.requireNonNull(Arrays.copyOf(resources, resources.length));
        this.geo = geo;
        neighbors = new HashSet<>();
        inNeighbors = new HashSet<>();
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

        if (neighbors.contains(link) || neigh.neighbors.contains(link)) {
            throw new IllegalArgumentException("link " + link.node1.name + " - " + link.node2.name + " added twice");
        }

        neighbors.add(link);
        neigh.neighbors.add(link);
        inNeighbors.add(link);
        neigh.inNeighbors.add(link);

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

        if (neighbors.contains(link)) {
            throw new IllegalArgumentException("link " + link.node1.name + " -> " + link.node2.name + " added twice");
        }

        neighbors.add(link);
        neigh.inNeighbors.add(link);

        return link;
    }

    /**
     * Returns the Collection of this node's neighbors.
     *
     * @return A HashSet with all neighbor nodes.
     */
    public HashSet<Link> getNeighbors() {
        return neighbors;
    }


    /**
     * Returns a set of all links that end in this node.
     * Only makes sense in the directed (or full duplex) case.
     *
     * @return A HashSet with all links in the form of (*, this).
     */
    public HashSet<Link> getInLinks() {
        return inNeighbors.stream().filter(l -> l.node2.equals(this)).collect(Collectors.toCollection(HashSet::new));
    }

    /**
     * Returns a set of all links that start in this node.
     * Only makes sense in the directed (or full duplex) case.
     *
     * @return A HashSet with all links in the form of (this, *).
     */
    public HashSet<Link> getOutLinks() {
        return neighbors.stream().filter(l -> l.node1.equals(this)).collect(Collectors.toCollection(HashSet::new));
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
                ", resources=" + Arrays.toString(resources) +
                ", geo=" + geo +
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
