package de.lexej.VNFP.model.solution;

import de.lexej.VNFP.model.Link;
import de.lexej.VNFP.model.Node;
import de.lexej.VNFP.model.TrafficRequest;
import de.lexej.VNFP.model.VNF;

import java.util.Objects;

/**
 * Objects of this class are part of a path assignment ({@link TrafficAssignment}).
 * They represent a tuple {@code (Node, VNF)}, while the VNF is optional
 * (possibly {@code null} if the node is only part of the route and no NF is applied there)..
 *
 * @author alex
 */
public class NodeAssignment {
    /**
     * The corresponding node.
     */
    public final Node node;
    /**
     * The applied VNF on this node. May be {@code null}, if no NF is applied here.
     */
    public final VNF vnf;
    /**
     * The TrafficRequest that this NodeRequest is part of.
     * This reverse pointer is required to calculate bandwidth capacities.
     * <b>This variable is set in the constructor of the {@link TrafficAssignment} class.</b>
     */
    public TrafficRequest traffReq;
    /**
     * The TrafficAssignment that this NodeRequest is part of.
     * This reverse pointer is required to calculate bandwidth capacities.
     * <b>This variable is set in the constructor of the {@link TrafficAssignment} class.</b>
     * This value is not used in <tt>equals()</tt> and <tt>hashCode()</tt> methods.
     */
    public TrafficAssignment traffAss;
    /**
     * The {@link Link} instance that points towards the previously used node in the path.
     * It may be {@code null} in case of the ingress, or if the previous node was the same.
     */
    public final Link prev;

    /**
     * Creates a new instance.
     *
     * @param node The corresponding node of the path.
     * @param vnf  The applied VNF on this node. May be {@code null}, if no NF is applied here.
     * @param prev The {@link Link} instance that points towards the previously used node in the path.
     *             It may be {@code null} in case of the ingress, or if the previous node was the same.
     */
    public NodeAssignment(Node node, VNF vnf, Link prev) {
        this.node = Objects.requireNonNull(node);
        this.vnf = vnf;
        this.prev = prev;
    }

    @Override
    public String toString() {
        return "NodeAssignment{" +
                "node=" + node.name +
                ", vnf=" + (vnf != null ? vnf.name : null) +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NodeAssignment that = (NodeAssignment) o;

        if (!node.equals(that.node)) return false;
        if (vnf != null ? !vnf.equals(that.vnf) : that.vnf != null) return false;
        if (traffReq != null ? !traffReq.equals(that.traffReq) : that.traffReq != null) return false;
        return prev != null ? prev.equals(that.prev) : that.prev == null;
    }

    @Override
    public int hashCode() {
        int result = node.hashCode();
        result = 31 * result + (vnf != null ? vnf.hashCode() : 0);
        result = 31 * result + (traffReq != null ? traffReq.hashCode() : 0);
        result = 31 * result + (prev != null ? prev.hashCode() : 0);
        return result;
    }
}
