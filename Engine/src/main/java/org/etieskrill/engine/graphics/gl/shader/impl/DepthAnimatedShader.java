package org.etieskrill.engine.graphics.gl.shader.impl;

import io.github.etieskrill.injection.extension.shaderreflection.ReflectShader;
import org.etieskrill.engine.graphics.gl.shader.ShaderProgram;

import java.util.List;

@ReflectShader
public class DepthAnimatedShader extends ShaderProgram {
    public DepthAnimatedShader() {
        super(List.of("DepthAnimated.glsl"));
    }
}
