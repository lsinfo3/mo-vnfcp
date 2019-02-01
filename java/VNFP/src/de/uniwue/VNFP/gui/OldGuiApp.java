package de.uniwue.VNFP.gui;

import de.uniwue.VNFP.algo.ParetoFrontier;
import de.uniwue.VNFP.model.solution.Solution;
import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import java.text.DecimalFormat;
import java.util.List;

public class OldGuiApp extends Application {
    public static int indexX = 9;
    public static int indexY = 9;
    public static int indexC = 9;

    public static ParetoFrontier frontier;
    private static double minColorVal;
    private static double maxColorVal;

    private DecimalFormat formatter;

    private XYChart.Series<Number, Number>[] points;
    private List<Stop> colorStops;

    @FXML private Rectangle rect;
    @FXML private LineChart<Number, Number> chart;
    @FXML private NumberAxis xAxis;
    @FXML private NumberAxis yAxis;
    @FXML private ColumnConstraints column2width;
    @FXML private StackPane colorLegendPane;
    @FXML private Label colorLegendLabel;
    @FXML private Label minColorValLabel;
    @FXML private Label maxColorValLabel;
    @FXML private Label unfeasibleLabel;

    @Override
    public void start(Stage primaryStage) throws Exception {
        if (frontier == null) {
            throw new IllegalStateException("GUI launched without frontier");
        }
        formatter = new DecimalFormat("#.##");
        points = new XYChart.Series[frontier.size()];

        primaryStage.setTitle("VNFCP Optimization");
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("OldGuiApp.fxml")
        );
        loader.setController(this);
        Parent root = loader.load();
        primaryStage.setScene(new Scene(root));
        root.applyCss();
        root.layout();
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

        // LineChart
        xAxis.setLabel("Total Delay");
        xAxis.setForceZeroInRange(false);
        yAxis.setLabel("Total Used CPU");
        yAxis.setForceZeroInRange(false);
        colorLegendLabel.setText("Num. of Hops");

        chart.setTitle("Pareto Frontier [" + frontier.size() + " elements]");

        for (int i = 0; i < frontier.size(); i++) {
            addSolutionToChart(i);
        }

        // Color Labels
        updateColumn2Width();
        colorLegendLabel.textProperty().addListener(observable -> updateColumn2Width());
        minColorValLabel.textProperty().addListener(observable -> updateColumn2Width());
        maxColorValLabel.textProperty().addListener(observable -> updateColumn2Width());

        primaryStage.show();
    }

    // TODO: May be obsolete now.
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
        minColorVal = frontier.stream().map(s -> s.vals[i]).min(Double::compareTo).orElse(0.0);
        maxColorVal = frontier.stream().map(s -> s.vals[i]).max(Double::compareTo).orElse(1.0);
        if (minColorVal == maxColorVal) maxColorVal = minColorVal + 1;
        minColorValLabel.setText(formatter.format(minColorVal));
        maxColorValLabel.setText(formatter.format(maxColorVal));
    }

    private void addSolutionToChart(int i) {
        Solution s = frontier.get(i);
        points[i] = new XYChart.Series<>();
        points[i].getData().add(new XYChart.Data<>(
                s.vals[indexX],
                s.vals[indexY]));
        chart.getData().add(points[i]);

        Color c = getColor((s.vals[indexC] - minColorVal) / (maxColorVal - minColorVal));
        String col = toHex(c);

        if (s.isFeasible()) {
            points[i].getData().get(0).getNode().setStyle("-fx-stroke: "+col+"; -fx-background-color: "+col+";");
        }
        else {
            points[i].getData().get(0).getNode().setStyle(
                    "-fx-stroke: "+col+"; -fx-background-color: "+col+";"
                            + "-fx-shape: \"M5,0 L10,8 L0,8 Z\";"
            );
        }
    }

    private Color getColor(double t) {
        if (t < 0 || t > 1) {
            throw new IllegalArgumentException("t = " + t);
        }

        for (int i = 1; i < colorStops.size(); i++) {
            Stop s = colorStops.get(i);
            Stop p = colorStops.get(i-1);
            if (t <= s.getOffset()) {
                double start = p.getOffset();
                double end = s.getOffset();

                return p.getColor().interpolate(s.getColor(), (t-start) / (end-start));
            }
        }

        throw new IllegalArgumentException("t > last stop's offset: t="+t+", offset="+colorStops.get(colorStops.size()-1));
    }

    private static String toHex(Color color) {
        return String.format("#%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255)
        );
    }
}
