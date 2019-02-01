package de.uniwue.VNFP.gui.core;


import de.uniwue.VNFP.algo.ParetoFrontier;
import de.uniwue.VNFP.gui.options.contextMenu.ConMenu;
import de.uniwue.VNFP.gui.graphUI.GraphUI;
import de.uniwue.VNFP.gui.graphUI.cell.Cell;
import de.uniwue.VNFP.gui.plotUI.Plot;
import de.uniwue.VNFP.gui.plotUI.PlotData;
import de.uniwue.VNFP.gui.tableUI.Table;
import de.uniwue.VNFP.gui.tableUI.TableData;
import de.uniwue.VNFP.model.*;
import de.uniwue.VNFP.model.factory.FlowPlacementReader;
import de.uniwue.VNFP.model.factory.TopologyFileReader;
import de.uniwue.VNFP.model.factory.TrafficRequestsReader;
import de.uniwue.VNFP.model.factory.VnfLibReader;
import de.uniwue.VNFP.model.solution.Solution;
import de.uniwue.VNFP.util.Config;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;


import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by Simon Raffeck on 06.11.17.
 */
public class DataManager {

    private List<Cell> cells = new ArrayList<>();
    private List<TableData> tableData = new ArrayList<>();
    private List<PlotData> plotData = new ArrayList<>();

    private ArrayList<String> vnfTypes = new ArrayList<>();

    private List<TableData> backUpTableData = new ArrayList<>();
    private List<PlotData> backUpPlotData = new ArrayList<>();

    private TableView<TableData> tableView;

    private Controller controller;

    private GraphUI graphUI;
    private Plot plotUI;
    private Table tableUI;


    private List<Stop> colorStops = new ArrayList<>();
    private double minColorVal;
    private double maxColorVal;
    private Rectangle rect = new Rectangle(30, 250);
    private ColumnConstraints column2width = new ColumnConstraints();
    private StackPane colorLegendPane = new StackPane();
    private Label colorLegendLabel = new Label();
    private Label minColorValLabel = new Label();
    private Label maxColorValLabel = new Label();
    private Label unfeasibleLabel = new Label();

    private ConMenu conMenu = new ConMenu();

    private DecimalFormat formatter;

    private ArrayList<CheckBox> checkBoxes = new ArrayList<>();

    private String input_topology = "res/problem_instances/internet2/topology";
    private String input_requests = "res/problem_instances/internet2/requests";
    private String input_vnfs = "res/problem_instances/internet2/vnfLib";
    private String placementFlows = "res/problem_instances/internet2/placement_flows";

    private VnfLib vnfLib = VnfLibReader.readFromFile(input_vnfs);
    private NetworkGraph ng = TopologyFileReader.readFromFile(input_topology, vnfLib);
    private TrafficRequest[] reqs = TrafficRequestsReader.readFromFile(input_requests, ng, vnfLib);

    private Objs o = new Objs(vnfLib.res);
    private int indexX = o.TOTAL_DELAY.i;
    private int indexY = o.TOTAL_USED_RESOURCES[0].i;
    private int indexC = o.NUMBER_OF_HOPS.i;

    private Config config;
    private boolean detailsOpen = false;

    private ParetoFrontier front = FlowPlacementReader.readFromCsv(new ProblemInstance(ng, vnfLib, reqs, o), placementFlows);


    public DataManager(Controller controller) throws IOException {
        this.controller = controller;
        if (this.controller.frontier!=null){
            front=this.controller.frontier;
        }else {
            System.err.println("No ParetoFrontier detected!  Starting with Example...");
        }
        config = Config.getInstance();
        formatter = new DecimalFormat("#.##");
        for (Objs.Obj val:o.values()
                ) {
            CheckBox checkBox = new CheckBox();
            checkBox.setText(val.toString());
            checkBox.setSelected(false);
            checkBoxes.add(checkBox);
        }
        if (getRelevant().length>2){
            indexX=getRelevant()[0].i;
            indexY=getRelevant()[1].i;
        }
        setRelevantCheckboxes();
        initialize();
        backUpPlotData.addAll(plotData);
        backUpTableData.addAll(tableData);
    }

    private void initialize() {
        for (int i = 0; i < front.size(); i++) {
            addSolutionToTable(i);
        }
        colors();
        for (int i = 0; i < front.size(); i++) {
            addSolutionToPlot(i);
        }
        addSolutionToGraph();
    }

    private void update(){
        updatePlot();
        updateTable();
    }
    private void updatePlot(){
        for (PlotData pd:this.plotData
             ) {
            colors();
            Color c = getColor((pd.getSolution().vals[indexC] - minColorVal) / (maxColorVal - minColorVal));
            pd.update(c);
        }
    }
    private void updateTable(){
        for (TableData td:this.tableData
             ) {
            td.initValues();
        }
    }


