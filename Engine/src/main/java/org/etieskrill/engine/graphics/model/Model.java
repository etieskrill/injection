package org.etieskrill.engine.graphics.model;

import lombok.Getter;
import lombok.Setter;
import org.etieskrill.engine.Disposable;
import org.etieskrill.engine.entity.component.Transform;
import org.etieskrill.engine.entity.component.TransformC;
import org.etieskrill.engine.graphics.animation.Animation;
import org.etieskrill.engine.graphics.texture.Texture2D;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.joml.primitives.AABBf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.function.Supplier;

import static org.etieskrill.engine.graphics.model.loader.Loader.loadModel;
import static org.etieskrill.engine.graphics.model.loader.MeshProcessor.optimiseMesh;

//TODO refactor: reduce to data in anticipation of ces
//               find most comprehensive solution for multi-entry-point builders
public class Model implements Disposable {

    private static final Supplier<Model> ERROR_MODEL = () -> new Builder("cube.obj").build();

    private static final Logger logger = LoggerFactory.getLogger(Model.class);

    @Getter
    private final List<Node> nodes;
    private final List<Material> materials; //TODO since meshes know their materials, these here may not be necessary?

    @Getter
    private final List<Animation> animations;
    @Getter
    private final List<Bone> bones;

    @Getter
    private final AABBf boundingBox;

    @Getter
    private final String name;

    private final Transform transform;
    @Getter
    private final Transform initialTransform;
    private final Transform finalTransform;

    private final boolean culling;
    private final boolean transparency;

    //TODO move to entity eventually
    @Getter
    private boolean enabled;

    public static class Builder {
        @Getter
        protected final String file;
        @Getter
        protected String name;

        @Getter
        protected final List<Node> nodes;
        @Getter
        protected final List<Mesh> meshes;
        @Getter
        protected final List<Material> materials;
        @Getter
        protected final List<Animation> animations;
        @Getter
        protected final List<Bone> bones;

        @Getter
        protected final Map<String, Texture2D.Builder> embeddedTextures;

        protected boolean flipUVs = true;
        protected boolean flipWinding = false;
        protected boolean culling = true;
        protected boolean transparency = false;

        protected Transform transform = new Transform();
        protected Transform initialTransform = new Transform();

        @Setter
        protected AABBf boundingBox;

        public Builder(@NotNull String file) {
            if (file.isBlank()) throw new IllegalArgumentException("File name cannot be blank");
            if (file.contains("/"))
                throw new IllegalArgumentException("Custom folder structure not implemented yet: " + file);
            this.file = file;
            this.name = file.split("\\.")[0];

            this.nodes = new ArrayList<>();
            this.meshes = new ArrayList<>();
            this.materials = new LinkedList<>();
            this.animations = new LinkedList<>();
            this.bones = new ArrayList<>();
            this.embeddedTextures = new HashMap<>();

            loadModelData();
        }

        protected void loadModelData() {
            try {
                logger.debug("Loading model {} from file {}", name, file);
                loadModel(this);
            } catch (IOException e) {
                logger.warn("Could not load model {} from file {}", name, file, e);
            }
        }

        public void addNodes(Node... nodes) {
            addNodes(List.of(nodes));
        }

        public void addNodes(List<Node> nodes) {
            this.nodes.addAll(nodes);
        }

        public Builder setMaterials(Material... materials) {
            this.materials.clear();
            this.materials.addAll(List.of(materials));
            return this;
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

        public Builder optimiseMeshes() {
            return optimiseMeshes(5000, 0.001f);
        }

        public Builder optimiseMeshes(int targetIndexCount, float maxDeformation) {
            for (Mesh mesh : meshes) {
                optimiseMesh(mesh, targetIndexCount, maxDeformation);
            }
            return this;
        }

        public @NotNull Model build() {
            return new Model(this);
        }
    }

    public static class MemoryBuilder extends Builder {
        public MemoryBuilder(@NotNull String name) {
            super(name);
        }

        @Override
        protected void loadModelData() {
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
        this.finalTransform = new Transform(model.finalTransform);

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
        this.finalTransform = new Transform();

        enable();
    }

    @Deprecated(forRemoval = true)
    public Transform getTransform() {
        return transform;
    }

    //TODO what is this operation called? composition? move to transform
    @Contract("-> this")
    @Deprecated
    public TransformC getFinalTransform() {
        return finalTransform.set(transform)
                .translate(initialTransform.getPosition())
                .applyRotation(quat -> quat.mul(initialTransform.getRotation()))
                .applyScale(scale -> scale.mul(initialTransform.getScale()));
    }

    public boolean doCulling() {
        return culling;
    }

    public boolean hasTransparency() {
        return transparency;
    }

    public void enable() {
        enabled = true;
    }

    public void disable() {
        enabled = false;
    }

    @Override
    public void dispose() {
        if (!nodes.isEmpty()) nodes.getFirst().dispose(); //Node trees dispose themselves recursively
        materials.forEach(Material::dispose);
    }

}
