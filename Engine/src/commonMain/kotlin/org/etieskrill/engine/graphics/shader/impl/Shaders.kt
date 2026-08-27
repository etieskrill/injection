package org.etieskrill.engine.graphics.shader.impl

import io.github.etieskrill.injection.extension.shader.reflection.ReflectShader
import org.etieskrill.engine.graphics.shader.Shader

//TODO move em fuckers to their game module - with the resources!!!
@ReflectShader
class ContainerShader : Shader(listOf("Container.vert", "Container.frag"), false)

@ReflectShader
class SwordShader : Shader(listOf("Sword.vert", "Sword.frag"), false)

@ReflectShader
class RoundedBoxShader : Shader(listOf("RoundedBox.vert", "RoundedBox.frag"))

@ReflectShader
class TextureShader : Shader(listOf("Texture.vert", "Texture.frag"))

@ReflectShader
class PhongShininessMapShader : Shader(listOf("PhongShininessMap.vert", "PhongShininessMap.frag"))

@ReflectShader
class ScreenQuadShader : Shader(listOf("ScreenQuad.vert", "ScreenQuad.frag"))

@ReflectShader
class PostprocessingShader : Shader(listOf("Postprocessing.vert", "Postprocessing.frag"))

@ReflectShader
class CubeMapShader : Shader(listOf("CubeMap.vert", "CubeMap.frag"))

@ReflectShader
class ShowNormalsShader : Shader(listOf("ShowNormals.vert", "ShowNormals.geom", "ShowNormals.frag"))

@ReflectShader
class DepthCubeMapShader : Shader(listOf("DepthCubeMap.vert", "DepthCubeMap.geom", "DepthCubeMap.frag"))

@ReflectShader
class WireframeShader : Shader(listOf("Wireframe.glsl"))
