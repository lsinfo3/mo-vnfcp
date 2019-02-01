package de.uniwue.VNFP.gui.options;

import de.uniwue.VNFP.gui.core.DataManager;
import javafx.scene.Parent;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;

import java.io.IOException;

class TableOptions {
    private AnchorPane root = new AnchorPane();
    private VBox container= new VBox();

    private DataManager dataManager;

    TableOptions(DataManager dataManager) throws IOException {
        this.dataManager = dataManager;
        container.setPrefWidth(270);
        container.setMinHeight(400);
        root.setPrefWidth(300);
        root.getChildren().add(container);
        root.getChildren().get(0).setLayoutX(10);
        container.setLayoutX(10);
        initializeCheckboxes();
    }

    private void initializeCheckboxes(){
        double x = 3.0;
        double y = 3.0;
        for (CheckBox cb:dataManager.getCheckBoxes()
             ) {
            this.container.getChildren().add(cb);
            cb.setLayoutX(x);
            cb.setLayoutY(y);
            y = y + 5;

        }
    }
    Parent getParent() {
        return root;
    }

}
