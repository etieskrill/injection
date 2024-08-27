package org.etieskrill.engine.graphics.gl.shader.impl;

import org.etieskrill.engine.graphics.gl.shader.ShaderProgram;

import static org.etieskrill.engine.graphics.gl.shader.ShaderProgram.Uniform.Type.*;

public class ParticleShader extends ShaderProgram {

    @Override
    protected void init() {
        hasGeometryShader();
    }

    @Override
    protected String[] getShaderFileNames() {
        return new String[]{"ParticlePointVertex.glsl"};
    }

    @Override
    protected void getUniformLocations() {
        addUniform("model", MAT4);
        addUniform("camera", STRUCT);
        addUniform("size", FLOAT);
        addUniform("sprite", SAMPLER2D);
    }

}
