package org.etieskrill.engine.graphics.framebuffer

import net.bifreus.kml.Mat4
import net.bifreus.kml.Vec2ic
import net.bifreus.kml.Vec3
import net.bifreus.kml.deg
import org.etieskrill.engine.graphics.data.PointLight
import org.etieskrill.engine.graphics.texture.TextureCubeMap

private val CUBE_FACE_NORMALS = arrayOf(
    Vec3(1f, 0f, 0f), Vec3(-1f, 0f, 0f), Vec3(0f, 1f, 0f),
    Vec3(0f, -1f, 0f), Vec3(0f, 0f, 1f), Vec3(0f, 0f, -1f)
)

private val CUBE_FACE_UPS = arrayOf(
    Vec3(0f, -1f, 0f), Vec3(0f, -1f, 0f), Vec3(0f, 0f, 1f),
    Vec3(0f, 0f, -1f), Vec3(0f, -1f, 0f), Vec3(0f, -1f, 0f)
)

fun getCombinedMatrices(
    size: Vec2ic,
    near: Float,
    far: Float,
    light: PointLight,
    targets: Array<Mat4>
): Array<Mat4> {
    var projection = Mat4().apply {
        setPerspective(90f.deg, size.x.toFloat() / size.y.toFloat(), near, far)
    }
    for (i in 0..<TextureCubeMap.NUM_SIDES) {
        targets[i].setLookAt(light.position, light.position + CUBE_FACE_NORMALS[i], CUBE_FACE_UPS[i])
        projection *= targets[i]
    }

    return targets
}
