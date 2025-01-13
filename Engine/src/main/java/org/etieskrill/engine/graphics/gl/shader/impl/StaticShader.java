package org.etieskrill.engine.graphics.gl.shader.impl;

import io.github.etieskrill.injection.extension.shaderreflection.ReflectShader;
import org.etieskrill.engine.graphics.gl.shader.ShaderProgram;
import org.joml.Vector2f;

import java.util.List;

import static org.etieskrill.engine.graphics.gl.shader.impl.StaticShaderKt.*;

@ReflectShader(files = {"Phong.vert", "Phong.frag"}) //TODO also extract superclass, supress with flag
public class StaticShader extends ShaderProgram {
    public StaticShader() {
        super(List.of("Phong.vert", "Phong.frag"));

        setTextureScale(this, new Vector2f(1f));

        setBlinnPhong(this, true);

        setHasShadowMap(this, false);
        setHasPointShadowMaps(this, false);

        setPointShadowFarPlane(this, 20f);
    }
}
