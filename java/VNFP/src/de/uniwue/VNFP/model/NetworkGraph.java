package de.uniwue.VNFP.model;

import de.uniwue.VNFP.algo.FlowUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Represents the topology of (physical) nodes and links where the placement is to be performed on.
 *
 * @author alex
 */
public class NetworkGraph {
    /**
     * Indicates whether the edges in this graph are directed or bidirectional.
     */
    public final boolean directed;
    /**
     * Indicates whether the nodes were parsed with GeoCoordinates available for easier display.
     */
    public boolean hasGeoCoordinates;

    private HashMap<String, Node> nodes;
    private HashMap<Node, HashMap<Node, Node.Att>> backpointerDij;
    private HashMap<Node, HashMap<Node, Node.Att>> backpointerBfs;

    /**
     * Creates a new, empty graph.
     *
     * @param directed Indicates whether the edges in this graph are directed or bidirectional.
     */
    public NetworkGraph(boolean directed) {
        this.directed = directed;
        nodes = new HashMap<>();
    }

    /**
     * Creates a new node with the given resources and adds it to the graph
     *
     * @param name      Name / ID of the new node.
     * @param resources Amount of available computational resources.
     * @return Newly created Node object,
     */
    public Node addNode(String name, double[] resources) {
        Node n = new Node(name, resources);

        if (nodes.containsKey(name)) {
            throw new IllegalArgumentException("node " + n.name + " added twice");
        }

        nodes.put(name, n);
        backpointerDij = null;
        backpointerBfs = null;
        return n;
    }

    /**
     * Creates a new {@link Link} between the given nodes, and adds it to n1's neighbor list (or to both, if undirected).
     *
     * @param n1        First node of the link.
     * @param n2        Second node of the link.
     * @param bandwidth Available bandwidth. (Mbps)
     * @param delay     Latency of the link (μs).
     * @return Newly created Link object.
     */
    public Link addLink(Node n1, Node n2, double bandwidth, double delay) {
        backpointerDij = null;
        backpointerBfs = null;
        if (directed) return n1.addNeighbourDirected(n2, bandwidth, delay);
        else return n1.addNeighbour(n2, bandwidth, delay);
    }

    /**
     * Creates two new {@link Link}s between the given nodes, and adds them to each other's neighbor lists.
     * Only works for directed graphs.
     *
     * @param n1        First node of the link.
     * @param n2        Second node of the link.
     * @param bandwidth Available bandwidth. (Mbps)
     * @param delay     Latency of the link (μs).
     * @return Both newly created Link objects.
     */
    public Link[] addBothDirectedLinks(Node n1, Node n2, double bandwidth, double delay) {
        if (!directed) {
            throw new IllegalStateException("Attempting to add directed links to an undirected graph");
        }

        backpointerDij = null;
        backpointerBfs = null;
        Link l1 = n1.addNeighbourDirected(n2, bandwidth, delay);
        Link l2 = n2.addNeighbourDirected(n1, bandwidth, delay);
        return new Link[]{l1, l2};
    }

    /**
     * Returns the node map.
     *
     * @return A map with NodeName -> Node Object pointers.
     */
    public HashMap<String, Node> getNodes() {
        return nodes;
    }

    /**
     * Collects all links in the graph and returns the Collection.
     *
     * @return A HashSet containing all Links in the network.
     */
    public HashSet<Link> getLinks() {
        return nodes.values().stream()
                .map(Node::getNeighbors)
                .flatMap(HashSet::stream)
                .collect(Collectors.toCollection(HashSet::new));
    }

    /**
     * Returns shortest path pointers after a Dijkstra search.
     *
     * @return Backpointers after performing Dijkstra
     */
    public HashMap<Node, HashMap<Node, Node.Att>> getDijkstraBackpointers() {
        if (backpointerDij == null) {
            backpointerDij = new HashMap<>();
            for (Node n : nodes.values()) {
                backpointerDij.put(n, FlowUtils.dijkstra(n));
            }
        }
        return backpointerDij;
    }

    /**
     * Returns random (BFS or Dijkstra) shortest path pointers.
     *
     * @param r For custom random seeds.
     * @return Backpointers from either BFS or Dijkstra
     */
    public HashMap<Node, HashMap<Node, Node.Att>> getRandomBackpointers(Random r) {
        if (r.nextDouble() <= 0.5) return getBfsBackpointers();
        return getDijkstraBackpointers();
    }

