package org.etieskrill.game.horde.service;

import io.github.etieskrill.injection.extension.shader.reflection.ReflectShader;
import org.etieskrill.engine.graphics.gl.shader.ShaderProgram;

import java.util.List;

@ReflectShader
public class AnimatedBillBoardShader extends ShaderProgram {
    protected AnimatedBillBoardShader() {
        super(List.of("AnimatedBillBoard.glsl"));
    }
}
