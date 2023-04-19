package org.etieskrill.injection;

import javafx.beans.property.SimpleStringProperty;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import org.etieskrill.injection.math.Vector2;
import org.etieskrill.injection.particle.Particle;

public class Renderer extends Thread {

    private final int fps = 60;
    private final long fpsMS = 1000 / fps;
    private final Canvas canvas;

    private final PhysicsContainer physicsContainer;

    public Renderer(Canvas canvas, PhysicsContainer physicsContainer) {
        this.canvas = canvas;
        this.physicsContainer = physicsContainer;
    }

    @Override
    public void run() {
        long start = System.nanoTime();
        double delta;
        long sleepTime = 0;
        int printInfo = 0;

        while (true) {
            delta = (System.nanoTime() - start) / 1000000000d;
            start = System.nanoTime();

            float updateTime = Math.max((float) delta, 0);
            physicsContainer.update(updateTime);

            long systemPhysicsTime = System.nanoTime();
            double physicsTime = (systemPhysicsTime - start) / 1000000000d;

            canvas.setTranslateX(-App.windowX.get());
            canvas.setTranslateY(-App.windowY.get());
            GraphicsContext g2d = canvas.getGraphicsContext2D();
            g2d.setFill(Color.BLACK);
            g2d.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
            g2d.setStroke(Color.DIMGRAY);
            g2d.setFill(Color.LIGHTGRAY);
            for (Particle particle : physicsContainer.getParticles()) {
                Vector2 pos = particle.getPos();
                float radius = particle.getRadius();
                //g2d.setFill(new Color(particle.getTemp(), 0.1f, 0.1f, 1f));
                //g2d.setFill(new Color(Math.max(particle.getTemp() * 1.2f - 0.2f, 0f), 0f, 0f, 1f));
                g2d.fillOval(pos.getX() - radius, pos.getY() - radius, 2 * radius, 2 * radius);
            }

            long systemRenderTime = System.nanoTime();
            double renderTime = (systemRenderTime - systemPhysicsTime) / 1000000000d;
    
            if (printInfo++ > 60) {
                System.out.printf("Delta: %f, physics: %f, render: %f, sleep: %f\n", delta, physicsTime, renderTime, sleepTime / 1000f);
                printInfo = 0;
            }

            try {
                sleepTime = (long) Math.max(((double) 2 * fpsMS) - Math.max((delta * 1000d), fpsMS), 0f);
                Thread.sleep(sleepTime); //Sleep period dependent on last render cycle
            } catch (InterruptedException e) {
                System.err.println("Render thread could not sleep:\n" + e.getMessage());
            } catch (IllegalArgumentException ex) {
                System.err.println("Attempted to wait for a negative amount of time before next frame:\n" +
                        ex.getMessage());
            }
        }
    }

}
