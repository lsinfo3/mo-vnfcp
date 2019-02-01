package de.uniwue.VNFP.algo;

import de.uniwue.VNFP.model.Link;
import de.uniwue.VNFP.model.NetworkGraph;
import de.uniwue.VNFP.model.Node;
import de.uniwue.VNFP.model.TrafficRequest;
import de.uniwue.VNFP.model.solution.NodeAssignment;
import de.uniwue.VNFP.model.solution.TrafficAssignment;

import java.util.*;

/**
 * Contains static methods to faciliate TrafficAssignment creation by
 * computing shortest paths and connecting given intermediate nodes.
 *
 * @author alex
 */
public class FlowUtils {
    /**
     * Performs a shortest path search (wrt. hops) from the given start (z.B. {@code req.ingress})
     * and returns a corresponding mapping (Node -> Backpointer).
     *
     * @param start Starting node of the search..
     * @return A path-mapping with minimal number of hops.
     */
    public static HashMap<Node, Node.Att> bfs(Node start) {
        Objects.requireNonNull(start);

        // Data structures
        HashMap<Node, Node.Att> att = new HashMap<>();
        LinkedList<Node.Att> q = new LinkedList<>();

        Node.Att startNode = new Node.Att(start, 1, 0, null);
        q.add(startNode);
        att.put(start, startNode);

        // Actual bfs:
        while (!q.isEmpty()) {
            Node.Att u = q.poll();
            for (Link l : u.node.getNeighbors()) {
                Node vNode = l.getOther(u.node);

                // If the neighbor has not been visited yet:
                if (!att.containsKey(vNode)) {
                    Node.Att vNeu = new Node.Att(vNode, 1, u.d + 1, l);
                    q.add(vNeu);
                    att.put(vNode, vNeu);
                }
            }
        }

        return att;
    }

    /**
     * Performs a shortest path search (wrt. delay) from the given start (z.B. {@code req.ingress})
     * and returns a corresponding mapping (Node -> Backpointer).
     *
     * @param start Starting node of the search..
     * @return A path-mapping with minimal delay.
     */
    public static HashMap<Node, Node.Att> dijkstra(Node start) {
        Objects.requireNonNull(start);

        // Data structures
        HashMap<Node, Node.Att> att = new HashMap<>();
        PriorityQueue<Node.Att> q = new PriorityQueue<>();

        Node.Att startNode = new Node.Att(start, 1, 0, null);
        q.add(startNode);
        att.put(start, startNode);

        // Actual Dijkstra:
        while (!q.isEmpty()) {
            Node.Att u = q.poll();
            if (u.color == 2) continue;

            for (Link l : u.node.getNeighbors()) {
                Node vNode = l.getOther(u.node);
                Node.Att v = (att.containsKey(vNode) ? att.get(vNode) : new Node.Att(vNode, 0, Double.POSITIVE_INFINITY, null));

                if (v.d > u.d + l.delay) {
                    //v.color = 1;
                    //v.d = u.d + l.delay;
                    //v.pi = l;
                    Node.Att vNeu = new Node.Att(vNode, 1, u.d + l.delay, l);
                    q.add(vNeu);
                    att.put(vNode, vNeu);
                }
            }
            u.color = 2;
        }

        return att;
    }

    /**
     * Find the shortest path (wrt. hops) that contains all nodes in <tt>order</tt>
     * as well as ingress and egress of the request..
     *
     * @param req   TrafficRequest containing VNF-sequence, ingress und egress nodes.
     * @param order The locations of the requested VNFs in the respective order.
     * @param ng    The graph of the network topology.
     * @return TrafficAssignment with the shortest path:
     * <tt>Ingress -> ... -> VNF_1 -> ... -> VNF_2 -> ... -> VNF_n -> ... -> Egress</tt>.
     */
    public static TrafficAssignment fromVnfSequence(TrafficRequest req, Node[] order, NetworkGraph ng) {
        return fromVnfSequence(req, order, ng, ng.getBfsBackpointers());
    }

