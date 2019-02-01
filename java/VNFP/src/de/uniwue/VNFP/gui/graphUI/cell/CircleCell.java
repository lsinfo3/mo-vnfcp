package de.uniwue.VNFP.gui.graphUI.cell;

import de.uniwue.VNFP.algo.ParetoFrontier;
import de.uniwue.VNFP.model.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

/**
 * Created by Simon Raffeck on 06.11.17.
 */
public class CircleCell extends Cell {
    private Circle circle;
    private Circle indicator;
    private Integer dataId;
    private double intensity = 1;
    private Color color = Color.BLACK;

    public CircleCell(String id, Node node, int dataId, ParetoFrontier front) {
        super(id, node, front);
        this.getStyleClass().add("circleCell");
        this.dataId = dataId;
        circle = new Circle(20);
        indicator = new Circle(10);
        circle.setFill(color);
        setView(circle);
        addView(indicator);
    }

    @Override
    public double getCenterX() {
        return this.circle.getCenterX();
    }

    @Override
    public double getCenterY() {
        return this.circle.getCenterY();
    }

    @Override
    public CellType getType() {
        return CellType.CIRCLE;
    }

    @Override
    public void setDataId(int id) {
        this.dataId = id;
    }

    @Override
    public Integer getDataId() {
        return this.dataId;
    }

    @Override
    public void setFillColor(Color color) {
        this.circle.setFill(color);
        this.indicator.setFill(color);
    }

    @Override
    public double getIntensity() {
        return intensity;
    }

    @Override
    public void setIntensity(double intensity) {
        this.intensity = intensity;
        circle.setOpacity(intensity);
    }

    @Override
    public Color getColor() {
        return color;
    }

    @Override
    public void setColor(Color color) {
        this.color = color;
    }

    @Override
    public void setStroke(Color color,double i) {
        this.circle.setStroke(color);
        this.circle.setStrokeWidth(i);
    }
    @Override
    public void setIndicator(){
        this.indicator.setFill(Color.ORANGE);
    }
    @Override
    public boolean hasResources(){
        double resources =0;
        for (int i = 0; i <getNode().resources.length ; i++) {
            resources=resources+getNode().resources[i];
        }
        return !(resources == 0);
    }
}
