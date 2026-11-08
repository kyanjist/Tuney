package com.ale.tuney;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Labeled;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.Region;
import javafx.scene.paint.Paint;
import javafx.stage.Stage;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;

import java.util.Map;
import java.util.HashMap;

public class ThemeManager {
    private static final Map<Stage, BoundsState> stageBoundsMap = new HashMap<>();

    private static class BoundsState {
        double x, y, width, height;
        boolean maximized;
    }

    public static boolean isWindowMaximized(Stage stage) {
        BoundsState state = stageBoundsMap.get(stage);
        return state != null && state.maximized;
    }

    public static void toggleMaximize(Stage stage) {
        BoundsState state = stageBoundsMap.computeIfAbsent(stage, k -> new BoundsState());
        if (state.maximized) {
            stage.setX(state.x);
            stage.setY(state.y);
            stage.setWidth(state.width);
            stage.setHeight(state.height);
            state.maximized = false;
        } else {
            state.x = stage.getX();
            state.y = stage.getY();
            state.width = stage.getWidth();
            state.height = stage.getHeight();

            Screen screen = Screen.getScreensForRectangle(
                state.x, state.y, state.width, state.height
            ).stream().findFirst().orElse(Screen.getPrimary());
            Rectangle2D bounds = screen.getVisualBounds();

            stage.setX(bounds.getMinX());
            stage.setY(bounds.getMinY());
            stage.setWidth(bounds.getWidth());
            stage.setHeight(bounds.getHeight());
            state.maximized = true;
        }
    }

    public static void applyTheme(Parent root) {
        if (root == null) return;
        applyThemeNode(root);
    }

    private static void applyThemeNode(Node node) {
        if (node == null) return;

        if (node instanceof Parent parent) {
            parent.getStyleClass().remove("light-theme");
        }

        if (node instanceof Region region) {
            if (region.getProperties().containsKey("originalStyle")) {
                region.setStyle((String) region.getProperties().get("originalStyle"));
            }
        }

        if (node instanceof Labeled labeled) {
            if (labeled.getProperties().containsKey("originalTextFill")) {
                labeled.setTextFill((Paint) labeled.getProperties().get("originalTextFill"));
            }
        }

        if (node instanceof TextInputControl textInput) {
            if (textInput.getProperties().containsKey("originalStyle")) {
                textInput.setStyle((String) textInput.getProperties().get("originalStyle"));
            }
        }

        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                applyThemeNode(child);
            }
        }
    }
}
