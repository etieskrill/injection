package org.etieskrill.engine.graphics.gl;

import org.etieskrill.engine.graphics.assimp.Mesh;
import org.etieskrill.engine.graphics.assimp.Model;
import org.etieskrill.engine.graphics.texture.Texture;

public class Loaders {
    
    public static final class TextureLoader extends Loader<Texture> {
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
    
    public static final class ModelLoader extends Loader<Model> {
        private static ModelLoader instance;
    
        public static ModelLoader getInstance() {
            if (instance == null)
                instance = new ModelLoader();
            return instance;
        }
    
        @Override
        public Model get(String name) {
            logger.debug("Creating new instance of model {} via copy", name);
            return new Model(super.get(name));
        }
    
        @Override
        protected String getLoaderName() {
            return "Model";
        }
    }
    
}
