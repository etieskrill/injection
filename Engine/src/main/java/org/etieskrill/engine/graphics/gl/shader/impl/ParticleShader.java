package org.etieskrill.engine.graphics.gl.shader.impl;

import io.etieskrill.injection.extension.shaderreflection.ReflectShader;
import org.etieskrill.engine.graphics.gl.shader.ShaderProgram;

import java.util.List;

import static org.etieskrill.engine.graphics.gl.shader.ShaderProgram.Uniform.Type.*;

@ReflectShader
public class ParticleShader extends ShaderProgram {
    public ParticleShader() {
        super(List.of("ParticlePointVertex.glsl"));
    }
}
