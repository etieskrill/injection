package org.etieskrill.engine.graphics.shader.impl

import io.github.etieskrill.injection.extension.shader.reflection.ReflectShader
import org.etieskrill.engine.graphics.shader.Shader

@ReflectShader
class AnimationShader : Shader(listOf("Animation.vert", "Animation.frag")) {
//    @Override
//    protected void setUniformDefaults() {
//        AnimationShaderKt.setShowBoneSelector(this, 4);
//        AnimationShaderKt.setShowBoneWeights(this, false);
//    }
}
