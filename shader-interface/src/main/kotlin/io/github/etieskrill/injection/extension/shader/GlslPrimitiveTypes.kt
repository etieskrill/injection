package io.github.etieskrill.injection.extension.shader

import org.joml.*

typealias int = Int
typealias float = Float
typealias bool = Boolean

//FIXME used to be final versions (e.g. Vector2fc), changed for dsl module, warn to not directly modify, or add observable or something
typealias vec2 = Vector2f
typealias vec3 = Vector3f
typealias vec4 = Vector4f

typealias mat3 = Matrix3f
typealias mat4 = Matrix4f

typealias sampler2D = Texture2D
typealias sampler2DArray = Texture2DArray
typealias sampler2DShadow = ShadowMap<Texture2D>
typealias samplerCubeArrayShadow = ShadowMap<TextureCubeMapArray>

typealias struct = Any
