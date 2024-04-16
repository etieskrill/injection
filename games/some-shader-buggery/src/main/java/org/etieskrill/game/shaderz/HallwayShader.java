package org.etieskrill.game.shaderz;

import org.etieskrill.engine.graphics.gl.shader.ShaderProgram;

public class HallwayShader extends ShaderProgram {

    @Override
    protected void init() {
        disableStrictUniformChecking();
    }

    @Override
    protected String[] getShaderFileNames() {
        return new String[]{"hallway.vert", "hallway.frag"};
    }

    @Override
    protected void getUniformLocations() {
        addUniform("uTime", Uniform.Type.FLOAT);
    }

}
