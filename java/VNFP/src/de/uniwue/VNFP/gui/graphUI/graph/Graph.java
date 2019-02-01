package de.uniwue.VNFP.gui.graphUI.graph;

import de.uniwue.VNFP.gui.core.DataManager;
import de.uniwue.VNFP.gui.graphUI.MouseGestures;
import de.uniwue.VNFP.gui.graphUI.ZoomableScrollPane;
import de.uniwue.VNFP.gui.graphUI.cell.Cell;
import de.uniwue.VNFP.gui.graphUI.cell.CellLayer;
import javafx.scene.Group;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Pane;

public class Graph {

    private Model model;

    private ZoomableScrollPane scrollPane;

    private MouseGestures mouseGestures;

    /**
     * the pane wrapper is necessary or else the scrollpane would always align
     * the top-most and left-most child to the top and left eg when you drag the
     * top child down, the entire scrollpane would move down
     */
    private CellLayer cellLayer;
    private DataManager dataManager;

    public DataManager getDataManager() {
        return dataManager;
    }

    public Graph(DataManager dataManager) {
        this.dataManager = dataManager;

        this.model = new Model();

        Group canvas = new Group();
        cellLayer = new CellLayer();

        canvas.getChildren().add(cellLayer);

        mouseGestures = new MouseGestures(this);

        scrollPane = new ZoomableScrollPane(canvas);

        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);

    }

    public MouseGestures getMouseGestures() {
        return mouseGestures;
    }

    public ScrollPane getScrollPane() {
        return this.scrollPane;
    }

    private Pane getCellLayer() {
        return this.cellLayer;
    }

    public Model getModel() {
        return model;
    }

    public void beginUpdate() {
    }

    public void endUpdate() {

        // add components to graphUI.graph pane
        getCellLayer().getChildren().addAll(model.getAddedEdges());
        getCellLayer().getChildren().addAll(model.getAddedCells());

        // remove components from graphUI.graph pane
        getCellLayer().getChildren().removeAll(model.getRemovedCells());
        getCellLayer().getChildren().removeAll(model.getRemovedEdges());

        // enable dragging of cells
        for (Cell cell : model.getAddedCells()) {
            mouseGestures.makeDraggable(cell);
        }

        // every graphUI.cell must have a parent, if it doesn't, then the graphParent is
        // the parent
        getModel().attachOrphansToGraphParent(model.getAddedCells());

        // remove reference to graphParent
        getModel().disconnectFromGraphParent(model.getRemovedCells());

        // merge added & removed cells with all cells
        getModel().merge();

    }

    public double getScale() {
        return this.scrollPane.getScaleValue();
    }
}
