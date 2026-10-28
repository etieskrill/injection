package org.etieskrill.engine.graphics.model

import net.bifreus.kml.AABB
import net.bifreus.kml.Vec2
import net.bifreus.kml.Vec3
import org.etieskrill.engine.entity.component.Transform
import org.etieskrill.engine.graphics.model.loader.loadToVAO
import kotlin.math.cos
import kotlin.math.sin

fun model(name: String, block: ModelBuilder.() -> Unit): Model {
    val builder = ModelBuilder().apply(block)
    return Model(name, builder.rootNode, emptyList(), emptyList(), builder.boundingBox)
}

data class GeometryCounters(
    var planes: Int = 0,
    var boxes: Int = 0,
    var spheres: Int = 0
)

class ModelBuilder(
    var culling: Boolean = true,
    internal val rootNode: Node = Node("root"),
    internal val boundingBox: AABB = AABB(),
    internal val counters: GeometryCounters = GeometryCounters()
)

fun ModelBuilder.plane(a: Vec2, b: Vec2, transform: Transform? = null, material: Material? = null) {
    val cross = Vec3(0f, 1f, 0f)
    val vertices: List<Vertex> = listOf(
        Vertex(Vec3(a.x, 0f, a.y), normal = cross, textureCoords = Vec2(0f, 0f), tangent = Vec3(1f, 0f, 0f), biTangent = Vec3(0f, 0f, 1f)),
        Vertex(Vec3(a.x, 0f, b.y), normal = cross, textureCoords = Vec2(0f, 1f), tangent = Vec3(1f, 0f, 0f), biTangent = Vec3(0f, 0f, 1f)),
        Vertex(Vec3(b.x, 0f, b.y), normal = cross, textureCoords = Vec2(1f, 1f), tangent = Vec3(1f, 0f, 0f), biTangent = Vec3(0f, 0f, 1f)),
        Vertex(Vec3(b.x, 0f, a.y), normal = cross, textureCoords = Vec2(1f, 0f), tangent = Vec3(1f, 0f, 0f), biTangent = Vec3(0f, 0f, 1f))
    )
    val indices = listOf(0, 1, 2, 0, 2, 3)

    createModel(
        "plane-${counters.planes++}",
        vertices,
        indices,
        AABB(Vec3(a.x, 0f, a.y), Vec3(b.x, 0f, b.y)),
        transform,
        material
    )
}

fun ModelBuilder.box(a: Vec3, b: Vec3, transform: Transform? = null, material: Material? = null) {
    require(a.x <= b.x && a.y <= b.y && a.z <= b.z) { "Coordinates of point b may not be smaller than point a" }

    val vertices: List<Vertex> = listOf(
        // top
        Vertex(Vec3(a.x, b.y, b.z), normal = Vec3(0f, 1f, 0f)),
        Vertex(Vec3(b.x, b.y, b.z), normal = Vec3(0f, 1f, 0f)),
        Vertex(Vec3(b.x, b.y, a.z), normal = Vec3(0f, 1f, 0f)),
        Vertex(Vec3(a.x, b.y, a.z), normal = Vec3(0f, 1f, 0f)),
        // bottom
        Vertex(Vec3(a.x, a.y, a.z), normal = Vec3(0f, -1f, 0f)),
        Vertex(Vec3(b.x, a.y, a.z), normal = Vec3(0f, -1f, 0f)),
        Vertex(Vec3(b.x, a.y, b.z), normal = Vec3(0f, -1f, 0f)),
        Vertex(Vec3(a.x, a.y, b.z), normal = Vec3(0f, -1f, 0f)),
        // front
        Vertex(Vec3(a.x, b.y, a.z), normal = Vec3(0f, 0f, -1f)),
        Vertex(Vec3(b.x, b.y, a.z), normal = Vec3(0f, 0f, -1f)),
        Vertex(Vec3(b.x, a.y, a.z), normal = Vec3(0f, 0f, -1f)),
        Vertex(Vec3(a.x, a.y, a.z), normal = Vec3(0f, 0f, -1f)),
        // back
        Vertex(Vec3(a.x, a.y, b.z), normal = Vec3(0f, 0f, 1f)),
        Vertex(Vec3(b.x, a.y, b.z), normal = Vec3(0f, 0f, 1f)),
        Vertex(Vec3(b.x, b.y, b.z), normal = Vec3(0f, 0f, 1f)),
        Vertex(Vec3(a.x, b.y, b.z), normal = Vec3(0f, 0f, 1f)),
        // left
        Vertex(Vec3(a.x, b.y, b.z), normal = Vec3(-1f, 0f, 0f)),
        Vertex(Vec3(a.x, b.y, a.z), normal = Vec3(-1f, 0f, 0f)),
        Vertex(Vec3(a.x, a.y, a.z), normal = Vec3(-1f, 0f, 0f)),
        Vertex(Vec3(a.x, a.y, b.z), normal = Vec3(-1f, 0f, 0f)),
        // right
        Vertex(Vec3(b.x, b.y, a.z), normal = Vec3(1f, 0f, 0f)),
        Vertex(Vec3(b.x, b.y, b.z), normal = Vec3(1f, 0f, 0f)),
        Vertex(Vec3(b.x, a.y, b.z), normal = Vec3(1f, 0f, 0f)),
        Vertex(Vec3(b.x, a.y, a.z), normal = Vec3(1f, 0f, 0f))
    )

    val indices = listOf(
        0, 1, 2, 0, 2, 3, //top
        4, 5, 6, 4, 6, 7, //bottom
        8, 9, 10, 8, 10, 11, //front
        12, 13, 14, 12, 14, 15, //back
        16, 17, 18, 16, 18, 19, //left
        20, 21, 22, 20, 22, 23  //right
    )

    createModel("box-${counters.boxes++}", vertices, indices, AABB(a, b), transform, material)
}

