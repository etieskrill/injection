package org.etieskrill.engine.scene._2d;

import org.etieskrill.engine.math.Vec2f;

public class Layout {
    
    private final Vec2f minSize, prefSize;
    private float marginTop, marginBottom, marginLeft, marginRight;
    private Alignment alignment;
    private Scaling scaling;
    
    public static Layout get() {
        return new Layout(new Vec2f(-1f), new Vec2f(-1f), Alignment.CENTER, Scaling.PRESERVE_RATIO);
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
    
    public Vec2f computeSpan() {
        float spanX = Math.max(prefSize.getX() - minSize.getX(), 0f);
        float spanY = Math.max(prefSize.getY() - minSize.getY(), 0f);
        return new Vec2f(spanX, spanY);
    }
    
    public Vec2f getSize(Vec2f border) {
        float sizeX = 0f; //=  ? prefSize.getX() : ;
        if (prefSize.getX() - minSize.getX() < 0f) {
            if (prefSize.getX() == 0f) sizeX = prefSize.getX();
            else sizeX = prefSize.getX();
        }
        
        return null;
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

    Vec2f getMinSize() {
        return minSize;
    }

    public Layout setMinSize(Vec2f minSize) {
        this.minSize.set(minSize);
        return this;
    }

    Vec2f getPrefSize() {
        return prefSize;
    }

    public Layout setPrefSize(Vec2f prefSize) {
        this.prefSize.set(prefSize);
        return this;
    }

    public Layout pad(float top, float bottom, float left, float right) {
        return marginTop(top).marginBottom(bottom).marginLeft(left).marginRight(right);
    }

    public Layout pad(float vertical, float horizontal) {
        return marginTop(vertical).marginBottom(vertical).marginLeft(horizontal).marginRight(horizontal);
    }

    public Layout marginTop(float marginTop) {
        this.marginTop = marginTop;
        return this;
    }

    public Layout marginBottom(float marginBottom) {
        this.marginBottom = marginBottom;
        return this;
    }

    public Layout marginLeft(float marginLeft) {
        this.marginLeft = marginLeft;
        return this;
    }

    public Layout marginRight(float marginRight) {
        this.marginRight = marginRight;
        return this;
    }

}
