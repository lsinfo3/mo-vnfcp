package de.uniwue.VNFP.gui.graphUI.graph;
/*
  Created by Simon Raffeck on 06.11.17.
 */

import de.uniwue.VNFP.algo.ParetoFrontier;
import de.uniwue.VNFP.gui.graphUI.cell.*;
import de.uniwue.VNFP.model.Node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Model {

    private Cell graphParent;

    private List<Cell> allCells;
    private List<Cell> addedCells;
    private List<Cell> removedCells;

    private List<Edge> allEdges;
    private List<Edge> addedEdges;
    private List<Edge> removedEdges;

    private Map<String, Cell> cellMap; // <id,graphUI.cell>

    Model() {

        graphParent = new Cell("_ROOT_", new Node("root", new double[]{0}), null);

        // clear model, create lists
        clear();
    }

    private void clear() {

        allCells = new ArrayList<>();
        addedCells = new ArrayList<>();
        removedCells = new ArrayList<>();

        allEdges = new ArrayList<>();
        addedEdges = new ArrayList<>();
        removedEdges = new ArrayList<>();

        cellMap = new HashMap<>(); // <id,graphUI.cell>

    }
    List<Cell> getAddedCells() {
        return addedCells;
    }

    List<Cell> getRemovedCells() {
        return removedCells;
    }

    public List<Cell> getAllCells() {
        return allCells;
    }

    List<Edge> getAddedEdges() {
        return addedEdges;
    }

    List<Edge> getRemovedEdges() {
        return removedEdges;
    }

    public List<Edge> getAllEdges() {
        return allEdges;
    }

    public void addCell(String id, Node node, CellType type, int dataId, ParetoFrontier front, double opacity) {

        switch (type) {
            case CIRCLE:
                CircleCell circleCell = new CircleCell(id, node, dataId, front);
                circleCell.setIntensity(opacity);
                addCell(circleCell);
                break;

            case RECTANGLE:
                RectangleCell rectangleCell = new RectangleCell(id, node, dataId, front);
                rectangleCell.setIntensity(opacity);
                addCell(rectangleCell);
                break;

            case TRIANGLE:
                TriangleCell triangleCell = new TriangleCell(id, node, dataId, front);
                triangleCell.setIntensity(opacity);
                addCell(triangleCell);
                break;

            default:
                throw new UnsupportedOperationException("Unsupported type: " + type);
        }
    }

    private void addCell(Cell cell) {

        addedCells.add(cell);

        cellMap.put(cell.getCellId(), cell);

    }

    public void addEdge(String sourceId, String targetId, String remaining) {

        Cell sourceCell = cellMap.get(sourceId);
        Cell targetCell = cellMap.get(targetId);

        Edge edge = new Edge(sourceCell, targetCell,remaining);

        addedEdges.add(edge);

    }

    void attachOrphansToGraphParent(List<Cell> cellList) {

        for (Cell cell : cellList) {
            if (cell.getCellParents().size() == 0) {
                graphParent.addCellChild(cell);
            }
        }

    }

    void disconnectFromGraphParent(List<Cell> cellList) {

        for (Cell cell : cellList) {
            graphParent.removeCellChild(cell);
        }
    }

    void merge() {

        // cells
        allCells.addAll(addedCells);
        allCells.removeAll(removedCells);

        addedCells.clear();
        removedCells.clear();

        // edges
        allEdges.addAll(addedEdges);
        allEdges.removeAll(removedEdges);

        addedEdges.clear();
        removedEdges.clear();

    }
}