    /**
     * Returns shortest path pointers after a BFS.
     *
     * @return Backpointers after performing BFS
     */
    public HashMap<Node, HashMap<Node, Node.Att>> getBfsBackpointers() {
        if (backpointerBfs == null) {
            backpointerBfs = new HashMap<>();
            for (Node n : nodes.values()) {
                backpointerBfs.put(n, FlowUtils.bfs(n));
            }
        }
        return backpointerBfs;
    }

    /**
     * Computes the shortest path start -> middle -> end, where middle is
     * a node from the choices-array. Returns the middle node of this
     * shortest path.
     *
     * @param start   Starting node of the desired path.
     * @param end     End node of the desired path.
     * @param choices All available nodes for the middle-choice. (e.g. all nodes with <tt>cpuCapacity > 0</tt>)
     * @param bp      Backpointers (and distanced) from a Dijkstra- or BFS-search.
     * @return The middle node of the shortest path found.
     */
    public Node getShortestMiddleStation(Node start, Node end, Node[] choices, HashMap<Node, HashMap<Node, Node.Att>> bp) {
        Objects.requireNonNull(start);
        Objects.requireNonNull(end);
        Objects.requireNonNull(choices);
        Objects.requireNonNull(bp);

        if (choices.length == 0) {
            throw new IllegalArgumentException("choices array is empty");
        }

        Node middle = null;
        double d = Double.POSITIVE_INFINITY;
        for (Node n : choices) {
            double current_d = bp.get(start).get(n).d + bp.get(n).get(end).d;
            if (current_d < d) {
                middle = n;
                d = current_d;
            }
        }

        return middle;
    }

    @Override
    public String toString() {
        HashSet<Link> links = getLinks();
        StringBuilder sb = new StringBuilder("# Number of nodes, Number of links");
        sb.append("\n").append(nodes.size()).append(",").append(links.size());
        sb.append("\n\n# Node ID, Resources");

        for (Node n : nodes.values()) {
            sb.append("\n").append(n.name);
            for (double d : n.resources) {
                sb.append(",").append(d);
            }
        }

        sb.append("\n\n# Node ID, Node ID, Bandwidth, Delay");

        for (Link l : links) {
            sb.append("\n").append(l.node1.name).append(",").append(l.node2.name)
                    .append(",").append(l.bandwidth).append(",").append(l.delay);
        }

        return sb.toString();
    }

    /**
     * Creates a DOT file from this NetworkGraph object.
     * This can be used to quickly draw the graph with graphviz methods.
     *
     * @return DOT file containing the graph.
     */
    public String toDotFile() {
        HashSet<Link> links = getLinks();
        StringBuilder sb = new StringBuilder("graph networkGraphTest {\n" +
                "  node [\n" +
                "    shape = \"circle\",\n" +
                "    style = \"filled\",\n" +
                "    fontsize = 16,\n" +
                "    fixedsize = true\n" +
                "  ];\n" +
                "\n" +
                "  edge [\n" +
                "    color = \"#bbbbbb\"\n" +
                "  ];\n" +
                "\n" +
                "  // nodes with CPU\n" +
                "  node [\n" +
                "    color = \"#007399\",\n" +
                "    fillcolor = \"#007399\",\n" +
                "    fontcolor = white\n" +
                "  ];\n");

        // Nodes with resources:
        nodes.values().stream().filter(n -> n.resources[0] > 0.0).forEach(n -> sb.append("  ").append(n.name).append(";\n"));

        sb.append("\n" +
                "  // nodes without CPU\n" +
                "  node [\n" +
                "    color = \"#4dd2ff\",\n" +
                "    fillcolor = \"#4dd2ff\",\n" +
                "    fontcolor = black\n" +
                "  ];\n");

        // Nodes without CPU resources:
        nodes.values().stream().filter(n -> n.resources[0] == 0.0).forEach(n -> sb.append("  ").append(n.name).append(";\n"));

        sb.append("\n" +
                "  // edges\n");

        // Edges:
        links.forEach(l -> sb.append("  ")
                .append(l.node1.name)
                .append(" -- ")
                .append(l.node2.name)
                .append(" [ label = \"")
                .append(Math.round(l.delay))
                .append("\" ];\n"));

        sb.append("}");

        return sb.toString();
    }

    /**
     * Returns the content of a double without its decimal points as a String.
     *
     * @param d The original number.
     * @return The same number without decimal places.
     */
    public static String noDigits(double d) {
        return String.format("%.0f", d);
    }
}
