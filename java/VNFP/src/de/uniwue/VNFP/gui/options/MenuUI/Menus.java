package de.uniwue.VNFP.gui.options.MenuUI;

import de.uniwue.VNFP.gui.Gui;
import javafx.scene.Scene;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;

/**
 * Created by Simon Raffeck on 15.12.17.
 */
public class Menus {
    private MenuBar menuBar;
    private Gui gui;
    public Menus(Scene scene,Gui gui){
        menuBar = new MenuBar();
        this.gui=gui;
        Menu menuView = new Menu("View");
        MenuItem darkTheme = new MenuItem("Dark Theme");
        MenuItem reset = new MenuItem("Reset Data");
        CheckMenuItem tableView = new CheckMenuItem("Show Table");
        tableView.setSelected(true);
        CheckMenuItem plotView = new CheckMenuItem("Show Plot");
        plotView.setSelected(true);
        CheckMenuItem graphView = new CheckMenuItem("Show Graph");
        graphView.setSelected(true);
        reset.setOnAction(t -> reset());
        darkTheme.setOnAction(t -> darkTheme(scene));
        tableView.setOnAction(t -> {
            if (tableView.isSelected()){
                gui.getController().showTable();
            }else{
                gui.getController().deleteTable();
            }
        });
        plotView.setOnAction(t -> {
            if (plotView.isSelected()){
                gui.getController().showPlot();
            }else{
                gui.getController().deletePlot();
            }
        });
        graphView.setOnAction(t -> {
            if (graphView.isSelected()){
                gui.getController().showGraph();
            }else{
                gui.getController().deleteGraph();
            }
        });
        menuView.getItems().addAll(tableView);
        menuView.getItems().addAll(plotView);
        menuView.getItems().addAll(graphView);

        menuBar.getMenus().addAll(menuView);
    }
    private void darkTheme(Scene scene){
        scene.getStylesheets().remove("application.css");
        scene.getStylesheets().add(gui.getClass().getResource("darkApplication.css").toExternalForm());
    }
    private void reset(){
        gui.getController().getDataManager().reset();
    }

    public MenuBar getMenuBar() {
        return menuBar;
    }
}
