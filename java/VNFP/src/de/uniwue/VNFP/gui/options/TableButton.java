package de.uniwue.VNFP.gui.options;

import de.uniwue.VNFP.gui.core.DataManager;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Parent;
import javafx.scene.control.Button;

import java.io.IOException;

/**
 * Created by Simon Raffeck on 10.04.18.
 */
public class TableButton {
    private boolean tableOptionsClicked = false;
    private Button options=new Button("Table Options");
    TableButton(DataManager dataManager) throws IOException {

        options.setText("Table Options");
        EventHandler<ActionEvent> tableOptions = event -> {
            if (!tableOptionsClicked) {
                TableOptions tableOptions1 = null;
                try {
                    tableOptions1 = new TableOptions(dataManager);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                assert tableOptions1 != null;
                tableOptions1.getParent().prefWidth(dataManager.getController().getTablePane().getWidth());
                dataManager.getController().getTablePane().getWidth();
                dataManager.getController().setTable(tableOptions1.getParent());
                tableOptionsClicked = true;
            } else {
                dataManager.reInitiate();
                dataManager.getController().setTable(dataManager.getTableView());
                tableOptionsClicked = false;
            }
        }; options.setOnAction(tableOptions);

    }

    public Parent getPane() {
        return options;
    }
}