    private void colors() {
        computeColorMinMax();

        // Gradient
        Stop[] stops = new Stop[]{
                new Stop(0, Color.BLACK),
                new Stop(0.8, Color.color(1, 0.63, 0.4)),
                new Stop(1, Color.color(1, 0.78, 0.5))
        };
        LinearGradient lg = new LinearGradient(0, 1, 0, 0, true, CycleMethod.NO_CYCLE, stops);
        colorStops = lg.getStops();
        rect.setFill(lg);

        // Color Labels
        updateColumn2Width();
        colorLegendLabel.textProperty().addListener(observable -> updateColumn2Width());
        minColorValLabel.textProperty().addListener(observable -> updateColumn2Width());
        maxColorValLabel.textProperty().addListener(observable -> updateColumn2Width());

    }

    private void addSolutionToPlot(int i) {

        Solution s = front.get(i);
        Color c = getColor((s.vals[indexC] - minColorVal) / (maxColorVal - minColorVal));
        PlotData plotData = new PlotData(s, this, c);
        plotData.setDataId(i);
        this.getPlotData().add(plotData);
    }

    public void recolorPlot(){
        colors();
        for (PlotData pd:this.getPlotData()
             ) {
            Color c = getColor((pd.getSolution().vals[indexC] - minColorVal) / (maxColorVal - minColorVal));
            pd.setColor(c);
        }
    }

    private void addSolutionToGraph() {
        for (Node node : front.get(0).graph.getNodes().values()
                ) {
            Cell cell = new Cell(node.name, node, front);
            cell.setDataId(0);
            this.getCells().add(cell);
        }
    }

    private void addSolutionToTable(int i) {

        Solution s = front.get(i);
        TableData tableData = new TableData(this, s);
        tableData.setDataId(i);
        this.getTableData().add(tableData);
    }


    private void updateColumn2Width() {
        double w1 = colorLegendLabel.maxWidth(colorLegendLabel.getHeight());
        double w2 = minColorValLabel.maxWidth(minColorValLabel.getHeight()) + minColorValLabel.getTranslateX();
        double w3 = maxColorValLabel.maxWidth(maxColorValLabel.getHeight()) + maxColorValLabel.getTranslateX();
        double w4 = unfeasibleLabel.maxWidth(unfeasibleLabel.getHeight()) + unfeasibleLabel.getTranslateX();
        double w = Math.max(w4, Math.max(w3, Math.max(w2, w1)));

        column2width.setPrefWidth(w);
        colorLegendPane.setPrefWidth(w);
    }

    private void computeColorMinMax() {
        final int i = indexC;
        minColorVal = tableData.stream().filter(d->!d.isDeleted()).map(d->d.getSolution().vals[i]).min(Double::compareTo).orElse(0.0);
        maxColorVal = tableData.stream().filter(d->!d.isDeleted()).map(d->d.getSolution().vals[i]).max(Double::compareTo).orElse(1.0);
        if (minColorVal == maxColorVal) maxColorVal = minColorVal + 1;
        minColorValLabel.setText(formatter.format(minColorVal));
        maxColorValLabel.setText(formatter.format(maxColorVal));
    }

    public Label getMinColorValLabel() {
        return minColorValLabel;
    }

    public Label getMaxColorValLabel() {
        return maxColorValLabel;
    }

    private Color getColor(double t) {
        if (t < 0 || t > 1) {
            throw new IllegalArgumentException("t = " + t);
        }

        for (int i = 1; i < colorStops.size(); i++) {
            Stop s = colorStops.get(i);
            Stop p = colorStops.get(i - 1);
            if (t <= s.getOffset()) {
                double start = p.getOffset();
                double end = s.getOffset();

                return p.getColor().interpolate(s.getColor(), (t - start) / (end - start));
            }
        }

        throw new IllegalArgumentException("t > last stop's offset: t=" + t + ", offset=" + colorStops.get(colorStops.size() - 1));
    }
    public TableData getTableDataWithId(int id) {
        for (TableData tb : this.tableData
                ) {
            if (tb.getDataId() == id) {
                return tb;
            }
        }
        return null;
    }

    public PlotData getPlotDataWithId(int id) {
        for (PlotData pd : this.plotData
                ) {
            if (pd.getDataId() == id) {
                return pd;
            }
        }
        return null;
    }

    public TableView getTableView() {
        return tableView;
    }

    public void setTableView(TableView<TableData> tableView) {
        this.tableView = tableView;
    }

    public List<TableData> getTableData() {
        return tableData;
    }

    public List<Cell> getCells() {
        return cells;
    }

