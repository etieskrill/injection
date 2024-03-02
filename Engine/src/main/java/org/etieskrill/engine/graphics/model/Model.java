package org.etieskrill.engine.graphics.model;

import org.etieskrill.engine.Disposable;
import org.etieskrill.engine.entity.data.AABB;
import org.etieskrill.engine.entity.data.Transform;
import org.etieskrill.engine.graphics.animation.Animation;
import org.etieskrill.engine.graphics.texture.Texture2D;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.function.Supplier;

import static org.etieskrill.engine.graphics.model.loader.Loader.loadModel;

//TODO refactor: reduce to data in anticipation of ces
//               find most comprehensive solution for multi-entry-point builders
public class Model implements Disposable {
    
    private static final Supplier<Model> ERROR_MODEL = () -> new Builder("cube.obj").build();
    
    private static final Logger logger = LoggerFactory.getLogger(Model.class);

    private final List<Node> nodes;
    private final List<Material> materials; //TODO since meshes know their materials, these here may not be necessary?

    private final List<Animation> animations;
    private final List<Bone> bones;

    private final AABB boundingBox;
    
    private final String name;
    
    private final Transform transform;
    private final Transform initialTransform;

    private final boolean culling;
    private final boolean transparency;
    
    //TODO move to entity eventually
    private boolean enabled;
    
    public static class Builder {
        protected final String file;
        protected String name;

        protected final List<Node> nodes;
        protected final List<Mesh> meshes;
        protected final List<Material> materials;
        protected final List<Animation> animations;
        protected final List<Bone> bones;

        protected final Map<String, Texture2D.Builder> embeddedTextures;

        protected boolean flipUVs = true;
        protected boolean flipWinding = false;
        protected boolean culling = true;
        protected boolean transparency = false;

        protected Transform transform = new Transform();
        protected Transform initialTransform = new Transform();

        protected AABB boundingBox;
        
        public Builder(@NotNull String file) {
            if (file.isBlank()) throw new IllegalArgumentException("File name cannot be blank");
            if (file.contains("/")) throw new IllegalArgumentException("Custom folder structure not implemented yet: " + file);
            this.file = file;
            this.name = file.split("\\.")[0];

            this.nodes = new ArrayList<>();
            this.meshes = new ArrayList<>();
            this.materials = new LinkedList<>();
            this.animations = new LinkedList<>();
            this.bones = new ArrayList<>();
            this.embeddedTextures = new HashMap<>();
        }

        public String getFile() {
            return file;
        }

        public String getName() {
            return name;
        }

        public List<Node> getNodes() {
            return nodes;
        }

        public void addNodes(Node... nodes) {
            addNodes(List.of(nodes));
        }

        public void addNodes(List<Node> nodes) {
            this.nodes.addAll(nodes);
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

        public List<Bone> getBones() {
            return bones;
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

        public Builder setInitialTransform(Transform initialTransform) {
            this.initialTransform = initialTransform;
            return this;
        }

        public void setBoundingBox(AABB boundingBox) {
            this.boundingBox = boundingBox;
        }

        public @NotNull Model build() {
            try {
                logger.debug("Loading model {} from file {}", name, file);
                loadModel(this);
                return new Model(this);
            } catch (IOException e) {
                logger.info("Exception while loading model, using default: ", e);
                return ERROR_MODEL.get();
            }
        }
    }
    
    public static class MemoryBuilder extends Builder {
        public MemoryBuilder(@NotNull String name) {
            super(name);
        }
    
        @Override
        public @NotNull Model build() {
            logger.debug("Loading model {} from memory", name);
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

        //TODO since the below couple of lines represent the model as loaded into the graphics memory
        // and should effectively be immutable, consider encapsulating them into another class
        //Shared resources
        this.nodes = model.nodes;
        this.materials = model.materials;
        this.animations = model.animations;
        this.bones = model.bones;
        this.boundingBox = model.boundingBox;
        this.name = model.name;

        this.culling = model.culling;
        this.transparency = model.transparency;

        //Instance resources
        this.transform = new Transform(model.transform);
        this.initialTransform = new Transform(model.initialTransform);

        this.enabled = model.enabled;
    }
    
    private Model(Builder builder) {
        this.nodes = Collections.unmodifiableList(builder.nodes);
        this.materials = Collections.unmodifiableList(builder.materials);
        this.animations = Collections.unmodifiableList(builder.animations);
        this.bones = Collections.unmodifiableList(builder.bones);
        this.boundingBox = builder.boundingBox;
        this.name = builder.name;
    
        this.culling = builder.culling;
        this.transparency = builder.transparency;
        
        this.transform = new Transform(builder.transform);
        this.initialTransform = new Transform(builder.initialTransform);

        enable();
    }
    
    public AABB getBoundingBox() {
        return boundingBox;
    }
    
    public AABB getWorldBoundingBox() {
        Matrix4f worldTransform = new Matrix4f(getFinalTransform().getMatrix());
        return new AABB(worldTransform.transformPosition(new Vector3f(boundingBox.getMin())),
                worldTransform.transformPosition(new Vector3f(boundingBox.getMax())));
    }
    
    public String getName() {
        return name;
    }

    public List<Node> getNodes() {
        return nodes;
    }

    public List<Animation> getAnimations() {
        return animations;
    }

    public List<Bone> getBones() {
        return bones;
    }

    public Transform getTransform() {
        return transform;
    }

    public Transform getInitialTransform() {
        return initialTransform;
    }

    @Contract("-> new")
    public Transform getFinalTransform() {
        //return transform.apply(initialTransform, new Transform());
        return new Transform(transform).apply(initialTransform);
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
        nodes.getFirst().dispose(); //Node trees dispose themselves recursively
        materials.forEach(Material::dispose);
    }
    
}
