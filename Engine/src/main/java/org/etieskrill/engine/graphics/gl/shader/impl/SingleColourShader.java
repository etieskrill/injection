package org.etieskrill.engine.graphics.gl.shader.impl;

import io.etieskrill.injection.extension.shaderreflection.ReflectShader;
import org.etieskrill.engine.graphics.gl.shader.ShaderProgram;

import static org.etieskrill.engine.graphics.gl.shader.ShaderProgram.Uniform.Type.VEC4;

@ReflectShader
public class SingleColourShader extends ShaderProgram {
    @Override
    protected String[] getShaderFileNames() {
        return new String[]{"SingleColour.vert", "SingleColour.frag"};
    }

    @Override
    protected void getUniformLocations() {
        //TODO i've forgotten about this thing waaaay to often, either enforce via enums soon or rework this goddamned system
        addUniform("colour", VEC4);
    }
}
