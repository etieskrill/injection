package org.etieskrill.engine.graphics.gl.shader.impl;

import io.github.etieskrill.injection.extension.shader.reflection.ReflectShader;
import org.etieskrill.engine.graphics.shader.Shader;

import java.util.List;

@ReflectShader
public class GridShader extends Shader {
    public GridShader() {
        super(List.of("Grid.glsl"));
    }

    //TODO autodetect
//    @Override
//    protected void init() {
//        hasGeometryShader();
//    }

}
