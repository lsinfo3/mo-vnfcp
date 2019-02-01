package de.uniwue.VNFP.gui.graphUI.DetailsUI;

import de.uniwue.VNFP.gui.core.DataManager;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.chart.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;


import java.io.IOException;
import java.util.ArrayList;



/**
 * Created by Simon Raffeck on 28.11.17.
 */
public class Details {
    @FXML private Button close;
    @FXML private BarChart barChart;
    @FXML private ScrollPane scrollPane;
    @FXML private CategoryAxis xAxis;
    @FXML private NumberAxis yAxis;
    private Pane contentBars= new Pane();
    private ArrayList<XYChart.Series> series = new ArrayList<>();
    private ArrayList<Rectangle> bars = new ArrayList<>();
    private ArrayList<Rectangle> barsMax = new ArrayList<>();
    private ArrayList<Label> labels = new ArrayList<>();

    private Parent root;
    private DataManager dataManager;

    public Details(DataManager dataManager) throws IOException {
        this.dataManager = dataManager;
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("details.fxml")
        );
        loader.setController(this);
        root = loader.load();
        close.setText("Close");
        close.setOnAction(closeButton);
        xAxis.setLabel("VNFInstances");
        yAxis.setLabel("Usage (Mbps)");
        barChart.getXAxis().setTickLabelsVisible(true);
        barChart.setLegendVisible(false);
        scrollPane.setContent(contentBars);
        scrollPane.setStyle("-fx-background-color:transparent;");
        init();
        populateBarChart();
    }

    private void init(){
        double x = 3.0;
        double labelY = 5.0;
        double barY = 25;
        for (int i = 0; i < dataManager.getO().TOTAL_USED_RESOURCES.length; i++) {
            Label label = new Label(dataManager.getO().TOTAL_USED_RESOURCES[i].toString());
            Rectangle rec = new Rectangle();
            rec.setArcHeight(5);
            rec.setArcWidth(5);
            rec.setHeight(26);
            rec.setWidth(200);
            rec.setStroke(Color.BLACK);
            rec.setFill(Color.color(1, 0.78, 0.5));
            rec.getStyleClass().add("detailBars");
            Rectangle recMax = new Rectangle();
            recMax.setArcHeight(5);
            recMax.setArcWidth(5);
            recMax.setHeight(26);
            recMax.setWidth(200);
            recMax.setStroke(Color.BLACK);
            recMax.setFill(Color.TRANSPARENT);
            recMax.getStyleClass().add("detailBars");
            rec.setLayoutX(x);
            rec.setLayoutY(barY);
            recMax.setLayoutX(x);
            recMax.setLayoutY(barY);
            label.setLayoutX(x);
            label.setLayoutY(labelY);
            labelY =labelY + 55;
            barY=barY+55;
            contentBars.getChildren().add(rec);
            contentBars.getChildren().add(recMax);
            contentBars.getChildren().add(label);
            this.labels.add(label);
            this.bars.add(rec);
            this.barsMax.add(recMax);
        }
    }

    public void populateBarChart(){
        barChart.getData().addAll(series);
        for (XYChart.Series s:series
                ) {
            for (int i = 0; i <s.getData().size() ; i++) {
                int index=this.dataManager.getVnfTypes().indexOf(((XYChart.Data) s.getData().get(i)).getXValue().toString());
                ((XYChart.Data) s.getData().get(i)).getNode().setStyle(calculateBarColors(index));
            }


        }
    }
    public void addDataToSeries(String name, double value, String serie){
        XYChart.Data data = new XYChart.Data(name,value);
        for (XYChart.Series s:series
             ) {
            if (s.getName().equals(serie)){
                s.getData().add(data);
                return;
            }
        }
        XYChart.Series series = new XYChart.Series();
        series.setName(serie);
        this.series.add(series);
        series.getData().add(data);
    }
    public Parent getPane() {
        return root;
    }


    public void setBarWidth(int index,double i,String text,double max){
        if (Double.isNaN(i) || i == 0) {
            i = 1;
            bars.get(index).setWidth(i);
            barsMax.get(index).setWidth(max);
        }else {
            bars.get(index).setWidth(i);
            barsMax.get(index).setWidth(max);
        }
        labels.get(index).setText(text);
    }

    private String calculateBarColors(int i){
        int mod = i%10;
        switch(mod){
            case 0:
                return "-fx-bar-fill: navy;";
            case 1:
                return "-fx-bar-fill: red;";
            case 2:
                return "-fx-bar-fill: green;";
        }
        return null;
    }


    private EventHandler<ActionEvent> closeButton = new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent event) {
            dataManager.getController().setBottom(dataManager.getPlotUI().initialize());
            dataManager.getController().getBottom().setRight(dataManager.getController().getLegend().getPane());
            dataManager.setDetailsOpen(false);
        }
    };

    }
