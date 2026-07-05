package org.etieskrill.engine.graphics.gl.framebuffer

import org.etieskrill.engine.graphics.data.PointLight
import org.etieskrill.engine.graphics.texture.CubeMapTexture
import org.joml.Math.toRadians
import org.joml.Matrix4f
import org.joml.Vector2ic
import org.joml.Vector3f
import org.joml.plus

private val CUBE_FACE_NORMALS = arrayOf(
    Vector3f(1f, 0f, 0f), Vector3f(-1f, 0f, 0f), Vector3f(0f, 1f, 0f),
    Vector3f(0f, -1f, 0f), Vector3f(0f, 0f, 1f), Vector3f(0f, 0f, -1f)
)

private val CUBE_FACE_UPS = arrayOf(
    Vector3f(0f, -1f, 0f), Vector3f(0f, -1f, 0f), Vector3f(0f, 0f, 1f),
    Vector3f(0f, 0f, -1f), Vector3f(0f, -1f, 0f), Vector3f(0f, -1f, 0f)
)

fun getCombinedMatrices(
    size: Vector2ic,
    near: Float,
    far: Float,
    light: PointLight,
    targets: Array<Matrix4f>
): Array<Matrix4f> {
    val projection = Matrix4f().setPerspective(
        toRadians(90f), size.x().toFloat() / size.y().toFloat(), near, far
    )
    for (i in 0..<CubeMapTexture.NUM_SIDES) {
        targets[i].lookAt(light.position, light.position + CUBE_FACE_NORMALS[i], CUBE_FACE_UPS[i])
        projection.mul(targets[i], targets[i])
    }

    return targets
}
