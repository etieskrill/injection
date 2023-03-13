package org.etieskrill.injection;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        Canvas canvas = new Canvas(800, 500);
        BorderPane root = new BorderPane(canvas);
        Label labelFPS = new Label();
        root.setTop(labelFPS);
        stage.setScene(new Scene(root, 800, 500));
        Renderer renderer = new Renderer(canvas, new PhysicsContainer(500));
        renderer.start();

        labelFPS.textProperty().bindBidirectional(renderer.getLabelFPSTextProperty());

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
