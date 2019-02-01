package de.uniwue.VNFP.gui.core;

import de.uniwue.VNFP.algo.ParetoFrontier;
import de.uniwue.VNFP.gui.graphUI.GraphUI;
import de.uniwue.VNFP.gui.plotUI.legend.Legend;
import de.uniwue.VNFP.gui.options.Options;
import de.uniwue.VNFP.gui.plotUI.Plot;
import de.uniwue.VNFP.gui.tableUI.Table;
import de.uniwue.VNFP.gui.tableUI.TableData;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.chart.Chart;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;

import java.io.IOException;

/**
 * Created by Simon Raffeck on 08.11.17.
 */
public class Controller {
    private boolean tableShown;
    private boolean plotShown;
    private boolean graphShown;
    private double splitterPos=0;


    private void setPlotShown() {
        this.plotShown = true;
    }

    private void setGraphShown() {
        this.graphShown = true;
    }

    private void setTableShown() {
        this.tableShown = true;
    }

    public ParetoFrontier frontier;
    private DataManager dataManager;
    private BorderPane root;
    private BorderPane top;
    private BorderPane bottom;
    private BorderPane tablePane;

    private SplitPane leftRight;
    private SplitPane topBottom;


    private Chart chart;
    private ScrollPane graphUi;
    private TableView tableView;

    private GraphUI graph;
    private Legend legend;
    private Options options;

    public Controller(ParetoFrontier frontier){
        this.frontier=frontier;
    }

    public BorderPane initialize() {
        try {
            dataManager = new DataManager(this);
            root = new BorderPane();
            top = new BorderPane();
            bottom = new BorderPane();
            tablePane = new BorderPane();
            tablePane.setPrefWidth(300);
            Plot plot = new Plot(dataManager);
            chart = plot.initialize();
            graph = new GraphUI(dataManager);
            Table table = new Table(dataManager);

            dataManager.setGraphUI(graph);
            dataManager.setPlotUI(plot);
            dataManager.setTableUI(table);

            legend = new Legend(dataManager);
            options = new Options(dataManager);



            graphUi = graph.initialize();
            tableView = table.initialize();
            tableView.setPrefWidth(300);
            dataManager.setTableView(tableView);
            topBottom = new SplitPane(top,bottom);
            topBottom.setOrientation(Orientation.VERTICAL);
            leftRight = new SplitPane(tablePane,topBottom);
            leftRight.setDividerPositions(0.20);
            this.splitterPos=leftRight.getDividerPositions()[0];
            top.setRight(options.getPane());
            bottom.setRight(legend.getPane());
            bottom.setCenter(chart);
            top.setCenter(graphUi);
            root.setCenter(leftRight);
            setTableShown();
            setPlotShown();
            setGraphShown();
            setTable(dataManager.getTableView());
            return root;
        } catch (IOException e) {
            e.printStackTrace();
        }
        throw new IllegalArgumentException("No input");
    }

    void update() {
        this.splitterPos=leftRight.getDividerPositions()[0];
        tablePane = new BorderPane();
        tablePane.setCenter(tableView);
        tablePane.setTop(options.getTableButton().getPane());
        BorderPane.setAlignment(options.getTableButton().getPane(), Pos.CENTER);
        topBottom = new SplitPane(top,bottom);
        topBottom.setOrientation(Orientation.VERTICAL);
        leftRight = new SplitPane(tablePane,topBottom);
        leftRight.setDividerPositions(this.splitterPos);
        top.setRight(options.getPane());
        bottom.setRight(legend.getPane());
        bottom.setCenter(chart);
        top.setCenter(graphUi);
        root.setCenter(leftRight);
    }

    public BorderPane getRoot() {
        return root;
    }

    public void setRoot(BorderPane root) {
        this.root = root;
    }

    void setChart(Chart chart) {
        this.chart = chart;
    }

    void setTableView(TableView<TableData> tableView) {
        this.tableView = tableView;
    }

    public GraphUI getGraph() {
        return graph;
    }

    public void setGraph(GraphUI graph) {
        this.graph = graph;
    }
    public void setGraphUI(ScrollPane graphUI){this.graphUi = graphUI;}

    public Legend getLegend() {
        return legend;
    }


    Options getOptions() {
        return options;
    }

    public void setBottom(Parent pane){this.bottom.setCenter(pane);}

    public BorderPane getBottom() {
        return bottom;
    }

    public void setTable(Parent parent){
        tablePane.setCenter(parent);
        tablePane.setTop(options.getTableButton().getPane());
        BorderPane.setAlignment(options.getTableButton().getPane(), Pos.CENTER);
        root.setCenter(leftRight);
    }
    public void deleteTable(){
        this.tableShown=false;
        tablePane = new BorderPane();
        setLeftRight();

    }
    public void showTable(){
        this.tableShown=true;
        tablePane = new BorderPane();
        tablePane.setCenter(tableView);
        tablePane.setTop(options.getTableButton().getPane());
        BorderPane.setAlignment(options.getTableButton().getPane(), Pos.CENTER);
        setLeftRight();
    }
    public void deletePlot(){
        this.plotShown=false;
        bottom = new BorderPane();
        setLeftRight();
    }
    public void showPlot(){
        this.plotShown=true;
        bottom = new BorderPane();
        bottom.setCenter(chart);
        bottom.setRight(legend.getPane());
        setLeftRight();
    }
    public void deleteGraph(){
        this.graphShown=false;
        top = new BorderPane();
        top.setCenter(new Pane());
        setLeftRight();
    }
    public void showGraph(){
        this.graphShown=true;
        top = new BorderPane();
        top.setCenter(graphUi);
        top.setRight(options.getPane());
        setLeftRight();
    }
    private void setLeftRight(){
        if (this.tableShown){
            this.leftRight = new SplitPane(tablePane,setTopBottom());
            leftRight.setDividerPositions(0.20);
            root.setCenter(leftRight);
        }else{
            root.setCenter(setTopBottom());
        }
    }
    private Parent setTopBottom(){
        if (this.plotShown && this.graphShown){
            this.topBottom = new SplitPane(top,bottom);
            topBottom.setOrientation(Orientation.VERTICAL);
            return topBottom;
        }
        if (!this.plotShown){
            if (!this.graphShown){

            }
            return top;
        }
        if(!this.graphShown){
            if (!this.plotShown){

            }
            return bottom;
        }
        return topBottom;
    }

    public BorderPane getTablePane() {
        return tablePane;
    }

    public DataManager getDataManager() {
        return dataManager;
    }
}
