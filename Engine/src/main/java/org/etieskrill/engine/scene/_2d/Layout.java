package org.etieskrill.engine.scene._2d;

import glm_.vec2.Vec2;

public class Layout {
    
    private final Vec2 minSize, prefSize;
    private float marginTop, marginBottom, marginLeft, marginRight;
    private Alignment alignment;
    private Scaling scaling;
    
    public static Layout get() {
        return new Layout(new Vec2(-1f), new Vec2(-1f), Alignment.CENTER, Scaling.PRESERVE_RATIO);
    }

    public static Layout copy(Layout layout) {
        return new Layout(layout.getMinSize(), layout.getPrefSize(), layout.getAlignment(), layout.getScaling());
    }

    public Layout copy() {
        return copy(this);
    }
    
    private Layout(Vec2 minSize, Vec2 prefSize, Alignment alignment, Scaling scaling) {
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
    
    public Vec2 computeSpan() {
        float spanX = Math.max(prefSize.getX() - minSize.getX(), 0f);
        float spanY = Math.max(prefSize.getY() - minSize.getY(), 0f);
        return new Vec2(spanX, spanY);
    }
    
    public Vec2 getSize(Vec2 border) {
        float sizeX = 0f;
        if (border.getX() - minSize.getX() < 0f || prefSize.getX() - minSize.getX() < 0f)
            sizeX = minSize.getX();
        else if (border.getX() - prefSize.getX() < 0f)
            sizeX = border.getX();
        else
            sizeX = prefSize.getX();

        float sizeY = 0f;
        if (border.getY() - minSize.getY() < 0f || prefSize.getY() - minSize.getY() < 0f)
            sizeY = minSize.getY();
        else if (border.getY() - prefSize.getY() < 0f)
            sizeY = border.getY();
        else
            sizeY = prefSize.getY();

        return new Vec2(sizeX, sizeY);
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
    
    Vec2 getMinSize() {
        return minSize;
    }

    public Layout setMinSize(Vec2 minSize) {
        this.minSize.put(minSize);
        return this;
    }
    
    Vec2 getPrefSize() {
        return prefSize;
    }

    public Layout setPrefSize(Vec2 prefSize) {
        this.prefSize.put(prefSize);
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
    
    public boolean caresAboutSize() {
        return minSize.getX() >= 0f || prefSize.getX() >= 0f
                || minSize.getY() >= 0f || prefSize.getY() >= 0f;
    }

}
