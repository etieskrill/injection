package org.etieskrill.engine.graphics.gl.shader.impl;

import io.github.etieskrill.injection.extension.shader.reflection.ReflectShader;
import org.etieskrill.engine.graphics.gl.shader.ShaderProgram;

import java.util.List;

@ReflectShader
public class DepthCubeMapArrayShader extends ShaderProgram {
    public DepthCubeMapArrayShader() {
        super(List.of("DepthCubeMapArray.vert", "DepthCubeMapArray.geom", "DepthCubeMapArray.frag"));
    }
}
