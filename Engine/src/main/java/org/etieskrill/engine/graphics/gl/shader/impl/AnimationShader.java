package org.etieskrill.engine.graphics.gl.shader.impl;

import io.github.etieskrill.injection.extension.shaderreflection.ReflectShader;
import org.etieskrill.engine.graphics.gl.shader.ShaderProgram;

import java.util.List;

@ReflectShader
public class AnimationShader extends ShaderProgram {
    public AnimationShader() {
        super(List.of("Animation.vert", "Animation.frag"));
    }

    @Override
    protected void setUniformDefaults() {
        AnimationShaderKt.setShowBoneSelector(this, 4);
        AnimationShaderKt.setShowBoneWeights(this, false);
    }
}
