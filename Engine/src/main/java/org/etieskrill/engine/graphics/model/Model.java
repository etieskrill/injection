package org.etieskrill.engine.graphics.model;

import org.etieskrill.engine.Disposable;
import org.etieskrill.engine.entity.data.AABB;
import org.etieskrill.engine.entity.data.Transform;
import org.etieskrill.engine.graphics.animation.Animation;
import org.etieskrill.engine.graphics.texture.Texture2D;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.function.Supplier;

import static org.etieskrill.engine.graphics.model.loader.Loader.loadModel;

//TODO refactor: loading in separate class / classes
//               reduce to data in anticipation of ces
//               find most comprehensive solution for multi-entry-point builders
public class Model implements Disposable {
    
    private static final Supplier<Model> ERROR_MODEL = () -> new Builder("cube.obj").build();
    
    private static final Logger logger = LoggerFactory.getLogger(Model.class);
    
    private final List<Mesh> meshes; //TODO these should become immutable after model instantiation
    private final List<Material> materials; //TODO since meshes know their materials, these here may not be necessary?

    private final List<Animation> animations;

    private final AABB boundingBox;
    
    private final String name;
    
    private final Transform transform;
    
    private final boolean culling;
    private final boolean transparency;
    
    //TODO move to entity eventually
    private boolean enabled;
    
    public static class Builder {
        protected final String file;
        protected String name;

        protected final List<Mesh> meshes;
        protected final List<Material> materials;
        protected final List<Animation> animations;

        protected final Map<String, Texture2D.Builder> embeddedTextures;

        protected boolean flipUVs = true;
        protected boolean flipWinding = false;
        protected boolean culling = true;
        protected boolean transparency = false;
        
        protected Transform transform = Transform.getBlank();

        protected AABB boundingBox;
        
        public Builder(String file) {
            if (file.isBlank()) throw new IllegalArgumentException("File name cannot be blank");
            if (file.contains("/")) throw new IllegalArgumentException("Custom folder structure not implemented yet: " + file);
            this.file = file;
            this.name = file.split("\\.")[0];

            this.meshes = new LinkedList<>();
            this.materials = new LinkedList<>();
            this.animations = new LinkedList<>();
            this.embeddedTextures = new HashMap<>();
        }

        public String getFile() {
            return file;
        }

        public String getName() {
            return name;
        }

        //TODO actually integrate this & material setter into loading process
        public Builder setMeshes(Mesh... meshes) {
            this.meshes.clear();
            this.meshes.addAll(List.of(meshes));
            return this;
        }

        public List<Mesh> getMeshes() {
            return meshes;
        }

        public Builder setMaterials(Material... materials) {
            this.materials.clear();
            this.materials.addAll(List.of(materials));
            return this;
        }

        public List<Material> getMaterials() {
            return materials;
        }

        public List<Animation> getAnimations() {
            return animations;
        }

        public Map<String, Texture2D.Builder> getEmbeddedTextures() {
            return embeddedTextures;
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder removeMaterials() {
            this.materials.clear();
            return this;
        }

        public Builder setFlipUVs(boolean flipUVs) {
            this.flipUVs = flipUVs;
            return this;
        }

        public boolean shouldFlipUVs() {
            return flipUVs;
        }

        public Builder setFlipWinding(boolean flipWinding) {
            this.flipWinding = flipWinding;
            return this;
        }

        public boolean shouldFlipWinding() {
            return flipWinding;
        }

        public Builder disableCulling() {
            this.culling = false;
            return this;
        }
    
        public Builder hasTransparency() {
            transparency = true;
            return this;
        }
        
        //TODO add refractive toggle/mode (NONE, GLASS, WATER etc.)
        
        public Builder setTransform(Transform transform) {
            this.transform = transform;
            return this;
        }

        public void setBoundingBox(AABB boundingBox) {
            this.boundingBox = boundingBox;
        }

        public Model build() {
            try {
                return new Model(this);
            } catch (IOException e) {
                logger.info("Exception while loading model, using default: ", e);
                return ERROR_MODEL.get();
            }
        }
    }
    
    //TODO this is a very very very Very VERY temporary solution, i can hardly look at it
    public static class MemoryBuilder extends Builder {
        public MemoryBuilder(String name) {
            super(name);
        }
    
        @Override
        public Model build() {
            return new Model(this);
        }
    }
    
    public static Model ofFile(String file) {
        return ofFile(file, true);
    }
    
    public static Model ofFile(String file, boolean flipUVs) {
        return new Builder(file).setFlipUVs(flipUVs).build();
    }
    
    /**
     * Copy constructor, for ... not cloning.
     */
    public Model(Model model) {
        logger.trace("Creating copy of model {}", model.name);

        //TODO since the below three lines represent the model as loaded into the graphics memory
        // and should effectively be immutable, consider encapsulating them into another class
        this.meshes = model.meshes;
        this.materials = model.materials;
        this.animations = model.animations;
        this.boundingBox = model.boundingBox;
        this.name = model.name;

        this.transform = new Transform(model.transform);

        this.culling = model.culling;
        this.transparency = model.transparency;
        
        this.enabled = model.enabled;
    }
    
    private Model(Builder builder) throws IOException {
        logger.debug("Loading model {} from file {}", builder.name, builder.file);
        loadModel(builder);

        this.meshes = Collections.unmodifiableList(builder.meshes);
        this.materials = Collections.unmodifiableList(builder.materials);
        this.animations = Collections.unmodifiableList(builder.animations);
        this.boundingBox = builder.boundingBox;
        this.name = builder.name;
    
        this.culling = builder.culling;
        this.transparency = builder.transparency;
        
        this.transform = builder.transform;
        
        enable();
    }
    
    private Model(MemoryBuilder builder) {
        logger.debug("Loading model {} from memory", builder.name);

        this.meshes = Collections.unmodifiableList(builder.meshes);
        this.materials = Collections.unmodifiableList(builder.materials);
        this.animations = Collections.unmodifiableList(builder.animations);
        this.boundingBox = builder.boundingBox;
        this.name = builder.name;

        this.culling = builder.culling;
        this.transparency = builder.transparency;

        this.transform = builder.transform;
        
        enable();
    }
    
    public AABB getBoundingBox() {
        return boundingBox;
    }
    
    public AABB getWorldBoundingBox() {
        Matrix4f worldTransform = this.transform.toMat();
        return new AABB(worldTransform.transformPosition(new Vector3f(boundingBox.getMin())),
                worldTransform.transformPosition(new Vector3f(boundingBox.getMax())));
    }
    
    public String getName() {
        return name;
    }
    
    public List<Mesh> getMeshes() {
        return meshes;
    }

    public List<Animation> getAnimations() {
        return animations;
    }

    public Transform getTransform() {
        return transform;
    }
    
    public boolean doCulling() {
        return culling;
    }
    
    public boolean hasTransparency() {
        return transparency;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void enable() {
        enabled = true;
    }
    
    public void disable() {
        enabled = false;
    }

    @Override
    public void dispose() {
        meshes.forEach(Mesh::dispose);
        materials.forEach(Material::dispose);
    }
    
}
