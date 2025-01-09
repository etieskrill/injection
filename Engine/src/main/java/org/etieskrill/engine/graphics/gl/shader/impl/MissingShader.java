package org.etieskrill.engine.graphics.gl.shader.impl;

import org.etieskrill.engine.graphics.gl.shader.ShaderProgram;

public class MissingShader extends ShaderProgram {
    @Override
    protected String[] getShaderFileNames() {
        return new String[]{"MissingShader.glsl"};
    }

    @Override
    protected void getUniformLocations() {
        addUniform("mesh", ShaderProgram.Uniform.Type.MAT4);
        addUniform("model", ShaderProgram.Uniform.Type.MAT4);
        addUniform("combined", ShaderProgram.Uniform.Type.MAT4);
    }
}
