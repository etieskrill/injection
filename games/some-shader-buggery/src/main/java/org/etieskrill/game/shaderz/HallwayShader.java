package org.etieskrill.game.shaderz;

import org.etieskrill.engine.graphics.gl.shaders.ShaderProgram;

import java.nio.file.Path;

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
