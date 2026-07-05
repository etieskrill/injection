package org.etieskrill.engine.graphics.particle

import org.etieskrill.engine.common.Disposable
import org.etieskrill.engine.graphics.camera.Camera
import org.etieskrill.engine.graphics.gl.shader.ShaderProgram

interface ParticleRenderer : Disposable {

    fun renderParticles(root: ParticleNode, camera: Camera, shader: ShaderProgram? = null)

}
