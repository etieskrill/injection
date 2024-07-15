package org.etieskrill.engine.util;

import org.etieskrill.engine.graphics.animation.Animation;
import org.etieskrill.engine.graphics.gl.shader.ShaderProgram;
import org.etieskrill.engine.graphics.model.Mesh;
import org.etieskrill.engine.graphics.model.Model;
import org.etieskrill.engine.graphics.texture.AbstractTexture;
import org.etieskrill.engine.graphics.texture.font.Font;
import org.jetbrains.annotations.Nullable;

public final class Loaders {

    public static final class TextureLoader extends DisposableLoader<AbstractTexture> {
        private static TextureLoader instance;

        //TODO kinda-not-very-singleton-at-all, is this valid?
        public static TextureLoader get() {
            if (instance == null)
                instance = new TextureLoader();
            return instance;
        }

        @Override
        protected String getLoaderName() {
            return "Texture";
        }
    }

    public static final class MeshLoader extends DisposableLoader<Mesh> {
        private static MeshLoader instance;

        public static MeshLoader get() {
            if (instance == null)
                instance = new MeshLoader();
            return instance;
        }

        @Override
        protected String getLoaderName() {
            return "Mesh";
        }
    }

    /**
     * Creates a new instance for every call to {@link #get(String name)}, where data in graphics memory - such as model
     * and material data - are shared between models, but not instance fields such as transform.
     */
    public static final class ModelLoader extends DisposableLoader<Model> {
        private static ModelLoader instance;

        public static ModelLoader get() {
            if (instance == null)
                instance = new ModelLoader();
            return instance;
        }

        @Override
        public @Nullable Model get(String name) {
            Model model = super.get(name);
            if (model == null) return null;
            logger.debug("Creating new instance of model {}", name);
            return new Model(model);
        }

        @Override
        protected String getLoaderName() {
            return "Model";
        }
    }

    /**
     * Always returns the same instance for a given resource identifier.
     */
    public static final class ShaderLoader extends DisposableLoader<ShaderProgram> {
        private static ShaderLoader instance;

        public static ShaderLoader get() {
            if (instance == null)
                instance = new ShaderLoader();
            return instance;
        }

        @Override
        protected String getLoaderName() {
            return "Shader";
        }
    }

    public static final class FontLoader extends DisposableLoader<Font> {
        private static FontLoader instance;

        public static FontLoader get() {
            if (instance == null)
                instance = new FontLoader();
            return instance;
        }

        @Override
        protected String getLoaderName() {
            return "Font";
        }
    }

    public static final class AnimationLoader extends Loader<Animation> {
        private static AnimationLoader instance;

        public static AnimationLoader get() {
            if (instance == null) {
                instance = new AnimationLoader();
            }
            return instance;
        }

        @Override
        protected String getLoaderName() {
            return "Animation";
        }
    }

    public static void disposeDefaultLoaders() {
        TextureLoader.get().dispose();
        MeshLoader.get().dispose();
        ModelLoader.get().dispose();
        ShaderLoader.get().dispose();
        FontLoader.get().dispose();
    }

}