    public void setCells(List<Cell> cells) {
        this.cells = cells;
    }

    public List<PlotData> getPlotData() {
        return plotData;
    }

    public ParetoFrontier getFront() {
        return this.front;
    }

    public int getIndexX() {
        return indexX;
    }

    public int getIndexY() {
        return indexY;
    }

    public void setIndexX(int indexX) {
        this.indexX = indexX;
    }

    public void setIndexY(int indexY) {
        this.indexY = indexY;
    }

    public void setIndexC(int indexC) {
        this.indexC = indexC;
    }


    public void reInitiate() {
        update();
        this.controller.setChart(plotUI.initialize());
        getController().getOptions().updateLegend();
        TableView tableView = tableUI.initialize();
        this.controller.setTableView(tableView);
        this.controller.update();
        setTableView(tableView);
        for (PlotData pd:this.plotData
             ) {
            if (pd.isSelected()){
                pd.select();
            }
        }
    }

    public GraphUI getGraphUI() {
        return graphUI;
    }

    public void setGraphUI(GraphUI graphUI) {
        this.graphUI = graphUI;
    }

    public Plot getPlotUI() {
        return plotUI;
    }

    public void setPlotUI(Plot plotUI) {
        this.plotUI = plotUI;
    }

    public Table getTableUI() {
        return tableUI;
    }

    void setTableUI(Table tableUI) {
        this.tableUI = tableUI;
    }

    public Controller getController() {
        return controller;
    }

    public ArrayList<CheckBox> getCheckBoxes() {
        return checkBoxes;
    }

    private void setRelevantCheckboxes(){
        Objs.Obj[] objs = config.getRelevantObjectives(vnfLib);
        for (Objs.Obj obj : objs) {
            for (CheckBox cb : getCheckBoxes()
                    ) {
                if (cb.getText().equals(obj.toString())) {
                    cb.setSelected(true);
                }
            }
        }
    }

    public Objs.Obj[] getRelevant(){
        return config.getRelevantObjectives(vnfLib);
    }

    public ArrayList<String> getChecked(){
        ArrayList<String> res = new ArrayList<>();
        for (CheckBox cb:getCheckBoxes()
                ) {
            if (cb.isSelected()) {
                res.add(cb.getText());
            }
        }
        return res;
    }
    public double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    public ConMenu getConMenu() {
        return conMenu;
    }
    public void reset(){
        plotData.clear();
        tableData.clear();
        plotData.addAll(backUpPlotData);
        for (TableData td:backUpTableData
             ) {
            td.setDeleted(false);
        }
        tableData.addAll(backUpTableData);
        reInitiate();
    }

    public void deleteSelected(){
        ArrayList<TableData> selectedTD = new ArrayList<>();
        selectedTD.addAll(tableView.getSelectionModel().getSelectedItems());
        for (TableData toDelete:selectedTD
             ) {
            toDelete.delte();
        }
        ArrayList<PlotData> selectedPD = new ArrayList<>();
        for (PlotData pd:getPlotData()
                ) {
            if (pd.isSelected()) {
                selectedPD.add(pd);
            }
        }
        for (PlotData pd:selectedPD
                ) {
            pd.delte();
        }
        tableUI.getTable().sort();
    }

    public void insertDeleted() {
        for (TableData toInsert : tableView.getSelectionModel().getSelectedItems()
                ) {
            for (PlotData pd : backUpPlotData
                    ) {
                if (!this.plotData.contains(pd)) {
                    if (pd.getDataId().equals(toInsert.getDataId())) {
                        plotData.add(pd);
                        toInsert.setDeleted(false);
                    }
                }
            }
        }
        reInitiate();
        tableUI.getTable().sort();
    }
    public ArrayList<String> getVnfTypes() {
        return vnfTypes;
    }

    public NetworkGraph getNg() {
        return ng;
    }

    public Objs getO() {
        return o;
    }

    public Config getConfig() {
        return config;
    }

    public VnfLib getVnfLib() {
        return vnfLib;
    }

    public void getXAxisString(){
        String text;
        if (getRelevant().length<1){
            text = this.plotUI.getxAxisString();
        }else{
            text=getRelevant()[0].toString();
        }
        this.plotUI.setxAxisString(text);
    }
    public void getYAxisString(){
        String text;
        if (getRelevant().length<2){
            text = this.plotUI.getyAxisString();
        }else{
            text=getRelevant()[1].toString();
        }
        this.plotUI.setyAxisString(text);
    }


    public boolean isDetailsOpen() {
        return detailsOpen;
    }

    public void setDetailsOpen(boolean detailsOpen) {
        this.detailsOpen = detailsOpen;
    }
}
