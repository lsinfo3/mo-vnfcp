package de.uniwue.VNFP.gui.plotUI;


import de.uniwue.VNFP.gui.core.DataManager;
import javafx.scene.chart.*;
import java.util.HashMap;

/**
 * Created by Simon Raffeck on 06.11.17.
 */
public class Plot {

    private String xAxisString = "Total Delay";
    private String yAxisString = "Total Used CPU";

    private DataManager dataManager;

    private HashMap<XYChart.Data, PlotData> plotValues = new HashMap<>();


    public Plot(DataManager dataManager) {
        this.dataManager = dataManager;
        this.dataManager.setPlotUI(this);
        this.dataManager.getXAxisString();
        this.dataManager.getYAxisString();
    }

    public Chart initialize() {
        NumberAxis xAxis = new NumberAxis();
        NumberAxis yAxis = new NumberAxis();
        ScatterChart<Number, Number> scatterChart = new ScatterChart<>(xAxis, yAxis);
        plotValues.clear();
        xAxis.setLabel(xAxisString);
        xAxis.setForceZeroInRange(false);
        yAxis.setLabel(yAxisString);
        yAxis.setForceZeroInRange(false);
        //creating the chart

        scatterChart.setTitle("Pareto Frontier [" + dataManager.getFront().size() + " elements]");
        scatterChart.setLegendVisible(false);
        //defining a series
        XYChart.Series series = new XYChart.Series();
        series.setName("Solution Points");
        //populating the series with data
        for (PlotData pd : this.dataManager.getPlotData()
                ) {
            series.getData().add(pd.getXyData());
            this.plotValues.put(pd.getXyData(), pd);
        }
        scatterChart.getData().add(series);
        return scatterChart;
    }
    public DataManager getDataManager() {
        return dataManager;
    }

    public void setDataManager(DataManager dataManager) {
        this.dataManager = dataManager;
    }


    public String getxAxisString() {
        return xAxisString;
    }

    public void setxAxisString(String xAxisString) {
        this.xAxisString = xAxisString;
    }

    public String getyAxisString() {
        return yAxisString;
    }

    public void setyAxisString(String yAxisString) {
        this.yAxisString = yAxisString;

    }

}
