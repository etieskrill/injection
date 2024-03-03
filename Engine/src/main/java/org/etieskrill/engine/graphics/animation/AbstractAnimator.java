package org.etieskrill.engine.graphics.animation;

import org.etieskrill.engine.entity.data.Transform;
import org.etieskrill.engine.entity.data.TransformC;
import org.etieskrill.engine.graphics.model.Bone;
import org.etieskrill.engine.graphics.model.Model;
import org.jetbrains.annotations.UnmodifiableView;
import org.joml.Matrix4fc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.etieskrill.engine.graphics.animation.Animation.MAX_BONES;

public abstract class AbstractAnimator {

    protected final Model model;

    protected boolean playing;
    protected double playbackSpeed;

    @UnmodifiableView
    protected final List<Transform> boneTransforms;

    private static final Logger logger = LoggerFactory.getLogger(AbstractAnimator.class);

    public AbstractAnimator(Model model) {
        this.model = model;

        this.playing = false;
        this.playbackSpeed = 1;

        List<Transform> boneTransforms = new ArrayList<>(MAX_BONES);
        for (int i = 0; i < MAX_BONES; i++) boneTransforms.add(new Transform());
        this.boneTransforms = Collections.unmodifiableList(boneTransforms);
    }

    public abstract void play();
    public abstract void stop();

    public void switchPlaying() {
        if (isPlaying()) stop();
        else play();
    }

    public boolean isPlaying() {
        return playing;
    }

    public abstract void update(double delta);

    public List<TransformC> getBoneTransforms() {
        return boneTransforms.stream().map(transform -> (TransformC) transform).toList();
    }

    public List<Matrix4fc> getBoneTransformMatrices() {
        return boneTransforms.stream().map(TransformC::getMatrix).toList();
    }

    public Model getModel() {
        return model;
    }

    public void setPlaybackSpeed(double speed) {
        this.playbackSpeed = speed;
    }

    protected static void validateBonesInModel(Animation animation, Model model) { //TODO this should happen only once while loading the data
        List<Bone> bones = animation.getBoneAnimations().stream()
                .map(BoneAnimation::bone)
                .toList();

        if (bones.stream().anyMatch(bone -> !model.getBones().contains(bone))) {
            throw new IllegalArgumentException("Animation contains bones which are not present in the model");
        }

        logger.atTrace().log(() -> {
            List<Bone> nonAnimatedBones = model.getBones().stream()
                    .filter(bone -> !bones.contains(bone))
                    .toList();
            if (!nonAnimatedBones.isEmpty())
                return "Bones contain no animation data: " + nonAnimatedBones;
            else
                return "All bones are animated";
        });
    }

}
