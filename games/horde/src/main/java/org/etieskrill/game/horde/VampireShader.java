package org.etieskrill.game.horde;

import org.etieskrill.engine.graphics.gl.shader.ShaderProgram;

import static org.etieskrill.engine.graphics.gl.shader.ShaderProgram.Uniform.Type.*;

public class VampireShader extends ShaderProgram {

    public static final int MAX_BONES = 100;

    @Override
    protected String[] getShaderFileNames() {
        return new String[]{"Vampire.glsl"};
    }

    @Override
    protected void getUniformLocations() {
        addUniformArray("boneMatrices", MAX_BONES, MAT4);

        addUniformArray("globalLights", 1, STRUCT);
        addUniformArray("lights", 2, STRUCT);

        addUniform("material.alpha", FLOAT, 1f);
    }

}