package org.etieskrill.engine.graphics.shader.impl

import io.github.etieskrill.injection.extension.shader.reflection.ReflectShader
import org.etieskrill.engine.graphics.shader.Shader

@ReflectShader(files = ["ParticlePointVertex.glsl"])
class ParticleShader : Shader(listOf("shaders/ParticlePointVertex.glsl"))
