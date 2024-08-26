package org.etieskrill.engine.graphics.particle;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import lombok.experimental.SuperBuilder;
import org.etieskrill.engine.entity.component.Transform;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@Getter
@SuperBuilder
@AllArgsConstructor
public class ParticleRoot {

    @Builder.Default
    protected final Transform transform = new Transform();
    @Singular
    protected final List<@NotNull ParticleEmitter> emitters;

    public abstract static class ParticleRootBuilder<C extends ParticleRoot, B extends ParticleRootBuilder<C, B>> {
        public Transform getTransform() {
            return transform$value;
        }

        public ArrayList<ParticleEmitter> getEmitters() {
            return emitters;
        }
    }

}
