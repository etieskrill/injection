package org.etieskrill.engine.graphics.particle;

import org.etieskrill.engine.Disposable;
import org.etieskrill.engine.graphics.camera.Camera;
import org.etieskrill.engine.graphics.gl.shader.ShaderProgram;
import org.jetbrains.annotations.Nullable;

public interface ParticleRenderer extends Disposable {

    default void renderParticles(ParticleNode root, Camera camera) {
        renderParticles(root, camera, null);
    }

    void renderParticles(ParticleNode root, Camera camera, @Nullable ShaderProgram shader);

}
