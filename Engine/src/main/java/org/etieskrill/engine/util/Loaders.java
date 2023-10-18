package org.etieskrill.engine.util;

import org.etieskrill.engine.graphics.assimp.Mesh;
import org.etieskrill.engine.graphics.assimp.Model;
import org.etieskrill.engine.graphics.gl.shaders.ShaderProgram;
import org.etieskrill.engine.graphics.texture.AbstractTexture;

public final class Loaders {
    
    public static final class TextureLoader extends Loader<AbstractTexture> {
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
    
    public static final class MeshLoader extends Loader<Mesh> {
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
    public static final class ModelLoader extends Loader<Model> {
        private static ModelLoader instance;
    
        public static ModelLoader get() {
            if (instance == null)
                instance = new ModelLoader();
            return instance;
        }
    
        @Override
        public Model get(String name) {
            logger.debug("Creating new instance of model {}", name);
            return new Model(super.get(name));
        }
    
        @Override
        protected String getLoaderName() {
            return "Model";
        }
    }
    
    /**
     * Always returns the same instance for a given resource identifier.
     */
    public static final class ShaderLoader extends Loader<ShaderProgram> {
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
    
    public static void disposeDefaultLoaders() {
        TextureLoader.get().dispose();
        MeshLoader.get().dispose();
        ModelLoader.get().dispose();
        ShaderLoader.get().dispose();
    }
    
}
