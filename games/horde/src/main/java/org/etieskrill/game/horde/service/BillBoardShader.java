package org.etieskrill.game.horde.service;

import io.github.etieskrill.injection.extension.shader.reflection.ReflectShader;
import org.etieskrill.engine.graphics.gl.shader.ShaderProgram;

import java.util.List;

@ReflectShader
public class BillBoardShader extends ShaderProgram {
    protected BillBoardShader() {
        super(List.of("BillBoard.glsl"));
    }
}
