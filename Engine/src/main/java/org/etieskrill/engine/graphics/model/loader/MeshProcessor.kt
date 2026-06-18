package org.etieskrill.engine.graphics.model.loader

import io.github.oshai.kotlinlogging.KotlinLogging
import org.etieskrill.engine.graphics.model.Bone
import org.etieskrill.engine.graphics.model.Material
import org.etieskrill.engine.graphics.model.Mesh
import org.etieskrill.engine.graphics.model.Vertex
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f
import org.joml.Vector4i
import org.joml.primitives.AABBf
import org.lwjgl.BufferUtils
import org.lwjgl.assimp.AIMesh
import org.lwjgl.assimp.AIScene
import org.lwjgl.assimp.Assimp.*
import org.lwjgl.util.meshoptimizer.MeshOptimizer.meshopt_simplify

private val logger = KotlinLogging.logger {}

internal fun loadMeshes(
    scene: AIScene,
    materials: List<Material>,
    meshes: MutableList<Mesh>,
    bones: MutableList<Bone>
) {
    val meshBuffer = scene.mMeshes() ?: return

    val newMeshes = generateSequence { AIMesh.create(meshBuffer.get()) }
        .take(scene.mNumMeshes())
        .map { processMesh(it, materials) }
        .toList()

    meshes += newMeshes
    bones += newMeshes.flatMap { it.bones ?: emptyList() }.distinct()
}

@OptIn(ExperimentalStdlibApi::class)
private fun processMesh(aiMesh: AIMesh, materials: List<Material>): Mesh {
    val positions = aiMesh.mVertices()
    val normals = aiMesh.mNormals()
    val texCoords = aiMesh.mTextureCoords(0)
    val tangents = aiMesh.mTangents()
    val biTangents = aiMesh.mBitangents()

    val (bones, boneWeights) = getBones(aiMesh)

    val vertices = List(aiMesh.mNumVertices()) { i ->
        Vertex(
            position = positions.get().let { Vector3f(it.x(), it.y(), it.z()) },
            normal = normals?.get()?.let { Vector3f(it.x(), it.y(), it.z()) },
            textureCoords = texCoords?.get()?.let { Vector2f(it.x(), it.y()) },
            tangent = tangents?.get()?.let { Vector3f(it.x(), it.y(), it.z()) },
            biTangent = biTangents?.get()?.let { Vector3f(it.x(), it.y(), it.z()) },
            bones = boneWeights?.get(i)?.bones ?: Vector4i(-1),
            boneWeights = boneWeights?.get(i)?.boneWeights ?: Vector4f(0f)
        )
    }

    val indices = mutableListOf<Int>()
    val faceBuffer = aiMesh.mFaces()
    repeat(aiMesh.mNumFaces()) {
        val face = faceBuffer.get()
        val indexBuffer = face.mIndices()
        repeat(face.mNumIndices()) {
            indices += indexBuffer.get()
        }
    }

//    val min = aiMesh.mAABB().mMin()
//    val max = aiMesh.mAABB().mMax()
//    val boundingBox = AABBf(min.x(), min.y(), min.z(), max.x(), max.y(), max.z())
    val boundingBox = calculateBoundingBox(vertices)

    val material = materials[aiMesh.mMaterialIndex()]

    val primitiveType = aiMesh.mPrimitiveTypes()
    if (primitiveType and aiPrimitiveType_NGONEncodingFlag > 0) {
        logger.info { "Mesh contains n-gon encoding, which is not explicitly supported; if a mesh looks weird, this may be the reason" } //FIXME i dunnot if this even has any impact
    }
    val drawMode = when (primitiveType and aiPrimitiveType_NGONEncodingFlag.inv()) {
        aiPrimitiveType_POINT -> Mesh.DrawMode.POINTS
        aiPrimitiveType_LINE -> Mesh.DrawMode.LINES
        aiPrimitiveType_TRIANGLE -> Mesh.DrawMode.TRIANGLES
        else -> error("Unsupported primitive type: 0x${primitiveType.toHexString()}")
    }

    val mesh = loadToVAO(vertices, indices, material, bones, boundingBox, drawMode)

    logger.trace { "Loaded mesh with ${vertices.size} vertices and ${indices.size} indices" }
    return mesh
}

fun calculateBoundingBox(vertices: List<Vertex>) = AABBf().apply { vertices.forEach { union(it.position) } }

fun optimiseMesh(mesh: Mesh, targetIndexCount: Int, maxDeformation: Float) {
    val vertexData = mesh.vao.vertexBuffer.getData()
    //TODO workaround: temporary 1-1 vertex-index buffer? duplicate vertices did make the algorithm shit itself tho, so probably do an actual index run using... assimp? can it even do that?
    val indexData = mesh.vao.indexBuffer?.getData()?.asIntBuffer() ?: error("Can only optimise indexed meshes")

    val newIndexData = BufferUtils.createByteBuffer(Int.SIZE_BYTES * indexData.capacity())

    val errorBuffer = BufferUtils.createFloatBuffer(1)

    val vertexBytes = Vertex.Companion.Accessor.elementByteSize.toLong()
    val numIndices = meshopt_simplify(
        newIndexData.asIntBuffer(), indexData, vertexData.asFloatBuffer(),
        vertexData.capacity() / vertexBytes, vertexBytes,
        targetIndexCount.toLong(), maxDeformation, 0, errorBuffer
    )
    //TODO compress vertex buffer using new indices

    logger.trace { "Original mesh has ${vertexData.capacity() / vertexBytes} vertices and ${indexData.capacity()} indices" }
    logger.trace {
        "Optimised mesh has %d indices and a deformation of %5.3f%% (max %5.3f%%)"
            .format(numIndices, 100 * errorBuffer.get(), 100 * maxDeformation)
    }
    logger.debug { "Mesh was compressed by a factor of %4.1f".format(indexData.capacity().toFloat() / numIndices) }

    mesh.vao.indexBuffer.setData(newIndexData)
}
