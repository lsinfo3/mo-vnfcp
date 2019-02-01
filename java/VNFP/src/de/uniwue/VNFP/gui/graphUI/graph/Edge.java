package de.uniwue.VNFP.gui.graphUI.graph;


import de.uniwue.VNFP.gui.graphUI.cell.Cell;
import de.uniwue.VNFP.gui.graphUI.cell.CellType;
import javafx.scene.Group;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;

public class Edge extends Group {

    private Text tooltip;

    private Line line;

    Edge(Cell source, Cell target, String remaining) {
        this.getStyleClass().add("edges");

        source.addCellChild(target);
        target.addCellParent(source);
        tooltip = new Text();
        tooltip.setText(remaining);

        line = new Line();
        if (source.getType() == CellType.CIRCLE) {
            line.startXProperty().bind(source.layoutXProperty().add(source.getCenterX() / 2.0));
            line.startYProperty().bind(source.layoutYProperty().add(source.getCenterY() / 2.0));
        } else {
            line.startXProperty().bind(source.layoutXProperty().add(source.getBoundsInParent().getWidth() / 2.0));
            line.startYProperty().bind(source.layoutYProperty().add(source.getBoundsInParent().getHeight() / 2.0));
        }
        if (target.getType() == CellType.CIRCLE) {
            line.endXProperty().bind(target.layoutXProperty().add(target.getCenterX() / 2.0));
            line.endYProperty().bind(target.layoutYProperty().add(target.getCenterY() / 2.0));
        } else {
            line.endXProperty().bind(target.layoutXProperty().add(target.getBoundsInParent().getWidth() / 2.0));
            line.endYProperty().bind(target.layoutYProperty().add(target.getBoundsInParent().getHeight() / 2.0));
        }
        tooltip.xProperty().bind(line.layoutXProperty().add(line.endXProperty().add(line.startXProperty())).divide(2));
        tooltip.yProperty().bind(line.layoutYProperty().add(line.endYProperty().add(line.startYProperty())).divide(2));
        getChildren().addAll(line,tooltip);
        tooltip.setOpacity(0);
    }
    public Text getTooltip() {
        return tooltip;
    }
}
