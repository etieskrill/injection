package org.etieskrill.engine.graphics;

import glm_.mat4x4.Mat4;
import glm_.vec2.Vec2;
import glm_.vec3.Vec3;
import glm_.vec4.Vec4;
import org.etieskrill.engine.graphics.gl.Renderer;
import org.etieskrill.engine.graphics.gl.shaders.ShaderProgram;
import org.etieskrill.engine.graphics.gl.shaders.Shaders;
import org.etieskrill.engine.graphics.texture.font.Font;
import org.jetbrains.annotations.NotNull;

public class Batch {

    private final Renderer renderer;

    //TODO declare this as a pure ui rendering batch (or make subclass) and hardcode shaders here, with a colour stack n stuff
    private ShaderProgram shader;

    private final Mat4 combined;

    public Batch(@NotNull Renderer renderer) {
        this.renderer = renderer;
        this.shader = Shaders.getTextureShader();
        this.combined = new Mat4(1f);
    }

    private static final Vec4 resetColour = new Vec4(1);

    public void render(Vec3 position, Vec3 size, Vec4 colour) {
        shader.setUniform("uColour", colour, false);
        renderer.renderBox(position, size, shader, combined);
        shader.setUniform("uColour", resetColour, false);
    }

    public void render(String text, Font font, Vec2 position) {
        renderer.render(text, font, position, shader, combined);
    }

    public Batch setCombined(Mat4 mat) {
        this.combined.put(mat);
        return this;
    }

    public ShaderProgram getShader() {
        return shader;
    }

    public Batch setShader(ShaderProgram shader) {
        this.shader = shader;
        return this;
    }

}
