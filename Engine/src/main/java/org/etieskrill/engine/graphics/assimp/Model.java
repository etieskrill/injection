package org.etieskrill.engine.graphics.assimp;

import glm_.mat4x4.Mat4;
import glm_.vec4.Vec4;
import org.etieskrill.engine.Disposable;
import org.etieskrill.engine.entity.data.AABB;
import org.etieskrill.engine.entity.data.Transform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.function.Supplier;

//TODO refactor: loading in separate class / classe`s
//               reduce to data in anticipation of ces
//               find most comprehensive solution for multi-entry-point builders
public class Model implements Disposable {

    private static final Supplier<Model> ERROR_MODEL = () -> new Builder("cube.obj").build();
    
    private static final Logger logger = LoggerFactory.getLogger(Model.class);
    
    private final List<Mesh> meshes; //TODO these should become immutable after model instantiation
    private final List<Material> materials; //TODO since meshes know their materials, these here may not be necessary?
    
    private final AABB boundingBox;
    
    private final String name;
    
    private final Transform transform;
    
    private final boolean culling;
    private final boolean transparency;
    
    //TODO move to entity eventually
    private boolean enabled;
    
    public static class Builder {
        protected final List<Mesh> meshes = new LinkedList<>();
        protected final List<Material> materials = new LinkedList<>();
    
        public AABB boundingBox;
        
        protected final String file;
        protected String name;
    
        protected boolean flipUVs = true;
        protected boolean flipWinding = false;
        protected boolean culling = true;
        protected boolean transparency = false;
        
        protected Transform transform = Transform.getBlank();
        
        public Builder(String file) {
            if (file.isBlank()) throw new IllegalArgumentException("File name cannot be blank");
            if (file.contains("/")) throw new IllegalArgumentException("Custom folder structure not implemented yet: " + file);
            
            this.file = file;
            this.name = file.split("\\.")[0];
        }
    
        //TODO actually integrate this & material setter into loading process
        public Builder setMeshes(Mesh... meshes) {
            this.meshes.clear();
            this.meshes.addAll(List.of(meshes));
            return this;
        }
    
        public Builder setMaterials(Material... materials) {
            this.materials.clear();
            this.materials.addAll(List.of(materials));
            return this;
        }
        
        public Builder removeMaterials() {
            this.materials.clear();
            return this;
        }
    
        public Builder setName(String name) {
            this.name = name;
            return this;
        }
        
        public Builder setFlipUVs(boolean flipUVs) {
            this.flipUVs = flipUVs;
            return this;
        }
    
        public Builder setFlipWinding(boolean flipWinding) {
            this.flipWinding = flipWinding;
            return this;
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
            Model model = null;
            try {
                model = new Model(this);
            } catch (IOException ignored) {} //cannot occur,
            return model;
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
        //TODO since the below three lines represent the model as loaded into the graphics memory
        // and should effectively be immutable, consider encapsulating them into another class
        this.meshes = model.meshes;
        this.materials = model.materials;
        this.boundingBox = model.boundingBox;
        this.name = model.name;
        
        logger.trace("Creating copy of model {}", name);
        
        this.transform = new Transform(model.transform);
        this.culling = model.culling;
        this.transparency = model.transparency;
        
        this.enabled = model.enabled;
    }
    
    private Model(Builder builder) throws IOException {
        this.meshes = builder.meshes;
        this.materials = builder.materials;
        
        this.name = builder.name;
    
        this.culling = builder.culling;
        this.transparency = builder.transparency;

        if (builder instanceof MemoryBuilder) {
            logger.debug("Loading model {} from memory", name);
        } else {
            logger.debug("Loading model {} from file {}", name, builder.file);
            ModelLoader.loadModel(builder);
        }

        this.boundingBox = builder.boundingBox;

        this.transform = builder.transform;
        
        enable();
    }
    
    public AABB getBoundingBox() {
        return boundingBox;
    }
    
    public AABB getWorldBoundingBox() {
        Mat4 transform = this.transform.toMat();
        return new AABB(transform.times(new Vec4(boundingBox.getMin())).toVec3(),
                        transform.times(new Vec4(boundingBox.getMax())).toVec3());
    }
    
    public String getName() {
        return name;
    }
    
    public List<Mesh> getMeshes() {
        return meshes;
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

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
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
