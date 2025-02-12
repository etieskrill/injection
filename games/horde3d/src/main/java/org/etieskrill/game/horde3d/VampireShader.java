package org.etieskrill.game.horde3d;

import org.etieskrill.engine.graphics.gl.shader.ShaderProgram;
import org.joml.Vector3f;

import java.util.List;

import static org.etieskrill.engine.graphics.gl.shader.ShaderProgram.Uniform.Type.*;

public class VampireShader extends ShaderProgram {

    public static final int MAX_BONES = 100;

    protected VampireShader() {
        super(List.of("Vampire.glsl"));
    }

    @Override
    protected void setUniformDefaults() {
        addUniformArray("boneMatrices", MAX_BONES, MAT4);

        addUniformArray("globalLights", 1, STRUCT);
        addUniformArray("lights", 2, STRUCT);

        addUniform("material._colour", VEC3, new Vector3f(1));
        addUniform("material.alpha", FLOAT, 1f);
    }

}
