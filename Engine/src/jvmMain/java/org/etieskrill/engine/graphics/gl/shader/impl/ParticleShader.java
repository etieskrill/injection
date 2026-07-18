package org.etieskrill.engine.graphics.gl.shader.impl;

import io.github.etieskrill.injection.extension.shader.reflection.ReflectShader;
import org.etieskrill.engine.graphics.shader.Shader;

import java.util.List;

@ReflectShader(files = "ParticlePointVertex.glsl")
public class ParticleShader extends Shader {
    public ParticleShader() {
        super(List.of("ParticlePointVertex.glsl"));
    }
}