    /**
     * Find the shortest path (wrt. the given backpointers) that contains all nodes in <tt>order</tt>
     * as well as ingress and egress of the request..
     *
     * @param req          TrafficRequest containing VNF-sequence, ingress und egress nodes.
     * @param order        The locations of the requested VNFs in the respective order.
     * @param ng           The graph of the network topology.
     * @param backpointers Result of a shortest path search.
     * @return TrafficAssignment with the shortest path:
     * <tt>Ingress -> ... -> VNF_1 -> ... -> VNF_2 -> ... -> VNF_n -> ... -> Egress</tt>.
     */
    public static TrafficAssignment fromVnfSequence(TrafficRequest req, Node[] order, NetworkGraph ng, HashMap<Node, HashMap<Node, Node.Att>> backpointers) {
        Objects.requireNonNull(req);
        Objects.requireNonNull(order);
        Objects.requireNonNull(ng);
        Objects.requireNonNull(backpointers);

        if (order.length != req.vnfSequence.length) {
            throw new IllegalArgumentException("array length mismatch: order.length = " + order.length + ", req.vnfSequence.length = " + req.vnfSequence.length + ":\n"
                + "order=" + Arrays.toString(order) + "\n"
                + "req.vnfSequence=" + Arrays.toString(req.vnfSequence));
        }

        // Define path: (including ingress, excluding egress)
        ArrayList<NodeAssignment> path = new ArrayList<>();
        Node last = req.ingress;
        for (int i = 0; i < order.length; i++) {
            ArrayList<NodeAssignment> part = createPath(last, order[i], backpointers);

            // Special case: first node of the path
            if (i == 0 && part.size() > 1) {
                path.add(part.get(0));
            }

            // All nodes except of first and last: (NodeAssignments without VNF instance)
            for (int j = 1; j < part.size() - 1; j++) {
                path.add(part.get(j));
            }

            // The instance is located on the last node of this sub-path:
            NodeAssignment oldAssig = part.get(part.size() - 1);
            path.add(new NodeAssignment(oldAssig.node, req.vnfSequence[i], oldAssig.prev));

            last = order[i];
        }

        // Special case: order.length == 0 --> add ingress (as the above special case has not been executed)
        if (order.length == 0) {
            path.add(new NodeAssignment(req.ingress, null, null));
        }

        // Add egress:
        if (!last.equals(req.egress)) {
            ArrayList<NodeAssignment> part = createPath(last, req.egress, backpointers);

            // All nodes except of the first: (NodeAssignments without VNF instance)
            for (int j = 1; j < part.size(); j++) {
                path.add(part.get(j));
            }
        }

        return new TrafficAssignment(req, path.toArray(new NodeAssignment[path.size()]), ng);
    }

    /**
     * Uses the result of a shortest path search to create a path
     * from {@code start} to {@code end}.
     *
     * @param start        Beginning node of this path.
     * @param end          End node of this path.
     * @param backpointers Result of a shortest path search.
     * @return Shortest path from {@code start} to {@code end}.
     */
    public static ArrayList<NodeAssignment> createPath(Node start, Node end, HashMap<Node, HashMap<Node, Node.Att>> backpointers) {
        ArrayList<NodeAssignment> path = new ArrayList<>();
        HashMap<Node, Node.Att> prevs = backpointers.get(start);
        Node current = end;

        Link prev = prevs.get(current).pi;
        path.add(new NodeAssignment(current, null, prev));

        while (prev != null) {
            current = prev.getOther(current);
            prev = prevs.get(current).pi;
            path.add(new NodeAssignment(current, null, prev));
        }

        // Mirror path:
        int n = path.size();
        for (int i = 0; i < n / 2; i++) {
            NodeAssignment temp = path.get(i);
            path.set(i, path.get(n - 1 - i));
            path.set(n - 1 - i, temp);
        }
        return path;
    }
}
