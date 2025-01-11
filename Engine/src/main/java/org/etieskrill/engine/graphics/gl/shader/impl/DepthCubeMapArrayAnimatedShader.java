package org.etieskrill.engine.graphics.gl.shader.impl;

import io.etieskrill.injection.extension.shaderreflection.ReflectShader;
import org.etieskrill.engine.graphics.gl.shader.ShaderProgram;

import java.util.List;

@ReflectShader
public class DepthCubeMapArrayAnimatedShader extends ShaderProgram {
    public DepthCubeMapArrayAnimatedShader() {
        super(List.of("DepthCubeMapArrayAnimated.glsl"));
    }
}
