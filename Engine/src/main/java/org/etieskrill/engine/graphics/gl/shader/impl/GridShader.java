package org.etieskrill.engine.graphics.gl.shader.impl;

import io.github.etieskrill.injection.extension.shaderreflection.ReflectShader;
import org.etieskrill.engine.graphics.gl.shader.ShaderProgram;

import java.util.List;

@ReflectShader
public class GridShader extends ShaderProgram {
    public GridShader() {
        super(List.of("Grid.glsl"));
    }

    //TODO autodetect
//    @Override
//    protected void init() {
//        hasGeometryShader();
//    }

}
