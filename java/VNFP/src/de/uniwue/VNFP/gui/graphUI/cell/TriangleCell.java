package de.uniwue.VNFP.gui.graphUI.cell;


import de.uniwue.VNFP.algo.ParetoFrontier;
import de.uniwue.VNFP.model.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;


public class TriangleCell extends Cell {

    private double intensity = 1;
    private Color color = Color.BLACK;

    public TriangleCell(String id, Node node, int dataId, ParetoFrontier front) {
        super(id, node, front);
        this.dataId = dataId;

        double width = 50;
        double height = 50;

        Polygon view = new Polygon(width / 2, 0, width, height, 0, height);

        view.setStroke(Color.RED);
        view.setFill(Color.RED);
        view.setOpacity(getIntensity());

        setView(view);

    }

    @Override
    public CellType getType() {
        return CellType.TRIANGLE;
    }

    @Override
    public double getIntensity() {
        return intensity;
    }

    @Override
    public void setIntensity(double intensity) {
        this.intensity = intensity;
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