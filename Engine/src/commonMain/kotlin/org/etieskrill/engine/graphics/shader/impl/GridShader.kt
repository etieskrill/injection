package org.etieskrill.engine.graphics.shader.impl

import io.github.etieskrill.injection.extension.shader.reflection.ReflectShader
import org.etieskrill.engine.graphics.shader.Shader

@ReflectShader
class GridShader : Shader(listOf("Grid.glsl")) {
    //TODO autodetect
//    @Override
//    protected void init() {
//        hasGeometryShader();
//    }
}
