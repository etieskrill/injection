package org.etieskrill.engine.graphics.model.loader

import io.github.oshai.kotlinlogging.KotlinLogging
import org.etieskrill.engine.common.ResourceLoadException
import org.etieskrill.engine.entity.component.Transform
import org.etieskrill.engine.graphics.animation.Animation
import org.etieskrill.engine.graphics.model.Bone
import org.etieskrill.engine.graphics.model.Material
import org.etieskrill.engine.graphics.model.Mesh
import org.etieskrill.engine.graphics.model.Model
import org.etieskrill.engine.graphics.model.Node
import org.etieskrill.engine.graphics.util.AssimpUtils
import org.etieskrill.engine.time.StepTimer
import org.joml.Matrix4fc
import org.joml.primitives.AABBf
import org.lwjgl.assimp.AINode
import org.lwjgl.assimp.Assimp.*

private val logger = KotlinLogging.logger {}
private val timer = StepTimer(logger)

actual fun loadModel(file: String, options: ModelLoaderOptions): Model {
//        aiAttachLogStream(AILogStream.create() //TODO i mean... it's possible if ever needed
//                .callback((messagePointer, userPointer) -> {
//                    String message = MemoryUtil
//                            .memUTF8(messagePointer)
//                            .replace(System.lineSeparator(), "") //TODO assimp appears to always spit out unix lfs; so the below line may suffice
//                            .replace("\n", "");
//                    logger.info("Assimp: {}", message);
//                }));

    timer.start()

    val aiScene = importScene(file, options)

    timer.log { "Imported scene" }

    val rootNode = aiScene.mRootNode()

    if ((aiScene.mFlags() and AI_SCENE_FLAGS_INCOMPLETE) != 0
        || rootNode == null
    ) {
        throw ResourceLoadException(aiGetErrorString())
    }

    val embeddedTextures = loadEmbeddedTextures(aiScene)
    timer.log { "Loaded embedded textures" }

    val materials = mutableListOf<Material>()
    loadMaterials(aiScene, materials, embeddedTextures, "null")
    timer.log { "Loaded materials" }

    val meshes = mutableListOf<Mesh>()
    val bones = mutableListOf<Bone>()
    loadMeshes(aiScene, materials, meshes, bones)
    timer.log { "Loaded meshes and bones" }

    val node = processNode(null, rootNode, meshes, bones)
    fun countNodes(node: Node): Int = 1 + node.children.sumOf { countNodes(it) }
    val numNodes = countNodes(node)
    timer.log { "Loaded $numNodes nodes" }

    val animations = loadAnimations(
        aiScene,
        bones
    ) //animations reference bones, which need first be loaded from the meshes, and also require the nodes to resolve the back reference
    timer.log { "Loaded animations" }

    val aabb = calculateModelBoundingBox(meshes)

    aiReleaseImport(aiScene)

    val name = aiScene.mName().dataString()

    val model = Model(name, node, animations, bones, aabb)

    timer.log {
        "Loaded model $name with $numNodes nodes, ${meshes.size} mesh/es, ${materials.size} material/s, ${bones.size} bone/s, and ${animations.size} animation/s"
    }

    return model
}

private fun processNode(parent: Node?, aiNode: AINode, meshes: List<Mesh>, bones: List<Bone>): Node {
    val nodeName = aiNode.mName().dataString()

    val transformationMatrix: Matrix4fc = AssimpUtils.fromAI(aiNode.mTransformation())
    val transform = Transform(transformationMatrix)
//        if (aiNode.mParent() == null && builder.getInitialTransform() != null) { //FIXME see Model#Model()
//            transform = new Transform(builder.getInitialTransform()).compose(transform);
//        }

    val bone = bones.find { it.name == nodeName }
    val node = Node(
        nodeName,
        transform,
        parent,
        mutableListOf(),
        getNodeMeshes(aiNode, meshes),
        bone
    )

    parent?.children += node

    val mChildren = aiNode.mChildren() ?: return node
    generateSequence { AINode.create(mChildren.get()) }
        .take(aiNode.mNumChildren())
        .forEach { processNode(node, it, meshes, bones) }

    return node
}

private fun getNodeMeshes(aiNode: AINode, meshes: List<Mesh>): List<Mesh> {
    val meshIndexBuffer = aiNode.mMeshes() ?: return emptyList()
    return generateSequence { meshIndexBuffer.get() }
        .take(aiNode.mNumMeshes())
        .map { meshes[it] }
        .toList()
}

private fun calculateModelBoundingBox(meshes: List<Mesh>): AABBf {
    val modelAabb = AABBf(meshes[0].boundingBox)
    meshes.forEach { modelAabb.union(it.boundingBox) }
    return modelAabb
}

//TODO loading animations is probs to be separated from model loading: either
// - load bones with animation every time, and resolve stuff when assigning an animation to a model/animator or
// - require model/nodes when loading in order to link dependencies directly on load
actual fun loadModelAnimations(file: String, model: Model, boneMatcher: BoneMatcher): List<Animation> {
    logger.info { "Loading animations from '$file'" }

    val aiScene = importScene(
        file, ModelLoaderOptions(
            flipTextureCoordinates = false, flipWinding = false
        )
    )

    return loadAnimations(aiScene, model.bones, boneMatcher)
}
