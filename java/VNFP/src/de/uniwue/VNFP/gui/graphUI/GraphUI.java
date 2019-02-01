package de.uniwue.VNFP.gui.graphUI;


import de.uniwue.VNFP.gui.core.DataManager;
import de.uniwue.VNFP.gui.graphUI.cell.Cell;
import de.uniwue.VNFP.gui.graphUI.cell.CellType;
import de.uniwue.VNFP.gui.graphUI.graph.Graph;
import de.uniwue.VNFP.gui.graphUI.graph.Model;
import de.uniwue.VNFP.gui.graphUI.layout.CircleLayout;
import de.uniwue.VNFP.gui.graphUI.layout.Layout;
import de.uniwue.VNFP.gui.graphUI.layout.PlottedLayout;
import de.uniwue.VNFP.model.Link;
import javafx.scene.control.ScrollPane;

public class GraphUI {
    private DataManager dataManager;

    public GraphUI(DataManager dataManager) {
        this.dataManager = dataManager;
    }
    private Graph graph = new Graph(dataManager);


    public ScrollPane initialize() {
        graph = new Graph(dataManager);
        addGraphComponents();
        if(dataManager.getNg().hasGeoCoordinates) {
            Layout layout = new PlottedLayout(graph);
            layout.execute();
        }else{
            Layout layout= new CircleLayout(graph);
            layout.execute();
        }
        dataManager.setCells(graph.getModel().getAllCells());
        return graph.getScrollPane();
    }

    private void addGraphComponents() {

        Model model = graph.getModel();

        graph.beginUpdate();
        for (Cell cell : this.dataManager.getCells()
                ) {
            model.addCell(cell.getCellId(), cell.getNode(), CellType.CIRCLE, cell.getDataId(), cell.getFront(), cell.getIntensity());
        }
        for (Cell cell : this.dataManager.getCells()
                ) {
            for (Link link : cell.getNode().getNeighbors()
                    ) {
                model.addEdge(link.node1.name, link.node2.name,String.format("%.2f%%",1-(cell.getFront().get(0).linkMap.get(link).remainingBandwidth()/link.bandwidth)));
            }
        }
        graph.endUpdate();

    }

    public Graph getGraph() {
        return graph;
    }
}