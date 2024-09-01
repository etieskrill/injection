package org.etieskrill.engine.entity.service.impl;

import org.etieskrill.engine.entity.Entity;
import org.etieskrill.engine.entity.component.Drawable;
import org.etieskrill.engine.entity.service.Service;
import org.etieskrill.engine.graphics.animation.Animator;

import java.util.List;
import java.util.Set;

public class AnimationService implements Service {

    @Override
    public boolean canProcess(Entity entity) {
        return entity.hasComponents(Drawable.class, Animator.class);
    }

    @Override
    public void process(Entity targetEntity, List<Entity> entities, double delta) {
        Drawable drawable = targetEntity.getComponent(Drawable.class);
        Animator animator = targetEntity.getComponent(Animator.class);

        animator.update(delta);

        if (drawable.getShader() != null) {
            drawable.getShader().setUniformArray("boneMatrices", animator.getTransformMatrices().toArray());
        }
    }

    @Override
    public Set<Class<? extends Service>> runBefore() {
        return Set.of(RenderService.class);
    }

}
