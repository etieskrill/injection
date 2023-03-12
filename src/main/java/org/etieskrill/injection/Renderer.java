package org.etieskrill.injection;

import javafx.beans.property.SimpleStringProperty;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;

public class Renderer extends Thread {

    private final int fps = 60;
    private final long fpsMS = 1000 / fps;
    private final Canvas canvas;
    private final SimpleStringProperty labelFPSText;

    private final PhysicsContainer physicsContainer;

    public Renderer(Canvas canvas, PhysicsContainer physicsContainer) {
        this.canvas = canvas;
        this.physicsContainer = physicsContainer;
        this.labelFPSText = new SimpleStringProperty();
    }

    @Override
    public void run() {
        long start = System.nanoTime();
        double delta;
        long now;

        while (true) {
            delta = (System.nanoTime() - start) / 1000000000d;
            start = System.nanoTime();

            float updateTime = Math.max((float) delta, 0);
            //labelFPSText.setValue("FPS: " + 1f / updateTime);
            physicsContainer.update(updateTime / 1f);

            GraphicsContext g2d = canvas.getGraphicsContext2D();
            g2d.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
            g2d.setFill(Color.RED);
            final Vector2 circlePos = new Vector2(200f, 200f);
            final float circleRadius = 200f;
            g2d.strokeOval(circlePos.getX() - (circleRadius), circlePos.getY() - (circleRadius), 2f * circleRadius, 2f * circleRadius);
            for (Particle particle : physicsContainer.getParticles()) {
                Vector2 pos = particle.getPos();
                float radius = particle.getRadius();
                g2d.fillOval(pos.getX() - radius, pos.getY() - radius, 2 * radius, 2 * radius);
            }

            try {
                Thread.sleep((long) Math.max(((double) 2 * fpsMS) - Math.max((delta * 1000d), fpsMS), 0f)); //Sleep period dependent on last render cycle
            } catch (InterruptedException e) {
                System.err.println("Render thread could not sleep:\n" + e.getMessage());
            } catch (IllegalArgumentException ex) {
                System.err.println("Attempted to wait for a negative amount of time before next frame:\n" +
                        ex.getMessage());
            }
        }
    }

    public SimpleStringProperty getLabelFPSTextProperty() {
        return labelFPSText;
    }

}
