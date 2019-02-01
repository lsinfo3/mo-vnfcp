package de.uniwue.VNFP.gui.plotUI.legend;

import de.uniwue.VNFP.gui.core.DataManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;

import java.io.IOException;


/**
 * Created by Simon Raffeck on 06.11.17.
 */
public class Legend {

    @FXML
    private Rectangle rect;
    @FXML
    private ColumnConstraints column2width = new ColumnConstraints();
    @FXML
    private StackPane colorLegendPane;
    @FXML
    private Label colorLegendLabel;
    @FXML
    private Label minColorValLabel;
    @FXML
    private Label maxColorValLabel;
    @FXML
    private Label unfeasibleLabel;


    private Parent root;

    private DataManager dataManager;

    public Legend(DataManager dataManager) throws IOException {

        this.dataManager = dataManager;

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("legend.fxml")
        );
        loader.setController(this);
        root = loader.load();

        // Gradient
        Stop[] stops = new Stop[]{
                new Stop(0, Color.BLACK),
                new Stop(0.8, Color.color(1, 0.63, 0.4)),
                new Stop(1, Color.color(1, 0.78, 0.5))
        };
        LinearGradient lg = new LinearGradient(0, 1, 0, 0, true, CycleMethod.NO_CYCLE, stops);
        rect.setFill(lg);

        minColorValLabel.setText(dataManager.getMinColorValLabel().getText());
        maxColorValLabel.setText(dataManager.getMaxColorValLabel().getText());
        colorLegendLabel.setText(dataManager.getO().NUMBER_OF_HOPS.toString());
        getColorLabel();

    }

    public Label getColorLegendLabel() {
        return colorLegendLabel;
    }

    private void setColorLegendLabel(String colorLegendLabel) {
        this.colorLegendLabel.setText(colorLegendLabel);
    }

    public Label getMinColorValLabel() {
        return minColorValLabel;
    }

    public Label getMaxColorValLabel() {
        return maxColorValLabel;
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public StackPane getPane() {
        return (StackPane) root;
    }

    private void getColorLabel(){
        String text;
        if (dataManager.getRelevant().length<3){
            text = getColorLegendLabel().getText();
        }else{
            text=dataManager.getRelevant()[2].toString();
        }
        setColorLegendLabel(text);
    }

}
