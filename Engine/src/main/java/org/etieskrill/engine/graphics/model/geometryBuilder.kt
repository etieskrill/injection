package org.etieskrill.engine.graphics.model

import org.etieskrill.engine.entity.component.Transform
import org.etieskrill.engine.graphics.model.loader.MeshLoader
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.primitives.AABBf
import kotlin.math.cos
import kotlin.math.sin

fun model(name: String, block: ModelBuilder.() -> Unit): Model {
    val model = Model.MemoryBuilder(name).apply { boundingBox = AABBf() }
    block(ModelBuilder(model).apply(block))
    return model.build()
}

data class GeometryCounters(
    var planes: Int = 0,
    var spheres: Int = 0
)

class ModelBuilder(
    internal val model: Model.Builder,
    internal val counters: GeometryCounters = GeometryCounters()
)

fun ModelBuilder.plane(a: Vector2f, b: Vector2f) {
    val vertices: List<Vertex> = listOf(
        Vertex.builder(Vector3f(a.x, 0f, a.y)).build(),
        Vertex.builder(Vector3f(a.x, 0f, b.y)).build(),
        Vertex.builder(Vector3f(b.x, 0f, b.y)).build(),
        Vertex.builder(Vector3f(b.x, 0f, a.y)).build(),
    )
    val indices = listOf(0, 1, 2, 0, 2, 3)

    createModel("plane-${counters.planes++}", vertices, indices, AABBf(Vector3f(a.x, 0f, a.y), Vector3f(b.x, 0f, b.y)))
}

/**
 * Note that the [number of points][numPoints] may be an approximation in some cases, but the generated sphere will
 * never have fewer points than specified.
 */
//TODO fibonacci sphere
fun ModelBuilder.sphere(radius: Float, numPoints: Int) {
    val segments = (0..100000).find { it * it + 1 >= numPoints }
        ?: error(
            "Primitive sphere number of segments would exceed 100000; specify a smaller number of points or " +
                    "call 'primitiveSphere' explicitly"
        )
    primitiveSphere(radius, segments)
}

fun ModelBuilder.primitiveSphere(radius: Float, xSegments: Int, ySegments: Int = xSegments) {
    val vertices = mutableListOf<Vertex>()
    val indices = mutableListOf<Int>()

    //TODO could improve by using vertical triangle strips instead

    vertices += Vertex.builder(Vector3f(0f, radius, 0f)).build()
    for (xSegment in 0..<xSegments) {
        indices += 0
        indices += xSegment + 2
        indices += xSegment + 1
    }

    for (ySegment in 1..<ySegments) {
        for (xSegment in 0..xSegments) {
            val theta = Math.PI.toFloat() * ySegment / ySegments
            val phi = 2 * Math.PI.toFloat() * xSegment / xSegments

            val x = radius * sin(theta) * sin(phi)
            val y = radius * cos(theta)
            val z = radius * sin(theta) * cos(phi)

            vertices += Vertex.builder(Vector3f(x, y, z)).build()

            if (ySegment == ySegments - 1 && xSegment > xSegments - 3) continue

            indices += 1 + ySegment * ySegments + xSegment
            indices += 1 + (ySegment - 1) * ySegments + xSegment
            indices += 1 + ySegment * ySegments + xSegment + 1

            indices += 1 + (ySegment - 1) * ySegments + xSegment
            indices += 1 + (ySegment - 1) * ySegments + xSegment + 1
            indices += 1 + ySegment * ySegments + xSegment + 1
        }
    }

    vertices += Vertex.builder(Vector3f(0f, -radius, 0f)).build()
    for (xSegment in 0..<xSegments - 1) {
        indices += vertices.lastIndex
        indices += vertices.lastIndex - ySegments + xSegment + 1
        indices += vertices.lastIndex - ySegments + xSegment
    }

    val numPoints = (ySegments - 1) * (xSegments + 1) + 2
    check(vertices.size == numPoints) { "Expected $numPoints points, but generated ${vertices.size}" }

    createModel("sphere-${counters.spheres++}", vertices, indices)
}

private fun ModelBuilder.createModel(
    name: String,
    vertices: List<Vertex>,
    indices: List<Int>,
    boundingBox: AABBf? = null
) {
    val mesh = MeshLoader.loadToVAO(vertices, indices, Material.Builder().build(), boundingBox)

    val node = Node(name, null, Transform(), listOf(mesh), null)
    model.nodes += node

    if (boundingBox != null) model.boundingBox = model.boundingBox.union(mesh.boundingBox)
}
