import de.uniwue.VNFP.model.NetworkGraph;
import de.uniwue.VNFP.model.TrafficRequest;
import de.uniwue.VNFP.model.VNF;
import de.uniwue.VNFP.model.VnfLib;
import de.uniwue.VNFP.model.factory.TopologyFileReader;
import de.uniwue.VNFP.model.factory.TrafficRequestsReader;
import de.uniwue.VNFP.model.factory.VnfLibReader;

import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SndLibConverter {
	private final static Pattern pNodes = Pattern.compile(" *([^ ]+) \\( (-?\\d+\\.\\d+) (-?\\d+\\.\\d+) \\)");
	private final static Pattern pLinks = Pattern.compile(" *[^ ]+ \\( ([^ ]+) ([^ ]+) \\) \\d+\\.\\d+ \\d+\\.\\d+ \\d+\\.\\d+ \\d+\\.\\d+ \\( (\\d+\\.\\d+) (\\d+\\.\\d+) \\)");
	private final static Pattern pDemands = Pattern.compile(" *[^ ]+ \\( ([^ ]+) ([^ ]+) \\) \\d+ (\\d+\\.\\d+) [^ ]+");

	public static void main(String[] args) throws Exception {
		Locale.setDefault(Locale.US);

//		String baseFolder = "res/problem_instances/geant2/";
//		convertFile(1e0, baseFolder+"geant.txt", baseFolder+"topology", baseFolder+"requests-nodelays");

//		String baseFolder = "res/problem_instances/internet2/";
//		String topology = baseFolder + "topology";
//		String vnfLib = baseFolder + "vnfLib";
//		String requests = baseFolder + "requests";
//		String fileOut = baseFolder + "relativeDelays";
//		printRelativeLatencies(topology, vnfLib, requests, fileOut);

		String baseFolder = "res/problem_instances/internet2/";
		String topology = baseFolder + "topology";
		String vnfLib = baseFolder + "vnfLib";
		String requests_nodelays = baseFolder + "requests-nodelays";
		String requests_out = baseFolder + "requests";
		artificialRequests(topology, vnfLib, requests_nodelays, requests_out);
	}

	public static void artificialRequests(String topology, String vnfLib, String requests_nodelays, String requests_out) throws Exception {
		VnfLib lib = VnfLibReader.readFromFile(vnfLib);
		NetworkGraph topo = TopologyFileReader.readFromFile(topology, lib);
		TrafficRequest[] reqs_nodelays = TrafficRequestsReader.readFromFile(requests_nodelays, topo, lib);

		BufferedWriter wReq = Files.newBufferedWriter(Paths.get(requests_out));
		wReq.write("# Ingress-ID, Egress-ID, Min-Bandwidth, Max-Delay, VNF, VNF, VNF, ...\n");
		Random r = new Random();
		VNF[] vnfs = lib.getAllVnfs().toArray(new VNF[0]);

		for (TrafficRequest req : reqs_nodelays) {
			int numVnfs = r.nextInt(vnfs.length) + 1;
			Collections.shuffle(Arrays.asList(vnfs), r);
			VNF[] reqVnfs = Arrays.copyOf(vnfs, numVnfs);
			String vnfString = Arrays.stream(reqVnfs).map(v -> v.name).collect(Collectors.joining(","));

			double shortest = req.getShortestDelay(topo.getDijkstraBackpointers());
			shortest += Arrays.stream(reqVnfs).mapToDouble(v -> v.delay).sum();
			double ratio = Math.pow(10, r.nextDouble() * 0.75 + 0.25); // should be a number between 1.7 and 10
			double expected = shortest * ratio;

			wReq.write(String.format("%s,%s,%d,%d,%s\n",
					req.ingress.name,
					req.egress.name,
					Math.round(req.bandwidthDemand * 1000),
					Math.round(expected),
					vnfString));
		}
		wReq.close();
	}

	public static void printRelativeLatencies(String topology, String vnfLib, String requests, String fileOut) throws Exception {
		VnfLib lib = VnfLibReader.readFromFile(vnfLib);
		NetworkGraph topo = TopologyFileReader.readFromFile(topology, lib);
		TrafficRequest[] reqs = TrafficRequestsReader.readFromFile(requests, topo, lib);

		BufferedWriter wLat = Files.newBufferedWriter(Paths.get(fileOut));
		wLat.write("required;shortest;ratio\n");

		for (TrafficRequest req : reqs) {
			double required = req.expectedDelay;
			double shortest = req.getShortestDelay(topo.getDijkstraBackpointers());
			double ratio = required / shortest;

			wLat.write(required + ";" + shortest + ";" + ratio + "\n");
		}
		wLat.close();
	}

	public static void convertFile(double bwMultiplier, String sndLibInput, String topologyOut, String requestsOut) throws IOException {
		LineNumberReader lnr = new LineNumberReader(new FileReader(sndLibInput));
		BufferedWriter wTopo = Files.newBufferedWriter(Paths.get(topologyOut));
		BufferedWriter wReqs = Files.newBufferedWriter(Paths.get(requestsOut));

		ArrayList<Node> nodes = new ArrayList<>();
		ArrayList<Link> links= new ArrayList<>();
		ArrayList<Demand> demands = new ArrayList<>();

		String l;
		int mode = 0;
		while ((l = lnr.readLine()) != null) {
			if (l.trim().isEmpty() || l.trim().startsWith("#")) {
				continue;
			}

			if (l.equals(")")) {
				mode = 0;
				continue;
			}
			if (l.equals("NODES (")) {
				mode = 1;
				continue;
			}
			if (l.equals("LINKS (")) {
				mode = 2;
				continue;
			}
			if (l.equals("DEMANDS (")) {
				mode = 3;
				continue;
			}

			// Nodes
			if (mode == 1) {
				Matcher m = pNodes.matcher(l);
				if (!m.matches()) {
					System.out.println("Error reading node line '" + l + "'");
					continue;
				}

				nodes.add(new Node(m.group(1), Double.parseDouble(m.group(2)), Double.parseDouble(m.group(3))));
				continue;
			}

			// Links
			if (mode == 2) {
				Matcher m = pLinks.matcher(l);
				if (!m.matches()) {
					System.out.println("Error reading link line '" + l + "'");
					continue;
				}

				links.add(new Link(m.group(1), m.group(2), Double.parseDouble(m.group(3)) * bwMultiplier, Double.parseDouble(m.group(4))));
				continue;
			}

			// Demands
			if (mode == 3) {
				Matcher m = pDemands.matcher(l);
				if (!m.matches()) {
					System.out.println("Error reading demand line '" + l + "'");
					continue;
				}

				demands.add(new Demand(m.group(1), m.group(2), Double.parseDouble(m.group(3)) * bwMultiplier));
				continue;
			}
		}
		System.out.println("Read " + nodes.size() + " nodes, " + links.size() + " links, " + demands.size() + " demands.");

		// Write topology
		wTopo.write("# Number-of-nodes, Number-of-links\n");
		wTopo.write(nodes.size() + "," + links.size() + "\n\n");

		wTopo.write("# Node-ID, CPU\n");
		for (Node n : nodes) {
			wTopo.write(n.toString() + "\n");
		}
		wTopo.write("\n");

		wTopo.write("# Node-ID, Node-ID, Bandwidth, Delay\n");
		for (Link link : links) {
			wTopo.write(link.toString() + "\n");
		}

		wTopo.flush();
		wTopo.close();
		System.out.println("Written topology '" + topologyOut + "'.");

		// Write demands
		wReqs.write("# Ingress-ID, Egress-ID, Min-Bandwidth, Max-Delay, VNF, VNF, VNF, ...\n");
		for (Demand d : demands) {
			wReqs.write(d.toString() + "\n");
		}

		wReqs.flush();
		wReqs.close();
		System.out.println("Written requests '" + requestsOut + "'.");
	}

	private static class Node {
		public final String name;
		public final double x;
		public final double y;

		public Node(String name, double x, double y) {
			this.name = name;
			this.x = x;
			this.y = y;
		}

		@Override
		public String toString() {
			return name + "(" + x + "," + y + "),0";
		}
	}

	private static class Link {
		public final String from;
		public final String to;
		public final double cap;
		public final double cost;

		public Link(String from, String to, double cap, double cost) {
			this.from = from;
			this.to = to;
			this.cap = cap;
			this.cost = cost;
		}

		@Override
		public String toString() {
			return from + "," + to + "," + cap + "," + cost;
		}
	}

	private static class Demand {
		public final String from;
		public final String to;
		public final double dataRate;

		public Demand(String from, String to, double dataRate) {
			this.from = from;
			this.to = to;
			this.dataRate = dataRate;
		}

		@Override
		public String toString() {
			return from + "," + to + "," + dataRate + ",-1,";
		}
	}
}
