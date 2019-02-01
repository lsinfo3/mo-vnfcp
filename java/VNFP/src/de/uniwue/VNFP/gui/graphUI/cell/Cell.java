package de.uniwue.VNFP.gui.graphUI.cell;

import java.util.ArrayList;
import java.util.List;


import de.uniwue.VNFP.algo.ParetoFrontier;
import de.uniwue.VNFP.model.Node;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

public class Cell extends Pane {

    private String cellId;

    Integer dataId;

    private List<Cell> children = new ArrayList<>();
    private List<Cell> parents = new ArrayList<>();

    private Node node;
    private ParetoFrontier front;
    private double intensity = 1;
    private Color color = Color.BLACK;

    public Cell(String cellId, Node node, ParetoFrontier front) {
        this.cellId = cellId;
        this.node = node;
        this.front=front;
    }

    public Node getNode() {
        return node;
    }
    public void setNode(Node node){
        this.node=node;
    }

    public void addCellChild(Cell cell) {
        children.add(cell);
    }

    public void addCellParent(Cell cell) {
        parents.add(cell);
    }

    public List<Cell> getCellParents() {
        return parents;
    }

    public void removeCellChild(Cell cell) {
        children.remove(cell);
    }

    void setView(javafx.scene.Node view) {

        getChildren().add(view);

    }
    void addView(javafx.scene.Node view){
        getChildren().add(view);
    }

    public String getCellId() {
        return cellId;
    }

    public double getCenterX() {
        return 0;
    }

    public double getCenterY() {
        return 0;
    }

    public CellType getType() {
        return null;
    }

    public void setDataId(int id) {
        this.dataId = id;
    }

    public Integer getDataId() {
        return this.dataId;
    }

    public void setFillColor(Color color) {}
    public void setStroke(Color color,double i){}

    public ParetoFrontier getFront() {
        return front;
    }

    public double getIntensity() {
        return intensity;
    }

    public void setIntensity(double intensity) {
        this.intensity = intensity;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }
    public void setIndicator(){}
    public boolean hasResources(){return false;}
}