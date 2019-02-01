package de.uniwue.VNFP.gui.graphUI.layout;

import de.uniwue.VNFP.gui.graphUI.cell.Cell;
import de.uniwue.VNFP.gui.graphUI.graph.Graph;

import java.util.List;


/**
 * Created by Simon Raffeck on 08.11.17.
 */
public class PlottedLayout extends Layout {
    private Graph graph;




    public PlottedLayout(Graph graph) {
        this.graph = graph;

    }
    public void execute() {
        List<Cell> cells = graph.getModel().getAllCells();
        double diffX = getDiffX();
        double diffY = getDiffY();
        double maxX = getMaxX()+diffX;
        double maxY = getMaxY()+diffY;
        for (Cell cell : cells) {

            double x = ((cell.getNode().geo.x+diffX)/maxX)*600;
            double y = ((cell.getNode().geo.y+diffY)/maxY)*300;
            cell.relocate(x, y);

        }
    }
    private double getDiffX(){
        double min =graph.getModel().getAllCells().get(0).getNode().geo.x;
        List<Cell> cells = graph.getModel().getAllCells();
        for (Cell cell : cells) {
            if (cell.getNode().geo.x<min){
                min=cell.getNode().geo.x;
            }
        }
        return min*-1;
    }
    private double getDiffY(){
        double min =graph.getModel().getAllCells().get(0).getNode().geo.y;
        List<Cell> cells = graph.getModel().getAllCells();
        for (Cell cell : cells) {
            if (cell.getNode().geo.y<min){
                min=cell.getNode().geo.y;
            }
        }
        return min*-1;
    }
    private double getMaxX(){
        double max =graph.getModel().getAllCells().get(0).getNode().geo.x;
        List<Cell> cells = graph.getModel().getAllCells();
        for (Cell cell : cells) {
            if (cell.getNode().geo.x>max){
                max=cell.getNode().geo.x;
            }
        }
        return max;
    }
    private double getMaxY(){
        double max =graph.getModel().getAllCells().get(0).getNode().geo.y;
        List<Cell> cells = graph.getModel().getAllCells();
        for (Cell cell : cells) {
            if (cell.getNode().geo.y>max){
                max=cell.getNode().geo.y;
            }
        }
        return max;
    }

}
