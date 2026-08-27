package org.etieskrill.engine.graphics.shader.impl

import io.github.etieskrill.injection.extension.shader.reflection.ReflectShader
import org.etieskrill.engine.graphics.shader.Shader

@ReflectShader
class LightSourceShader : Shader(listOf("LightSource.vert", "LightSource.frag"), false)
