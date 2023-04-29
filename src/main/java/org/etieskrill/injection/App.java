package org.etieskrill.injection;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.etieskrill.injection.math.Vector2f;

public class App extends Application {

    public static final Vector2f windowSize = new Vector2f(800f, 600f);
    public static ReadOnlyDoubleProperty windowX;
    public static ReadOnlyDoubleProperty windowY;

    @Override
    public void start(Stage stage) throws Exception {
        windowX = stage.xProperty();
        windowY = stage.yProperty();
        stage.setX(0);
        stage.setY(0);
        Canvas canvas = new Canvas(2.5d * windowSize.getX(), 2.5d * windowSize.getY());
        BorderPane root = new BorderPane(canvas);
        //Label labelFPS = new Label();
        //root.setTop(labelFPS);
        Scene scene = new Scene(root, windowSize.getX(), windowSize.getY());
        scene.setFill(Color.BLACK);
        stage.setScene(scene);
        Renderer renderer = new Renderer(canvas, new PhysicsContainer());
        renderer.start();

        //labelFPS.textProperty().bindBidirectional(renderer.getLabelFPSTextProperty());

        stage.setOnCloseRequest((v) -> {
            Platform.exit();
            System.exit(0);
        });
        stage.show();
    }

    public static void start(String[] args) {
        launch(args);
    }

}
