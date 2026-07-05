package io.github.etieskrill.injection.extension.shader

interface Texture {

    fun bind(unit: Int)
    fun bind() = bind(0)

    fun unbind(unit: Int)

}

interface Texture2D : Texture
interface Texture2DArray : Texture
interface TextureCubeMap : Texture
interface TextureCubeMapArray : Texture

interface TextureShadow : Texture

interface Texture2DShadow : TextureShadow
interface Texture2DArrayShadow : TextureShadow
interface TextureCubeMapShadow : TextureShadow
interface TextureCubeMapArrayShadow : TextureShadow
