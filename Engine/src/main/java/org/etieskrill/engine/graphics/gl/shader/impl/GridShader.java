package org.etieskrill.engine.graphics.gl.shader.impl;

import io.etieskrill.injection.extension.shaderreflection.ReflectShader;
import org.etieskrill.engine.graphics.gl.shader.ShaderProgram;

@ReflectShader
public class GridShader extends ShaderProgram {
    @Override
    protected void init() {
        hasGeometryShader();
    }

    @Override
    protected String[] getShaderFileNames() {
        return new String[]{"Grid.glsl"};
    }

    @Override
    protected void getUniformLocations() {
        addUniform("position", Uniform.Type.VEC3);
        addUniform("camera", Uniform.Type.STRUCT);
    }
}
