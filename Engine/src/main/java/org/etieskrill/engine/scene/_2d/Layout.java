package org.etieskrill.engine.scene._2d;

import org.etieskrill.engine.math.Vec2f;

public class Layout {
    
    private Vec2f minSize, preferredSize, maxSize;
    private float padTop, padBottom, padLeft, padRight;
    private Alignment alignment;
    private Scaling scaling;
    
    public static Layout get() {
        return new Layout(new Vec2f(), new Vec2f(), new Vec2f(), Alignment.CENTER, Scaling.PRESERVE_RATIO);
    }
    
    private Layout(Vec2f minSize, Vec2f preferredSize, Vec2f maxSize, Alignment alignment, Scaling scaling) {
        this.minSize = minSize;
        this.preferredSize = preferredSize;
        this.maxSize = maxSize;
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
    
    public void setAlignment(Alignment alignment) {
        this.alignment = alignment;
    }
    
}
