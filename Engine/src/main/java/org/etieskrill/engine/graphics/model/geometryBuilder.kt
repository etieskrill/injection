package org.etieskrill.engine.graphics.model

import org.etieskrill.engine.entity.component.Transform
import org.etieskrill.engine.graphics.model.loader.MeshLoader
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.primitives.AABBf

fun model(name: String, block: ModelBuilder.() -> Unit): Model {
    val model = Model.MemoryBuilder(name).apply { boundingBox = AABBf() }
    block(ModelBuilder(model).apply(block))
    return model.build()
}

data class GeometryCounters(
    var planes: Int = 0
)

class ModelBuilder(
    private val model: Model.Builder,
    private val counters: GeometryCounters = GeometryCounters()
) {
    fun plane(a: Vector2f, b: Vector2f) {
        val name = "plane-${counters.planes++}"

        val vertices: List<Vertex> = listOf(
            Vertex.builder(Vector3f(a.x, 0f, a.y)).build(),
            Vertex.builder(Vector3f(a.x, 0f, b.y)).build(),
            Vertex.builder(Vector3f(b.x, 0f, b.y)).build(),
            Vertex.builder(Vector3f(b.x, 0f, a.y)).build(),
        )
        val indices = listOf(0, 1, 2, 0, 2, 3)
        val mesh = MeshLoader.loadToVAO(
            vertices,
            indices,
            Material.Builder().build(),
            AABBf(Vector3f(a.x, 0f, a.y), Vector3f(b.x, 0f, b.y))
        )

        val node = Node(name, null, Transform(), listOf(mesh), null)

        model.nodes += node
        model.boundingBox = model.boundingBox.union(mesh.boundingBox)
    }
}
