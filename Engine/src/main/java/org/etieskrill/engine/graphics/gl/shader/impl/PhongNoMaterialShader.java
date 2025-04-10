package org.etieskrill.engine.graphics.gl.shader.impl;

import io.github.etieskrill.injection.extension.shader.reflection.ReflectShader;
import org.etieskrill.engine.graphics.gl.shader.ShaderProgram;

import java.util.List;

@ReflectShader
public class PhongNoMaterialShader extends ShaderProgram {
    public PhongNoMaterialShader() {
        super(List.of("PhongNoMaterial.glsl"), false);
    }
}
