package org.etieskrill.game.horde3d;

import org.etieskrill.engine.graphics.shader.Shader;

import java.util.List;

import static org.etieskrill.engine.graphics.shader.Shader.Uniform.Type.MAT4;
import static org.etieskrill.engine.graphics.shader.Shader.Uniform.Type.STRUCT;

public class ZombieShader extends Shader {

    public static final int MAX_BONES = 100;

    protected ZombieShader() {
        super(List.of("Zombie.glsl"), false);
    }

    @Override
    protected void setUniformDefaults() {
        addUniformArray("boneMatrices", MAX_BONES, MAT4);

        addUniformArray("globalLights", 1, STRUCT);
        addUniformArray("lights", 2, STRUCT);
    }

}
