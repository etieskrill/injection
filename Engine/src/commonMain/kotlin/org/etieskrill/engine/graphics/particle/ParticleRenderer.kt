package org.etieskrill.engine.graphics.particle

import org.etieskrill.engine.graphics.GraphicsContext
import org.etieskrill.engine.graphics.camera.Camera
import org.etieskrill.engine.graphics.shader.Shader

interface ParticleRenderer {

    fun renderParticles(root: ParticleNode, camera: Camera, shader: Shader? = null)

    /**
     * @return the graphics context this renderer is bound to
     */
    val context: GraphicsContext

}
