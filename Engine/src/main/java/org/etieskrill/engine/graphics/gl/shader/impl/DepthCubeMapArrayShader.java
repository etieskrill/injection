package org.etieskrill.engine.graphics.gl.shader.impl;

import io.etieskrill.injection.extension.shaderreflection.ReflectShader;
import org.etieskrill.engine.graphics.gl.shader.ShaderProgram;

import java.util.List;

@ReflectShader
public class DepthCubeMapArrayShader extends ShaderProgram {
    public DepthCubeMapArrayShader() {
        super(List.of("DepthCubeMapArray.vert", "DepthCubeMapArray.geom", "DepthCubeMapArray.frag"));
    }
}
