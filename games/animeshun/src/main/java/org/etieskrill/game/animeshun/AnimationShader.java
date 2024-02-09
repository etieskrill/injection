package org.etieskrill.game.animeshun;

import org.etieskrill.engine.graphics.gl.shaders.Shaders;

import static org.etieskrill.engine.graphics.gl.shaders.ShaderProgram.Uniform.Type.STRUCT;

public class AnimationShader extends Shaders.StaticShader {

    private static final int MAX_BONES = 100;

    @Override
    protected void init() {
    }

    @Override
    protected String[] getShaderFileNames() {
        return new String[]{"Animation.vert", "Animation.frag"};
    }

    @Override
    protected void getUniformLocations() {
        addUniformArray("boneMatrices[$]", MAX_BONES, Uniform.Type.MAT4);
        addUniformArray("globalLights[$]", 1, STRUCT);
        addUniformArray("lights[$]", 2, STRUCT);
    }

}
