package org.etieskrill.engine.graphics.gl.shader;

import io.etieskrill.injection.extension.shaderreflection.ReflectShader;
import org.etieskrill.engine.graphics.gl.shader.impl.LightSourceShader;

import java.util.List;

public class Shaders {

    public static LightSourceShader getLightSourceShader() {
        return new LightSourceShader();
    }

    public static TextureShader getTextureShader() {
        return new TextureShader();
    }

    public static OutlineShader getOutlineShader() {
        return new OutlineShader();
    }

    public static PhongShininessMapShader getBackpackShader() {
        return new PhongShininessMapShader();
    }

    public static CubeMapShader getCubeMapShader() {
        return new CubeMapShader();
    }

    public static PostprocessingShader getPostprocessingShader() {
        return new PostprocessingShader();
    }

    public static TextShader getTextShader() {
        return new TextShader();
    }

    @ReflectShader
    public static class ContainerShader extends ShaderProgram {
        public ContainerShader() {
            super(List.of("Container.vert", "Container.frag"), false);
        }
    }

    @ReflectShader
    public static class SwordShader extends ShaderProgram {
        public SwordShader() {
            super(List.of("Sword.vert", "Sword.frag"), false);
        }
    }

    @ReflectShader
    public static class RoundedBoxShader extends ShaderProgram {
        public RoundedBoxShader() {
            super(List.of("RoundedBox.vert", "RoundedBox.frag"));
        }
    }

    @ReflectShader
    public static class TextureShader extends ShaderProgram {
        public TextureShader() {
            super(List.of("Texture.vert", "Texture.frag"));
        }
    }

    @ReflectShader
    public static class OutlineShader extends ShaderProgram {
        public OutlineShader() {
            super(List.of("Outline.vert", "Outline.frag"));
        }
    }

    @ReflectShader
    public static class PhongShininessMapShader extends ShaderProgram {
        public PhongShininessMapShader() {
            super(List.of("PhongShininessMap.vert", "PhongShininessMap.frag"));
        }
    }

    @ReflectShader
    public static class ScreenQuadShader extends ShaderProgram {
        public ScreenQuadShader() {
            super(List.of("ScreenQuad.vert", "ScreenQuad.frag"));
        }
    }

    @ReflectShader
    public static class PostprocessingShader extends ShaderProgram {
        public PostprocessingShader() {
            super(List.of("Postprocessing.vert", "Postprocessing.frag"));
        }
    }

    @ReflectShader
    public static class CubeMapShader extends ShaderProgram {
        public CubeMapShader() {
            super(List.of("CubeMap.vert", "CubeMap.frag"));
        }
    }

    @ReflectShader
    public static class TextShader extends ShaderProgram {
        public TextShader() {
            super(List.of("Text.vert", "Text.geom", "Text.frag"));
        }
    }

    @ReflectShader
    public static class ShowNormalsShader extends ShaderProgram {
        public ShowNormalsShader() {
            super(List.of("ShowNormals.vert", "ShowNormals.geom", "ShowNormals.frag"));
        }
    }

    @ReflectShader
    public static class DepthShader extends ShaderProgram {
        public DepthShader() {
            super(List.of("Depth.vert", "Depth.frag"));
        }
    }

    @ReflectShader
    public static class DepthCubeMapShader extends ShaderProgram {
        public DepthCubeMapShader() {
            super(List.of("DepthCubeMap.vert", "DepthCubeMap.geom", "DepthCubeMap.frag"));
        }
    }

    @ReflectShader
    public static class WireframeShader extends ShaderProgram {
        public WireframeShader() {
            super(List.of("Wireframe.glsl"));
        }
    }

}
