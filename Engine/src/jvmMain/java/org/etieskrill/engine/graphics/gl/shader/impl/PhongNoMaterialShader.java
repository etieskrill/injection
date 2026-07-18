package org.etieskrill.engine.graphics.gl.shader.impl;

import io.github.etieskrill.injection.extension.shader.reflection.ReflectShader;
import org.etieskrill.engine.graphics.shader.Shader;

import java.util.List;

@ReflectShader
public class PhongNoMaterialShader extends Shader {
    public PhongNoMaterialShader() {
        super(List.of("PhongNoMaterial.glsl"), false);
    }
}
