package org.etieskrill.game.horde;

import org.etieskrill.engine.graphics.gl.shader.ShaderProgram;

import static org.etieskrill.engine.graphics.gl.shader.ShaderProgram.Uniform.Type.MAT4;
import static org.etieskrill.engine.graphics.gl.shader.ShaderProgram.Uniform.Type.STRUCT;

public class ZombieShader extends ShaderProgram {

    public static final int MAX_BONES = 100;

    @Override
    protected void init() {
        disableStrictUniformChecking();
    }

    @Override
    protected String[] getShaderFileNames() {
        return new String[]{"Zombie.glsl"};
    }

    @Override
    protected void getUniformLocations() {
        addUniformArray("boneMatrices", MAX_BONES, MAT4);

        addUniformArray("globalLights", 1, STRUCT);
        addUniformArray("lights", 2, STRUCT);
    }

}
