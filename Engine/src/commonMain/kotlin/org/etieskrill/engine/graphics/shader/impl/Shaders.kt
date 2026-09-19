package org.etieskrill.engine.graphics.shader.impl

import io.github.etieskrill.injection.extension.shader.reflection.ReflectShader
import org.etieskrill.engine.graphics.shader.Shader

//TODO move em fuckers to their game module - with the resources!!!
@ReflectShader
class ContainerShader : Shader(listOf("shaders/Container.vert", "shaders/Container.frag"), false)

@ReflectShader
class SwordShader : Shader(listOf("shaders/Sword.vert", "shaders/Sword.frag"), false)

@ReflectShader
class RoundedBoxShader : Shader(listOf("shaders/RoundedBox.vert", "shaders/RoundedBox.frag"))

@ReflectShader
class TextureShader : Shader(listOf("shaders/Texture.vert", "shaders/Texture.frag"))

@ReflectShader
class PhongShininessMapShader : Shader(listOf("shaders/PhongShininessMap.vert", "shaders/PhongShininessMap.frag"))

@ReflectShader
class ScreenQuadShader : Shader(listOf("shaders/ScreenQuad.vert", "shaders/ScreenQuad.frag"))

@ReflectShader
class PostprocessingShader : Shader(listOf("shaders/Postprocessing.vert", "shaders/Postprocessing.frag"))

@ReflectShader
class CubeMapShader : Shader(listOf("shaders/CubeMap.vert", "shaders/CubeMap.frag"))

@ReflectShader
class ShowNormalsShader :
    Shader(listOf("shaders/ShowNormals.vert", "shaders/ShowNormals.geom", "shaders/ShowNormals.frag"))

@ReflectShader
class DepthCubeMapShader :
    Shader(listOf("shaders/DepthCubeMap.vert", "shaders/DepthCubeMap.geom", "shaders/DepthCubeMap.frag"))

@ReflectShader
class WireframeShader : Shader(listOf("shaders/Wireframe.glsl"))
