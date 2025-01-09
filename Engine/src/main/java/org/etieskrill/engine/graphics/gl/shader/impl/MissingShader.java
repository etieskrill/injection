package org.etieskrill.engine.graphics.gl.shader.impl;

import org.etieskrill.engine.graphics.gl.shader.ShaderProgram;

import java.util.List;

import static org.etieskrill.engine.graphics.gl.shader.ShaderProgram.Uniform.Type.MAT4;

public class MissingShader extends ShaderProgram {
    public MissingShader() {
        super(List.of("MissingShader.glsl"), List.of(
                uniform("mesh", MAT4),
                uniform("model", MAT4),
                uniform("combined", MAT4)
        ));
    }
}