/**
 * Note that the [number of points][numPoints] may be an approximation in some cases, but the generated sphere will
 * never have fewer points than specified.
 */
//TODO fibonacci sphere
fun ModelBuilder.sphere(radius: Float, numPoints: Int, transform: Transform? = null, material: Material? = null) {
    val segments = (0..100000).find { it * it + 1 >= numPoints }
        ?: error(
            "Primitive sphere number of segments would exceed 100000; specify a smaller number of points or " +
                    "call 'primitiveSphere' explicitly"
        )
    primitiveSphere(radius, segments, transform = transform, material = material)
}

fun ModelBuilder.primitiveSphere(
    radius: Float,
    xSegments: Int,
    ySegments: Int = xSegments,
    transform: Transform? = null,
    material: Material? = null
) {
    require(radius > 0) { "Sphere radius must be positive" }
    require(xSegments >= 3) { "Sphere must have at least 3 horizontal segments" }
    require(ySegments >= 1) { "Sphere must have at least 1 vertical segment" }

    val vertices = mutableListOf<Vertex>()
    val indices = mutableListOf<Int>()

    //TODO could improve by using vertical triangle strips instead

    vertices += Vertex(Vec3(0f, radius, 0f), normal = Vec3(0f, 1f, 0f))
    for (xSegment in 0..<xSegments) {
        indices += 0
        indices += xSegment + 1
        indices += xSegment + 2
    }

    for (ySegment in 1..<ySegments) {
        for (xSegment in 0..xSegments) {
            val theta = Math.PI.toFloat() * ySegment / ySegments
            val phi = 2 * Math.PI.toFloat() * xSegment / xSegments

            val x = radius * sin(theta) * sin(phi)
            val y = radius * cos(theta)
            val z = radius * sin(theta) * cos(phi)

            val position = Vec3(x, y, z)
            vertices += Vertex(position, normal = Vec3(position).normalize())

            if (ySegment == ySegments - 1 && xSegment > xSegments - 3) continue

            indices += 1 + ySegment * ySegments + xSegment
            indices += 1 + ySegment * ySegments + xSegment + 1
            indices += 1 + (ySegment - 1) * ySegments + xSegment

            indices += 1 + (ySegment - 1) * ySegments + xSegment
            indices += 1 + ySegment * ySegments + xSegment + 1
            indices += 1 + (ySegment - 1) * ySegments + xSegment + 1
        }
    }

    vertices += Vertex(Vec3(0f, -radius, 0f), normal = Vec3(0f, -1f, 0f))
    for (xSegment in -1..<xSegments - 1) {
        indices += vertices.lastIndex
        indices += vertices.lastIndex - ySegments + xSegment + 1
        indices += vertices.lastIndex - ySegments + xSegment
    }

    val numPoints = (ySegments - 1) * (xSegments + 1) + 2
    check(vertices.size == numPoints) { "Expected $numPoints points, but generated ${vertices.size}" }

    createModel(
        "sphere-${counters.spheres++}", vertices, indices,
        AABB(Vec3(-radius), Vec3(radius)), transform, material
    )
}

private fun ModelBuilder.createModel(
    name: String,
    vertices: List<Vertex>,
    indices: List<Int>,
    boundingBox: AABB = AABB(),
    transform: Transform? = null,
    material: Material? = null
) {
    val transform = transform ?: Transform()
    val material = material ?: PhongMaterial(isTwoSided = !culling)

    val mesh = loadToVAO(vertices, indices, material, boundingBox = boundingBox)

    val node = Node(name, transform, rootNode, meshes = listOf(mesh))
    rootNode.children += node

    this.boundingBox(this.boundingBox + (transform.matrix * boundingBox))
}
