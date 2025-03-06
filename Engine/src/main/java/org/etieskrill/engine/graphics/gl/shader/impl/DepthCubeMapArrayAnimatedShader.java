package org.etieskrill.engine.graphics.gl.shader.impl;

import io.github.etieskrill.injection.extension.shader.reflection.ReflectShader;
import org.etieskrill.engine.graphics.gl.shader.ShaderProgram;

import java.util.List;

@ReflectShader
public class DepthCubeMapArrayAnimatedShader extends ShaderProgram {
    public DepthCubeMapArrayAnimatedShader() {
        super(List.of("DepthCubeMapArrayAnimated.glsl"));
    }
}
