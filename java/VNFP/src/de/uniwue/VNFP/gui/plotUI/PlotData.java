package de.uniwue.VNFP.gui.plotUI;


import de.uniwue.VNFP.gui.core.DataManager;
import de.uniwue.VNFP.gui.graphUI.cell.Cell;
import de.uniwue.VNFP.gui.tableUI.TableData;
import de.uniwue.VNFP.model.Node;
import de.uniwue.VNFP.model.solution.Solution;
import javafx.event.EventHandler;
import javafx.scene.chart.XYChart;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;


/**
 * Created by Simon Raffeck on 07.11.17.
 */
public class PlotData {
    private Integer dataId;
    private XYChart.Data xyData;
    private Shape shape;
    private DataManager dataManager;
    private double x;
    private double y;
    private Solution solution;

    private boolean selected;

    private int indexX;
    private int indexY;

    private Color color;

    public PlotData(Solution s, DataManager dataManager, Color color) {
        selected=false;
        indexX = dataManager.getIndexX();
        indexY = dataManager.getIndexY();
        this.x = s.vals[indexX];
        this.y = s.vals[indexY];
        this.solution = s;
        this.color = color;
        this.dataManager = dataManager;
        this.xyData = new XYChart.Data(x, y);
        toHex(color);
        if (this.solution.isFeasible()) {
            this.shape = new Circle(5, color);
            updateColor(Color.BLACK, 1);
        } else {
            this.shape = new Rectangle(7, 7, color);
            updateColor(Color.BLACK, 1);
        }
        xyData.setNode(shape);
        xyData.getNode().setOnMouseClicked(onMouseClickedEventHandler);


    }
    public void update(Color color){
        indexX = dataManager.getIndexX();
        indexY = dataManager.getIndexY();
        this.x = this.getSolution().vals[indexX];
        this.y = this.getSolution().vals[indexY];
        this.xyData = new XYChart.Data(x, y);
        setColor(color);
        xyData.setNode(shape);
        xyData.getNode().setOnMouseClicked(onMouseClickedEventHandler);
        if (isSelected()){
            select();
        }
    }

    public Shape getShape() {
        return shape;
    }

    public void setColor(Color color){
        this.shape.setFill(color);
    }

    public Color getColor() {
        return color;
    }


    private EventHandler<MouseEvent> onMouseClickedEventHandler = new EventHandler<>() {
        @Override
        public void handle(MouseEvent event) {
            MouseButton button = event.getButton();
            if(button==MouseButton.PRIMARY) {
                dataManager.getTableView().getSelectionModel().clearSelection();
                for (Cell cells : dataManager.getCells()
                        ) {
                    cells.setFillColor(cells.getColor());
                    if (!hasResources(cells))cells.setFillColor(Color.DARKGRAY);
                }
                for (PlotData pd : dataManager.getPlotData()
                        ) {
                    if (!pd.isSelected()) {
                        pd.updateColor(Color.BLACK, 1);
                        pd.setSelected(false);
                    }
                }
                if (getDataId() != null) {
                    select();
                }
            }
            if(button==MouseButton.SECONDARY) {
                dataManager.getConMenu().getContextMenu().show((Shape)event.getSource(),event.getSceneX(),event.getSceneY());
                dataManager.getConMenu().setPlotData();
            }
        }
    };

    private void paintSolutionNodes() {
        for (Node node : solution.graph.getNodes().values()
                ) {
            if (solution.nodeMap.get(node).getAssignments().length != 0) {
                for (Cell cell : dataManager.getCells()
                        ) {
                    if (cell.getCellId().equals(node.name)) {
                        cell.setIndicator();
                    }
                }
            }
        }
    }
    private boolean hasResources(Cell cell){
        return cell.hasResources();
    }
    public Integer getDataId() {
        return dataId;
    }

    public void setDataId(Integer dataId) {
        this.dataId = dataId;
    }

    XYChart.Data getXyData() {
        return xyData;
    }


    public double getX() {
        return x;
    }

    public void updateColor(Color color, double width) {
        shape.setStroke(color);
        shape.setStrokeWidth(width);
    }

    private static String toHex(Color color) {
        return String.format("#%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255)
        );
    }

    public Solution getSolution() {
        return solution;
    }

    public boolean isSelected() {
        return selected;
    }

    private void setSelected(boolean selected) {
        this.selected = selected;
    }

    public void select(){
        dataManager.getPlotDataWithId(getDataId()).setSelected(true);
        for (int i = 0; i < dataManager.getTableView().getItems().size(); i++) {
            if (dataManager.getTableView().getItems().get(i).equals(
                    dataManager.getTableDataWithId(getDataId()))) {
                dataManager.getTableView().getSelectionModel().select(i);

            }
        }
        dataManager.getPlotDataWithId(getDataId()).updateColor(Color.RED, 4);
        paintSolutionNodes();
    }
    public void delte(){
        TableData toDelte = dataManager.getTableDataWithId(this.dataId);
        dataManager.getPlotData().remove(this);
        this.setSelected(false);
        dataManager.getTableData().remove(toDelte);
        dataManager.getTableData().add(toDelte);
        toDelte.setDeleted(true);
        dataManager.reInitiate();
    }
}
