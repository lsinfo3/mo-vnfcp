package de.uniwue.VNFP.gui.graphUI;
/*
  Created by Simon Raffeck on 06.11.17.
 */

import de.uniwue.VNFP.gui.graphUI.DetailsUI.Details;
import de.uniwue.VNFP.gui.core.DataManager;
import de.uniwue.VNFP.gui.graphUI.cell.Cell;
import de.uniwue.VNFP.gui.graphUI.graph.Graph;
import de.uniwue.VNFP.model.solution.VnfInstances;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

import java.io.IOException;


public class MouseGestures {

    private final DragContext dragContext = new DragContext();

    private Graph graph;
    private DataManager dataManager;
    private Details details;
    private Cell lastSelected;

    public MouseGestures(Graph graph) {
        this.graph = graph;
        this.dataManager = graph.getDataManager();
    }

    public void makeDraggable(final Node node) {


        node.setOnMousePressed(onMousePressedEventHandler);
        node.setOnMouseDragged(onMouseDraggedEventHandler);
        node.setOnMouseReleased(onMouseReleasedEventHandler);
        node.setOnMouseClicked(onMouseClickedEventHandler);

    }

    private EventHandler<MouseEvent> onMousePressedEventHandler = new EventHandler<>() {

        @Override
        public void handle(MouseEvent event) {

            Node node = (Node) event.getSource();

            double scale = graph.getScale();

            dragContext.x = node.getBoundsInParent().getMinX() * scale - event.getScreenX();
            dragContext.y = node.getBoundsInParent().getMinY() * scale - event.getScreenY();

        }
    };

    private EventHandler<MouseEvent> onMouseClickedEventHandler = new EventHandler<>() {
        @Override
        public void handle(MouseEvent event) {
            if (event.getButton().equals(MouseButton.PRIMARY)) {
                        details = null;
                        try {
                            details = new Details(dataManager);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        for (Cell cell:dataManager.getCells()
                             ) {
                            cell.setStroke(cell.getColor(),0);
                        }
                        Cell cell = (Cell) event.getSource();
                        lastSelected=cell;
                        cell.setStroke(Color.RED,4);
                        int solution = dataManager.getTableUI().getTable().getSelectionModel().getFocusedIndex();
                        for (VnfInstances vnf : cell.getFront().get(solution).nodeMap.get(cell.getNode()).getVnfInstances().values()
                                ) {
                            for (int i =0;i<vnf.loads.length;i++){
                                if (!dataManager.getVnfTypes().contains(vnf.type.name)){dataManager.getVnfTypes().add(vnf.type.name);}
                                details.addDataToSeries(vnf.type.name,vnf.loads[i],""+i);
                            }

                        }
                    for (int i = 0; i < dataManager.getO().TOTAL_USED_RESOURCES.length; i++) {
                        double percantage = (cell.getNode().resources[i] - cell.getFront().get(solution).nodeMap.get(cell.getNode()).remainingResources()[i]) / cell.getNode().resources[i];
                        String label =cell.getNode().resources[i] - cell.getFront().get(solution).nodeMap.get(cell.getNode()).remainingResources()[i]
                                + " | " + cell.getNode().resources[i];
                        label =dataManager.getO().TOTAL_USED_RESOURCES[i].toString()+" : "+label;
                        details.setBarWidth(i,percantage * 100, label, 100);
                    }
                        details.populateBarChart();
                        dataManager.getController().setBottom(details.getPane());
                        dataManager.getController().getBottom().setRight(new Pane());
                        dataManager.setDetailsOpen(true);
                }
            }

    };
    public void fire(){
        try {
            details = new Details(dataManager);
        } catch (IOException e) {
            e.printStackTrace();
        }
        int solution = dataManager.getTableUI().getTable().getSelectionModel().getFocusedIndex();
        for (VnfInstances vnf : lastSelected.getFront().get(solution).nodeMap.get(lastSelected.getNode()).getVnfInstances().values()
                ) {
            for (int i =0;i<vnf.loads.length;i++){
                if (!dataManager.getVnfTypes().contains(vnf.type.name)){dataManager.getVnfTypes().add(vnf.type.name);}
                details.addDataToSeries(vnf.type.name,vnf.loads[i],""+i);
            }

        }
        for (int i = 0; i < dataManager.getO().TOTAL_USED_RESOURCES.length; i++) {
            double percantage = (lastSelected.getNode().resources[i] - lastSelected.getFront().get(solution).nodeMap.get(lastSelected.getNode()).remainingResources()[i]) / lastSelected.getNode().resources[i];
            String label =lastSelected.getNode().resources[i] - lastSelected.getFront().get(solution).nodeMap.get(lastSelected.getNode()).remainingResources()[i]
                    + " | " + lastSelected.getNode().resources[i];
            label =dataManager.getO().TOTAL_USED_RESOURCES[i].toString()+" : "+label;
            details.setBarWidth(i,percantage * 100, label, 100);
        }
        details.populateBarChart();
        dataManager.getController().setBottom(details.getPane());
        dataManager.getController().getBottom().setRight(new Pane());
        dataManager.setDetailsOpen(true);
    }

    private EventHandler<MouseEvent> onMouseDraggedEventHandler = new EventHandler<>() {

        @Override
        public void handle(MouseEvent event) {

            Node node = (Node) event.getSource();

            double offsetX = event.getScreenX() + dragContext.x;
            double offsetY = event.getScreenY() + dragContext.y;

            // adjust the offset in case we are zoomed
            double scale = graph.getScale();

            offsetX /= scale;
            offsetY /= scale;

            node.relocate(offsetX, offsetY);

        }
    };

    private EventHandler<MouseEvent> onMouseReleasedEventHandler = event -> {

    };

    class DragContext {

        double x;
        double y;

    }

}
