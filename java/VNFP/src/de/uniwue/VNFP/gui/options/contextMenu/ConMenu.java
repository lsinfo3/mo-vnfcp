package de.uniwue.VNFP.gui.options.contextMenu;

import de.uniwue.VNFP.gui.core.DataManager;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;

/**
 * Created by Simon Raffeck on 20.12.17.
 */
public class ConMenu {
    private DataManager dataManager;
    private ContextMenu contextMenu = new ContextMenu();

    public ConMenu(){
        MenuItem delete = new MenuItem("Delete Data");
        contextMenu.getItems().add(delete);
        MenuItem insert = new MenuItem("Insert Data");
        contextMenu.getItems().add(insert);
        delete.setOnAction(t -> dataManager.deleteSelected());
        insert.setOnAction(t -> dataManager.insertDeleted());
    }

    public ContextMenu getContextMenu() {
        return contextMenu;
    }

    public void setPlotData() {
    }

    public void setTableData() {
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public void setDataManager(DataManager dataManager) {
        this.dataManager = dataManager;
    }
}
