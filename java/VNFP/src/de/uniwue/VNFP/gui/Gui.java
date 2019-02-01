package de.uniwue.VNFP.gui;


import de.uniwue.VNFP.algo.ParetoFrontier;
import de.uniwue.VNFP.gui.options.MenuUI.Menus;
import de.uniwue.VNFP.gui.core.Controller;
import de.uniwue.VNFP.util.Config;
import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.scene.Scene;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.io.FileInputStream;
import java.io.FileNotFoundException;


/**
 * Created by Simon Raffeck on 06.11.17.
 */
public class Gui extends Application {
    public static ParetoFrontier frontier;
    private Controller controller;
    @Override
    public void start(Stage primaryStage) {
        Controller controller = new Controller(frontier);
        this.controller=controller;
        BorderPane root = controller.initialize();
        Scene scene = new Scene(root, 1024, 768);
        scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
        EventHandler<KeyEvent> onKeyPressed = event -> {
            KeyCode pressed = event.getCode();
            if (pressed == KeyCode.DELETE) {
                controller.getDataManager().deleteSelected();
            }
            if (pressed == KeyCode.INSERT) {
                controller.getDataManager().insertDeleted();
            }
        };
        scene.setOnKeyPressed(onKeyPressed);
        Menus menuBar = new Menus(scene,this);

        root.setTop(menuBar.getMenuBar());

        primaryStage.setScene(scene);
        primaryStage.setTitle("VNFCP Optimization");
        primaryStage.setMaximized(true);
        primaryStage.show();
        this.controller.setTable(this.controller.getDataManager().getTableView());
    }

    public static void main(String[] args) throws FileNotFoundException {
        Config.getInstance(new FileInputStream(args[0]));
        launch(args);
    }

    public Controller getController() {
        return controller;
    }
}
