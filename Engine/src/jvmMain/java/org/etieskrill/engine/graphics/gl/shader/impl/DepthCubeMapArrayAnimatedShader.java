package org.etieskrill.engine.graphics.gl.shader.impl;

import io.github.etieskrill.injection.extension.shader.reflection.ReflectShader;
import org.etieskrill.engine.graphics.shader.Shader;

import java.util.List;

@ReflectShader
public class DepthCubeMapArrayAnimatedShader extends Shader {
    public DepthCubeMapArrayAnimatedShader() {
        super(List.of("DepthCubeMapArrayAnimated.glsl"));
    }
}
