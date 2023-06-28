package org.etieskrill.engine.scene._2d;

import org.etieskrill.engine.math.Vec2f;

public class Layout {
    
    private final Vec2f minSize, prefSize;
    private float padTop, padBottom, padLeft, padRight;
    private Alignment alignment;
    private Scaling scaling;
    
    public static Layout get() {
        return new Layout(new Vec2f(0f), new Vec2f(), Alignment.CENTER, Scaling.PRESERVE_RATIO);
    }

    public static Layout copy(Layout layout) {
        return new Layout(layout.getMinSize(), layout.getPrefSize(), layout.getAlignment(), layout.getScaling());
    }

    public Layout copy() {
        return copy(this);
    }
    
    private Layout(Vec2f minSize, Vec2f prefSize, Alignment alignment, Scaling scaling) {
        this.minSize = minSize;
        this.prefSize = prefSize;
        this.alignment = alignment;
        this.scaling = scaling;
    }
    
    public enum Alignment {
        TOP_LEFT,
        TOP_CENTER,
        TOP_RIGHT,
        CENTER_LEFT,
        CENTER,
        CENTER_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_CENTER,
        BOTTOM_RIGHT
    }
    
    public enum Scaling {
        PRESERVE_RATIO,
        PAD,
        STRETCH
    }
    
    public Alignment getAlignment() {
        return alignment;
    }
    
    public Layout setAlignment(Alignment alignment) {
        this.alignment = alignment;
        return this;
    }

    public Scaling getScaling() {
        return scaling;
    }

    public Layout setScaling(Scaling scaling) {
        this.scaling = scaling;
        return this;
    }

    public Vec2f getMinSize() {
        return minSize;
    }

    public Layout setMinSize(Vec2f minSize) {
        this.minSize.set(minSize);
        return this;
    }

    public Vec2f getPrefSize() {
        return prefSize;
    }

    public Layout setPrefSize(Vec2f prefSize) {
        this.prefSize.set(prefSize);
        return this;
    }

    public Layout pad(float top, float bottom, float left, float right) {
        return padTop(top).padBottom(bottom).padLeft(left).padRight(right);
    }

    public Layout pad(float vertical, float horizontal) {
        return padTop(vertical).padBottom(vertical).padLeft(horizontal).padRight(horizontal);
    }

    public Layout padTop(float padTop) {
        this.padTop = padTop;
        return this;
    }

    public Layout padBottom(float padBottom) {
        this.padBottom = padBottom;
        return this;
    }

    public Layout padLeft(float padLeft) {
        this.padLeft = padLeft;
        return this;
    }

    public Layout padRight(float padRight) {
        this.padRight = padRight;
        return this;
    }

}
