package org.etieskrill.engine.graphics.model;

import org.etieskrill.engine.common.Disposable;
import org.etieskrill.engine.entity.component.Transform;
import org.etieskrill.engine.graphics.animation.Animation;
import org.jetbrains.annotations.NotNull;
import org.joml.primitives.AABBf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static org.etieskrill.engine.graphics.model.loader.Loader.loadModel;
import static org.etieskrill.engine.graphics.model.loader.MeshProcessorKt.optimiseMesh;

//TODO refactor: reduce to data in anticipation of ces
//               find most comprehensive solution for multi-entry-point builders
public class Model implements Disposable {

    private static final Logger logger = LoggerFactory.getLogger(Model.class);

    private final List<Node> nodes; //TODO flatten hierarchy by compatible/identical materials
    private final List<Material> materials; //TODO since meshes know their materials, these here may not be necessary?

    private final List<Animation> animations;
    private final List<Bone> bones;

    private final AABBf boundingBox;

    private final String name;

    private final boolean culling;
    private final boolean transparency;

    public static class Builder {
        protected final String file;
        protected String name;

        protected final List<Node> nodes;
        protected final List<Mesh> meshes;
        protected final List<Material> materials;
        protected final List<Animation> animations;
        protected final List<Bone> bones;

        protected boolean flipUVs = true;
        protected boolean flipWinding = false;
        protected boolean culling = true;
        protected boolean hasTransparency = false;

        protected Transform initialTransform = new Transform();

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

        public String getFile() {
            return file;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<Node> getNodes() {
            return nodes;
        }

        public List<Mesh> getMeshes() {
            return meshes;
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

        public boolean isFlipUVs() {
            return flipUVs;
        }

        public void setFlipUVs(boolean flipUVs) {
            this.flipUVs = flipUVs;
        }

        public boolean isFlipWinding() {
            return flipWinding;
        }

        public void setFlipWinding(boolean flipWinding) {
            this.flipWinding = flipWinding;
        }

        public boolean isCulling() {
            return culling;
        }

        public void setCulling(boolean culling) {
            this.culling = culling;
        }

        public boolean isHasTransparency() {
            return hasTransparency;
        }

        public void setHasTransparency(boolean hasTransparency) {
            this.hasTransparency = hasTransparency;
        }

        public Transform getInitialTransform() {
            return initialTransform;
        }

        public void setInitialTransform(Transform initialTransform) {
            this.initialTransform = initialTransform;
        }

        public AABBf getBoundingBox() {
            return boundingBox;
        }

        public void setBoundingBox(AABBf boundingBox) {
            this.boundingBox = boundingBox;
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
        var builder = new Builder(file);
        builder.setFlipUVs(flipUVs);
        return builder.build();
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

    public List<Node> getNodes() {
        return this.nodes;
    }

    public List<Animation> getAnimations() {
        return this.animations;
    }

    public List<Bone> getBones() {
        return this.bones;
    }

    public AABBf getBoundingBox() {
        return this.boundingBox;
    }

    public String getName() {
        return this.name;
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
