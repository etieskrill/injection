package org.etieskrill.game.shaderz;

import org.etieskrill.engine.graphics.gl.shaders.ShaderProgram;

public class HallwayShader extends ShaderProgram {

    @Override
    protected void init() {
    }

    @Override
    protected String[] getShaderFileNames() {
        return new String[]{"hallway.vert", "hallway.geom", "hallway.frag"};
    }

    @Override
    protected void getUniformLocations() {
        addUniform("uTime", Uniform.Type.FLOAT);
    }

}
