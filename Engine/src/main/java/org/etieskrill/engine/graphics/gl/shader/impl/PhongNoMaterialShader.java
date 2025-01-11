package org.etieskrill.engine.graphics.gl.shader.impl;

import io.etieskrill.injection.extension.shaderreflection.ReflectShader;
import org.etieskrill.engine.graphics.gl.shader.ShaderProgram;

import java.util.List;

@ReflectShader
public class PhongNoMaterialShader extends ShaderProgram {
    public PhongNoMaterialShader() {
        super(List.of("PhongNoMaterial.glsl"));
    }
}
