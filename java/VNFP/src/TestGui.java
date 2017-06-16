import de.lexej.VNFP.algo.ParetoFrontier;
import de.lexej.VNFP.gui.GuiApp;
import de.lexej.VNFP.model.NetworkGraph;
import de.lexej.VNFP.model.TrafficRequest;
import de.lexej.VNFP.model.VnfLib;
import de.lexej.VNFP.model.factory.FlowPlacementReader;
import de.lexej.VNFP.model.factory.TopologyFileReader;
import de.lexej.VNFP.model.factory.TrafficRequestsReader;
import de.lexej.VNFP.model.factory.VnfLibReader;
import de.lexej.VNFP.model.solution.Solution;

import java.util.Locale;

/**
 * Epic class for epic things.
 * (This generic message was probably left here because
 * this file's name is self-explanatory.)
 *
 * @author alex
 */
public class TestGui {
    public static void main(String[] args) throws Exception {
        Locale.setDefault(Locale.US);

        String input_topology = "res/problem_instances/BCAB15/topology-160";
        String input_requests = "res/problem_instances/BCAB15/requests-all";
        String input_vnfs = "res/problem_instances/BCAB15/vnfLib-orig";
        String placementFlows = "/home/alex/w/MA/eval/out/1480880061098/BCAB15/8_500_0.85_40_50_1_LEASTCPU_0_0_true/2/placementFlows";

        NetworkGraph ng = TopologyFileReader.readFromFile(input_topology);
        VnfLib vnfLib = VnfLibReader.readFromFile(input_vnfs);
        TrafficRequest[] reqs = TrafficRequestsReader.readFromFile(input_requests, ng, vnfLib);

        ParetoFrontier front = FlowPlacementReader.readFromCsv(ng, reqs, placementFlows);

        for (Solution s : front) {
            System.out.println(s.toString());
        }

        GuiApp.frontier = front;
        GuiApp.launch(GuiApp.class);
    }
}
