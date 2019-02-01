package de.uniwue.VNFP.gui.graphUI.layout;

import de.uniwue.VNFP.gui.graphUI.cell.Cell;
import de.uniwue.VNFP.gui.graphUI.graph.Graph;
import de.uniwue.VNFP.util.Point;

import java.util.List;

/**
 * Created by Simon Raffeck on 02.03.18.
 */
public class CircleLayout extends Layout{
    private Graph graph;
    private Point center = new Point(300,150);
    private double angle = 0;
    public CircleLayout(Graph graph){
        this.graph=graph;
        angle=360/graph.getModel().getAllCells().size();
    }



    public void execute() {
        List<Cell> cells = graph.getModel().getAllCells();
        int i=0;
        for (Cell cell : cells) {
            double d = 100;
            double x = center.x + (Math.cos(Math.toRadians(i * angle)) * d);
            double y = center.y + (Math.sin(Math.toRadians(i * angle)) * d);
            cell.relocate(x, y);
            i++;
        }
    }
}
