package org.etieskrill.engine.entity.component;

import org.etieskrill.engine.graphics.gl.shader.ShaderProgram;
import org.etieskrill.engine.graphics.model.Model;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;

public class Drawable {

    private final Model model;
    private boolean visible;
    private final Vector2f textureScale;
    private boolean drawWireframe;
    private @Nullable ShaderProgram shader;
    private boolean drawOutline;
    private float outlineWidth;

    //TODO some kind of grouping mechanism

    public Drawable(Model model) {
        this(model, null);
    }

    public Drawable(Model model, @Nullable ShaderProgram shader) {
        this.model = model;
        this.visible = true;
        this.textureScale = new Vector2f(1);
        this.drawWireframe = false;
        this.shader = shader;
        this.drawOutline = false;
        this.outlineWidth = 0.05f;
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

    public boolean isDrawWireframe() {
        return drawWireframe;
    }

    public void setDrawWireframe(boolean drawWireframe) {
        this.drawWireframe = drawWireframe;
    }

    public @Nullable ShaderProgram getShader() {
        return shader;
    }

    public void setShader(@Nullable ShaderProgram shader) {
        this.shader = shader;
    }

    public boolean isDrawOutline() {
        return drawOutline;
    }

    public void setDrawOutline(boolean drawOutline) {
        this.drawOutline = drawOutline;
    }

    public float getOutlineWidth() {
        return outlineWidth;
    }

    public void setOutlineWidth(float outlineWidth) {
        this.outlineWidth = outlineWidth;
    }

}
