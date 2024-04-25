package org.etieskrill.engine.entity.component;

import org.etieskrill.engine.graphics.model.Model;
import org.joml.Vector2f;

public class Drawable {

    private final Model model;
    private boolean visible;
    private final Vector2f textureScale;

    //TODO some kind of grouping mechanism

    public Drawable(Model model) {
        this.model = model;
        this.visible = true;
        this.textureScale = new Vector2f(1);
    }

    public Model getModel() {
        return model;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public Vector2f getTextureScale() {
        return textureScale;
    }

    public void setTextureScale(Vector2f textureScale) {
        this.textureScale.set(textureScale);
    }

}
