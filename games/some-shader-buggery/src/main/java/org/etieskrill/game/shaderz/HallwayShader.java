package org.etieskrill.game.shaderz;

import org.etieskrill.engine.graphics.shader.Shader;

public class HallwayShader extends Shader {

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
        addUniform("time", Uniform.Type.FLOAT);
    }

}
