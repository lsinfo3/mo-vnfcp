package de.uniwue.VNFP.gui.graphUI.cell;

import de.uniwue.VNFP.algo.ParetoFrontier;
import de.uniwue.VNFP.model.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class RectangleCell extends Cell {
    private Integer dataId;
    private Rectangle view;
    private double intensity = 1;
    private Color color = Color.BLACK;


    public RectangleCell(String id, Node node, int dataId, ParetoFrontier front) {
        super(id, node, front);
        this.dataId = dataId;

        view = new Rectangle(20, 20);

        view.setStroke(color);
        view.setFill(color);
        view.setOpacity(getIntensity());
        setView(view);

    }

    @Override
    public CellType getType() {
        return CellType.RECTANGLE;
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
        this.view.setStroke(color);
    }

    @Override
    public double getIntensity() {
        return intensity;
    }

    @Override
    public void setIntensity(double intensity) {
        this.intensity = intensity;
        view.setOpacity(intensity);
    }

    @Override
    public Color getColor() {
        return color;
    }

    @Override
    public void setColor(Color color) {
        this.color = color;
    }

}
