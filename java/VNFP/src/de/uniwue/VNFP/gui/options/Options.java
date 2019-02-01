package de.uniwue.VNFP.gui.options;

import de.uniwue.VNFP.gui.core.DataManager;
import de.uniwue.VNFP.gui.graphUI.graph.Edge;
import de.uniwue.VNFP.model.Objs;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;

import java.io.IOException;

/**
 * Created by Simon Raffeck on 14.11.17.
 */
public class Options {
    @FXML
    private AnchorPane anchorPane;
    @FXML
    private ComboBox<String> xaxis;
    @FXML
    private ComboBox<String> yaxis;
    @FXML
    private Label displayOptions;
    @FXML
    private ComboBox<String> colorPicker;
    @FXML
    private Label graphOptions;
    @FXML private CheckBox linkDetails;
    private boolean linksClicked = false;


    private Parent root;
    private DataManager dataManager;

    private TableButton tableButton;


    public Options(DataManager dataManager) throws IOException {
        this.dataManager = dataManager;
        this.tableButton= new TableButton(dataManager);
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("options.fxml")
        );
        loader.setController(this);
        root = loader.load();
        ObservableList<String> graphItems = FXCollections.observableArrayList();
        graphItems.add("Reset");
        ObservableList<String> options = FXCollections.observableArrayList();
        for (Objs.Obj val:dataManager.getO().values()) {
            options.add(val.toString());
        }
        linkDetails.setText("Show Used Bandwidth");
        EventHandler<ActionEvent> showLinkDetails = event -> {
            if (linksClicked) {
                for (Edge edge : dataManager.getGraphUI().getGraph().getModel().getAllEdges()
                        ) {
                    edge.getTooltip().setOpacity(0);
                }
                linksClicked = false;
            } else {
                for (Edge edge : dataManager.getGraphUI().getGraph().getModel().getAllEdges()
                        ) {
                    edge.getTooltip().setOpacity(1.0);
                }
                linksClicked = true;
            }
        }; linkDetails.setOnAction(showLinkDetails);
        linkDetails.setPrefWidth(180);
        colorPicker.setPromptText("Adjust Coloring");
        displayOptions.setText("Plot Coloring");
        graphOptions.setText("Graph Utilities");
        xaxis.setPromptText("Set X-Axis Display");
        yaxis.setPromptText("Set Y-Axis Display");
        xaxis.setItems(options);
        yaxis.setItems(options);
        EventHandler<ActionEvent> xAxisHandler = event -> {
            if (xaxis.getSelectionModel().getSelectedItem() == null) {
                return;
            }
            if (yaxis.getSelectionModel().getSelectedItem() == null) {
                return;
            }
            for (Objs.Obj val : dataManager.getO().values()) {
                if (val.toString().equals(xaxis.getSelectionModel().getSelectedItem())) {
                    dataManager.setIndexX(val.i);
                    dataManager.getPlotUI().setxAxisString(xaxis.getSelectionModel().getSelectedItem());
                }

            }
            dataManager.reInitiate();
        }; xaxis.setOnAction(xAxisHandler);
        EventHandler<ActionEvent> yAxisHandler = event -> {
            if (xaxis.getSelectionModel().getSelectedItem() == null) {
                return;
            }
            if (yaxis.getSelectionModel().getSelectedItem() == null) {
                return;
            }
            for (Objs.Obj val : dataManager.getO().values()) {
                if (val.toString().equals(yaxis.getSelectionModel().getSelectedItem())) {
                    dataManager.setIndexY(val.i);
                    dataManager.getPlotUI().setyAxisString(yaxis.getSelectionModel().getSelectedItem());
                }
            }
            dataManager.reInitiate();
        };
        yaxis.setOnAction(yAxisHandler);
        xaxis.getSelectionModel().select(dataManager.getPlotUI().getxAxisString());
        yaxis.getSelectionModel().select(dataManager.getPlotUI().getyAxisString());
        colorPicker.setItems(options);
        colorPicker.getSelectionModel().select(dataManager.getController().getLegend().getColorLegendLabel().getText());

        EventHandler<ActionEvent> colorPickerChange = event -> {
            if (colorPicker.getSelectionModel().getSelectedItem() == null) {
                return;
            }
            updateLegend();

        };
        colorPicker.setOnAction(colorPickerChange);

        Tooltip xAxisTp = new Tooltip();
        xAxisTp.setText("Change X-Axis");
        Tooltip yAxisTp = new Tooltip();
        yAxisTp.setText("Change Y-Axis");
        Tooltip legendTp = new Tooltip();
        legendTp.setText("Change Coloring");
        Tooltip graphTp = new Tooltip();
        graphTp.setText("Change graph");

        xaxis.setTooltip(xAxisTp);
        yaxis.setTooltip(yAxisTp);
        colorPicker.setTooltip(legendTp);


    }


    public Parent getPane() {
        return root;
    }

    public void updateLegend(){
        for (Objs.Obj val:dataManager.getO().values()) {
            if (val.toString().equals(colorPicker.getSelectionModel().getSelectedItem())) {
                dataManager.setIndexC(val.i);
                dataManager.recolorPlot();
                dataManager.getController().getLegend().getColorLegendLabel().setText(val.toString());
                dataManager.getController().getLegend().getMaxColorValLabel().setText(dataManager.getMaxColorValLabel().getText());
                dataManager.getController().getLegend().getMinColorValLabel().setText(dataManager.getMinColorValLabel().getText());
            }
        }
    }

    public TableButton getTableButton() {
        return tableButton;
    }
}
