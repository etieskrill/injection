package org.etieskrill.engine.graphics.gl.shader.impl;

import io.github.etieskrill.injection.extension.shader.reflection.ReflectShader;
import org.etieskrill.engine.graphics.shader.Shader;

import java.util.List;

import static org.etieskrill.engine.graphics.shader.Shader.Uniform.Type.VEC4;

@ReflectShader
public class SingleColourShader extends Shader {
    public SingleColourShader() {
        super(List.of("SingleColour.vert", "SingleColour.frag"), List.of(uniform("colour", VEC4)));
    }
}
