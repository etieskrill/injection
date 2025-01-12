package org.etieskrill.engine.graphics.gl.shader.impl;

import io.github.etieskrill.injection.extension.shaderreflection.ReflectShader;
import org.etieskrill.engine.graphics.gl.shader.ShaderProgram;

import java.util.List;

import static org.etieskrill.engine.graphics.gl.shader.ShaderProgram.Uniform.Type.VEC4;

@ReflectShader
public class SingleColourShader extends ShaderProgram {
    public SingleColourShader() {
        super(List.of("SingleColour.vert", "SingleColour.frag"), List.of(uniform("colour", VEC4)));
    }
}
