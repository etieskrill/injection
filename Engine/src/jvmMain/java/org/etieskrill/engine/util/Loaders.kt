package org.etieskrill.engine.util

import io.github.oshai.kotlinlogging.KotlinLogging
import org.etieskrill.engine.graphics.animation.Animation
import org.etieskrill.engine.graphics.gl.shader.ShaderProgram
import org.etieskrill.engine.graphics.model.Mesh
import org.etieskrill.engine.graphics.model.Model
import org.etieskrill.engine.graphics.text.Font
import org.etieskrill.engine.graphics.texture.AbstractTexture

private val logger = KotlinLogging.logger {}

open class TextureLoader : DisposableLoader<AbstractTexture>() {
    override val loaderName: String get() = "Texture"
}

object EngineTextureLoader : TextureLoader()

open class MeshLoader : DisposableLoader<Mesh>() {
    override val loaderName: String get() = "Mesh"
}

object EngineMeshLoader : MeshLoader()

/**
 * Creates a new instance for every call to [get], where data in graphics memory - such as model
 * and material data - are shared between models, but not instance fields such as transform.
 */
//TODO clean up model
open class ModelLoader : DisposableLoader<Model>() {
    override val loaderName: String get() = "Model"

    override operator fun get(name: String): Model? {
        val model = map[name] ?: return null
        logger.debug { "Creating new instance of model $name" }
        return Model(model)
    }
}

object EngineModelLoader : ModelLoader()

open class ShaderLoader : DisposableLoader<ShaderProgram>() {
    override val loaderName: String get() = "Shader"
}

object EngineShaderLoader : ShaderLoader()

open class FontLoader : DisposableLoader<Font>() {
    override val loaderName: String get() = "Font"
}

object EngineFontLoader : FontLoader()

open class AnimationLoader : Loader<Animation>() {
    override val loaderName: String get() = "Animation"
}

object EngineAnimationLoader : AnimationLoader()

fun disposeDefaultLoaders() {
    EngineTextureLoader.dispose()
    EngineMeshLoader.dispose()
    EngineModelLoader.dispose()
    EngineShaderLoader.dispose()
    EngineFontLoader.dispose()
}
