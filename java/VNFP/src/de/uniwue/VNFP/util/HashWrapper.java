package de.uniwue.VNFP.util;

import de.uniwue.VNFP.model.Node;
import de.uniwue.VNFP.model.TrafficRequest;
import de.uniwue.VNFP.model.VNF;

import java.util.Arrays;
import java.util.Objects;

/**
 * This class provides a different set of equals and hashCode functions for the {@link TrafficRequest}s.
 */
public class HashWrapper {
	private final TrafficRequest req;

	public HashWrapper(TrafficRequest req) {
		this.req = Objects.requireNonNull(req);
	}

	public HashWrapper(Node ingress, Node egress, VNF[] vnfs) {
		Objects.requireNonNull(ingress);
		Objects.requireNonNull(egress);
		Objects.requireNonNull(vnfs);
		this.req = new TrafficRequest(0, ingress, egress, 123, 123, vnfs);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		HashWrapper that = (HashWrapper) o;

		if (!req.ingress.equals(that.req.ingress)) return false;
		if (!req.egress.equals(that.req.egress)) return false;
		return Arrays.equals(req.vnfSequence, that.req.vnfSequence);
	}

	@Override
	public int hashCode() {
		int result = req.ingress.hashCode();
		result = 31 * result + req.egress.hashCode();
		result = 31 * result + Arrays.hashCode(req.vnfSequence);
		return result;
	}
}
