package org.etieskrill.engine.graphics.model;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.etieskrill.engine.Disposable;
import org.etieskrill.engine.entity.component.Transform;
import org.etieskrill.engine.graphics.animation.Animation;
import org.etieskrill.engine.graphics.texture.Texture2D;
import org.jetbrains.annotations.NotNull;
import org.joml.primitives.AABBf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

import static org.etieskrill.engine.graphics.model.loader.Loader.loadModel;
import static org.etieskrill.engine.graphics.model.loader.MeshProcessor.optimiseMesh;

//TODO refactor: reduce to data in anticipation of ces
//               find most comprehensive solution for multi-entry-point builders
public class Model implements Disposable {

    private static final Logger logger = LoggerFactory.getLogger(Model.class);

    private final @Getter List<Node> nodes; //TODO flatten hierarchy by compatible/identical materials
    private final List<Material> materials; //TODO since meshes know their materials, these here may not be necessary?

    private final @Getter List<Animation> animations;
    private final @Getter List<Bone> bones;

    private final @Getter AABBf boundingBox;

    private final @Getter String name;

    private final boolean culling;
    private final boolean transparency;

    public static class Builder {
        protected final @Getter String file;
        protected @Getter @Setter @Accessors(chain = true) String name;

        protected final @Getter List<Node> nodes;
        protected final @Getter List<Mesh> meshes;
        protected final @Getter List<Material> materials;
        protected final @Getter List<Animation> animations;
        protected final @Getter List<Bone> bones;

        protected final @Getter Map<String, Texture2D.Builder> embeddedTextures;

        protected @Getter @Setter @Accessors(chain = true) boolean flipUVs = true;
        protected @Getter @Setter @Accessors(chain = true) boolean flipWinding = false;
        protected @Setter @Accessors(chain = true) boolean culling = true;
        protected @Getter @Setter @Accessors(fluent = true) boolean hasTransparency = false;

        protected @Getter @Setter @Accessors(chain = true) Transform initialTransform = new Transform();

        protected @Getter @Setter AABBf boundingBox;

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

        public Builder removeMaterials() {
            this.materials.clear();
            return this;
        }

        //TODO add refractive toggle/mode (NONE, GLASS, WATER etc.)

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
    }

    private Model(Builder builder) {
        if (builder.getInitialTransform() != null) { //FIXME ugly solution to preserve non-malleability after builder - separate builder stage, constructor argument, or make class malleable and introduce ... "ModelRenderInstance"??
            var transform = ((Transform) builder.getNodes().getFirst().getTransform());
            var initialTransform = new Transform(builder.getInitialTransform()).compose(transform);
            transform.set(initialTransform);

            builder.getBoundingBox().transform(builder.getInitialTransform().getMatrix());
        }

        this.nodes = Collections.unmodifiableList(builder.nodes);
        this.materials = Collections.unmodifiableList(builder.materials);
        this.animations = Collections.unmodifiableList(builder.animations);
        this.bones = Collections.unmodifiableList(builder.bones);
        this.boundingBox = builder.boundingBox;
        this.name = builder.name;

        this.culling = builder.culling;
        this.transparency = builder.hasTransparency;
    }

    public boolean doCulling() {
        return culling;
    }

    public boolean hasTransparency() {
        return transparency;
    }

    @Override
    public void dispose() {
        if (!nodes.isEmpty()) nodes.getFirst().dispose(); //Node trees dispose themselves recursively
        materials.forEach(Material::dispose);
    }

}
