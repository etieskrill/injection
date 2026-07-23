package org.etieskrill.engine.graphics.gl.shader;

import io.github.etieskrill.injection.extension.shader.reflection.ReflectShader;
import org.etieskrill.engine.graphics.shader.Shader;

import java.util.List;

public class Shaders {

    public static TextureShader getTextureShader() {
        return new TextureShader();
    }

    public static OutlineShader getOutlineShader() {
        return new OutlineShader();
    }

    public static TextShader getTextShader() {
        return new TextShader();
    }

    //TODO move em fuckers to their game module - with the resources!!!
    @ReflectShader
    public static class ContainerShader extends Shader {
        public ContainerShader() {
            super(List.of("Container.vert", "Container.frag"), false);
        }
    }

    @ReflectShader
    public static class SwordShader extends Shader {
        public SwordShader() {
            super(List.of("Sword.vert", "Sword.frag"), false);
        }
    }

    @ReflectShader
    public static class RoundedBoxShader extends Shader {
        public RoundedBoxShader() {
            super(List.of("RoundedBox.vert", "RoundedBox.frag"));
        }
    }

    @ReflectShader
    public static class TextureShader extends Shader {
        public TextureShader() {
            super(List.of("Texture.vert", "Texture.frag"));
        }
    }

    @ReflectShader
    public static class OutlineShader extends Shader {
        public OutlineShader() {
            super(List.of("Outline.vert", "Outline.frag"));
        }
    }

    @ReflectShader
    public static class PhongShininessMapShader extends Shader {
        public PhongShininessMapShader() {
            super(List.of("PhongShininessMap.vert", "PhongShininessMap.frag"));
        }
    }

    @ReflectShader
    public static class ScreenQuadShader extends Shader {
        public ScreenQuadShader() {
            super(List.of("ScreenQuad.vert", "ScreenQuad.frag"));
        }
    }

    @ReflectShader
    public static class PostprocessingShader extends Shader {
        public PostprocessingShader() {
            super(List.of("Postprocessing.vert", "Postprocessing.frag"));
        }
    }

    @ReflectShader
    public static class CubeMapShader extends Shader {
        public CubeMapShader() {
            super(List.of("CubeMap.vert", "CubeMap.frag"));
        }
    }

    @ReflectShader
    public static class ShowNormalsShader extends Shader {
        public ShowNormalsShader() {
            super(List.of("ShowNormals.vert", "ShowNormals.geom", "ShowNormals.frag"));
        }
    }

    @ReflectShader
    public static class DepthShader extends Shader {
        public DepthShader() {
            super(List.of("Depth.vert", "Depth.frag"));
        }
    }

    @ReflectShader
    public static class DepthCubeMapShader extends Shader {
        public DepthCubeMapShader() {
            super(List.of("DepthCubeMap.vert", "DepthCubeMap.geom", "DepthCubeMap.frag"));
        }
    }

    @ReflectShader
    public static class WireframeShader extends Shader {
        public WireframeShader() {
            super(List.of("Wireframe.glsl"));
        }
    }

}
