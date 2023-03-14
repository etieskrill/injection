package org.etieskrill.injection;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.etieskrill.injection.math.Vector2;

public class App extends Application {

    protected static final Vector2 windowSize = new Vector2(1200f, 800f);

    @Override
    public void start(Stage stage) throws Exception {
        Canvas canvas = new Canvas(windowSize.getX(), windowSize.getY());
        BorderPane root = new BorderPane(canvas);
        //Label labelFPS = new Label();
        //root.setTop(labelFPS);
        stage.setScene(new Scene(root, windowSize.getX(), windowSize.getY()));
        Renderer renderer = new Renderer(canvas, new PhysicsContainer());
        renderer.start();

        //labelFPS.textProperty().bindBidirectional(renderer.getLabelFPSTextProperty());

        stage.setOnCloseRequest((v) -> {
            Platform.exit();
            System.exit(0);
        });
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

}
