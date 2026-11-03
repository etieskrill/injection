package org.etieskrill.engine.graphics.shader.impl

import io.github.etieskrill.injection.extension.shader.reflection.ReflectShader
import org.etieskrill.engine.graphics.shader.Shader

@ReflectShader
class AnimationShader : Shader(listOf("shaders/Animation.vert", "shaders/Animation.frag")) {
    init {
        showBoneSelector = 4
        showBoneWeights = false
    }
}
