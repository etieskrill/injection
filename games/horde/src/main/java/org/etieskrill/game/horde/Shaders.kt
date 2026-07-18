package org.etieskrill.game.horde

import io.github.etieskrill.injection.extension.shader.reflection.ReflectShader
import org.etieskrill.engine.graphics.shader.Shader

@ReflectShader
class FloorShader : Shader(listOf("Floor.glsl"))

@ReflectShader
class BlitShader : Shader(listOf("Blit.glsl"))
