package org.etieskrill.engine.entity.service.impl;

import org.etieskrill.engine.entity.Entity
import org.etieskrill.engine.entity.component.Drawable
import org.etieskrill.engine.entity.service.Service
import org.etieskrill.engine.graphics.animation.Animator
import kotlin.reflect.KClass

class AnimationService : Service {

    override fun canProcess(entity: Entity) = entity.hasComponents<Drawable, Animator>()

    override fun process(targetEntity: Entity, entities: List<Entity>, delta: Double) {
        val drawable = targetEntity.getComponent<Drawable>()!!
        val animator = targetEntity.getComponent<Animator>()!!

        animator.update(delta)

        drawable.shader?.setUniformArray("boneMatrices", animator.transformMatricesArray)
    }

    override val runBefore: Set<KClass<out Service>>
        get() = setOf(RenderService::class)

}
