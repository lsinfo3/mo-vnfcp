package de.uniwue.VNFP.gui.graphUI;

import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.ScrollEvent;
import javafx.scene.transform.Scale;

public class ZoomableScrollPane extends ScrollPane {
    private Scale scaleTransform;
    private double scaleValue = 1.0;

    public ZoomableScrollPane(Node content) {
        Group contentGroup = new Group();
        Group zoomGroup = new Group();
        contentGroup.getChildren().add(zoomGroup);
        zoomGroup.getChildren().add(content);
        setContent(contentGroup);
        scaleTransform = new Scale(scaleValue, scaleValue, 0, 0);
        zoomGroup.getTransforms().add(scaleTransform);

        zoomGroup.setOnScroll(new ZoomHandler());
    }

    public double getScaleValue() {
        return scaleValue;
    }

    private void zoomTo(double scaleValue) {

        this.scaleValue = scaleValue;

        scaleTransform.setX(scaleValue);
        scaleTransform.setY(scaleValue);

    }
    private class ZoomHandler implements EventHandler<ScrollEvent> {

        @Override
        public void handle(ScrollEvent scrollEvent) {
            {

                double delta = 0.1;
                if (scrollEvent.getDeltaY() < 0) {
                    scaleValue -= delta;
                } else {
                    scaleValue += delta;
                }

                zoomTo(scaleValue);

                scrollEvent.consume();
            }
        }
    }
}
