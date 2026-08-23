package org.etieskrill.engine.graphics.model.loader;

import org.etieskrill.engine.graphics.animation.Animation
import org.etieskrill.engine.graphics.model.Model

data class ModelLoaderOptions(
    val flipTextureCoordinates: Boolean = false,
    val flipWinding: Boolean = false,
    val optimiseMeshes: Boolean = false,
    val optimiseVertexCount: Int = 0,
    val optimiseMaxDeviation: Float = 0f
)

expect fun loadModel(file: String, options: ModelLoaderOptions = ModelLoaderOptions()): Model

typealias BoneMatcher = (String, String) -> Boolean

val DEFAULT_BONE_MATCHER: BoneMatcher = { modelBone, animBone ->
    //TODO pretty lenient bone name matching for the time being to allow for several file formats - should be undone
    modelBone.replace("_", "").replace(":", "") ==
            animBone.replace("_", "").replace(":", "")
}

expect fun loadModelAnimations(
    file: String,
    model: Model,
    boneMatcher: BoneMatcher = DEFAULT_BONE_MATCHER
): List<Animation>
