package org.etieskrill.engine.entity.service.impl

import net.bifreus.kml.Vec3
import org.etieskrill.engine.entity.Entity
import org.etieskrill.engine.entity.component.Acceleration
import org.etieskrill.engine.entity.component.DirectionalForceComponent
import org.etieskrill.engine.entity.component.DynamicCollider
import org.etieskrill.engine.entity.component.Friction
import org.etieskrill.engine.entity.component.OnGround
import org.etieskrill.engine.entity.component.StaticCollider
import org.etieskrill.engine.entity.component.Transform
import org.etieskrill.engine.entity.component.WorldSpaceAABB
import org.etieskrill.engine.entity.service.Service

class PhysicsService(
    private val solver: NarrowCollisionSolver
) : Service {

    private var lastDelta = 0.0
    private var firstCall = true

    override fun canProcess(entity: Entity) = entity.hasComponents<Transform, DynamicCollider, WorldSpaceAABB>()

    override fun process(targetEntity: Entity, entities: List<Entity>, delta: Double) {
        if (firstCall) {
            lastDelta = delta
            firstCall = false
        }

        val transform = targetEntity.getComponent<Transform>()!!
        val collider = targetEntity.getComponent<DynamicCollider>()!!
        val onGround = targetEntity.getComponent<OnGround>()

        updatePosition(targetEntity, transform, collider, onGround, delta)

        solveCollisions(entities, targetEntity, transform, collider, onGround)
    }

    private fun updatePosition(
        targetEntity: Entity,
        transform: Transform,
        collider: DynamicCollider,
        onGround: OnGround?,
        delta: Double
    ) {
        var velocity = transform.position - collider.previousPosition

        targetEntity.getComponent<Friction>()?.let {
            val verticalVelocity = velocity.y
            velocity *= 1f - (it.coefficient * delta).toFloat()
            velocity.y = verticalVelocity
        }

        if (lastDelta > 0.0) {
            velocity *= (delta / lastDelta).toFloat()
        }

        var newPosition = velocity + transform.position

        val correctedAccelDelta = (delta * ((delta + lastDelta) / 2.0)).toFloat()

        targetEntity.getComponent<DirectionalForceComponent>()?.let {
            newPosition += it.force * correctedAccelDelta
        }

        targetEntity.getComponent<Acceleration>()?.let {
            if (onGround?.isOnGround != true) {
                it.force.y = 0f
            }
            newPosition += it.force * it.factor * correctedAccelDelta
        }

        lastDelta = delta
        collider.previousPosition(transform.position)
        transform.position = newPosition
    }

    private fun solveCollisions(
        entities: List<Entity>,
        targetEntity: Entity,
        transform: Transform,
        collider: DynamicCollider,
        onGround: OnGround?
    ) {
        val boundingBox = targetEntity.getComponent<WorldSpaceAABB>()!!

        entities.forEach { entity ->
            if (entity == targetEntity) return@forEach

            val dynamicCollider = entity.getComponent<DynamicCollider>()
            val staticCollider = entity.getComponent<StaticCollider>()
            if (dynamicCollider == null && staticCollider == null) return@forEach

            val otherBoundingBox = entity.getComponent<WorldSpaceAABB>() ?: return@forEach
            val otherTransform = entity.getComponent<Transform>() ?: return@forEach

            if (!boundingBox.intersects(otherBoundingBox)) return@forEach

            if (dynamicCollider != null) {
                solver.solveDynamic(
                    transform, otherTransform, boundingBox, otherBoundingBox, collider, dynamicCollider,
                    targetEntity, entity
                )
            } else {
                solver.solveStatic(
                    transform, otherTransform, boundingBox, otherBoundingBox, collider, staticCollider!!,
                    targetEntity, entity, onGround
                )
            }
        }
    }

    interface NarrowCollisionSolver {

        fun solveStatic(
            transform: Transform,
            otherTransform: Transform,
            boundingBox: WorldSpaceAABB,
            otherBoundingBox: WorldSpaceAABB,
            collider: DynamicCollider,
            otherCollider: StaticCollider,
            entity: Entity,
            otherEntity: Entity,
            onGround: OnGround?
        )

        fun solveDynamic(
            transform: Transform,
            otherTransform: Transform,
            boundingBox: WorldSpaceAABB,
            otherBoundingBox: WorldSpaceAABB,
            collider: DynamicCollider,
            otherCollider: DynamicCollider,
            entity: Entity,
            otherEntity: Entity
        )

        companion object {
            val AABB_SOLVER = object : NarrowCollisionSolver {
                override fun solveStatic(
                    transform: Transform,
                    otherTransform: Transform,
                    boundingBox: WorldSpaceAABB,
                    otherBoundingBox: WorldSpaceAABB,
                    collider: DynamicCollider,
                    otherCollider: StaticCollider,
                    entity: Entity,
                    otherEntity: Entity,
                    onGround: OnGround?
                ) {
                    val overlap = (boundingBox intersect otherBoundingBox).size

                    val component = overlap.minComponent()
                    var overlapComponent = overlap[component]
                    if (overlapComponent <= 0) return

                    if (boundingBox.center[component] < otherBoundingBox.center[component]) {
                        overlapComponent = -overlapComponent
                    }

                    if (component == 1) onGround?.isOnGround = true

                    transform.position[component] = transform.position[component] + overlapComponent
                    collider.previousPosition[component] = transform.position[component]
                }

                override fun solveDynamic(
                    transform: Transform,
                    otherTransform: Transform,
                    boundingBox: WorldSpaceAABB,
                    otherBoundingBox: WorldSpaceAABB,
                    collider: DynamicCollider,
                    otherCollider: DynamicCollider,
                    entity: Entity,
                    otherEntity: Entity
                ) {
                    if (collider.isStaticOnly || otherCollider.isStaticOnly) return

                    val overlap = (boundingBox intersect otherBoundingBox).size

                    val component = overlap.minComponent()
                    var overlapComponent = overlap[component]
                    if (overlapComponent <= 0) return

                    if (boundingBox.center[component] < otherBoundingBox.center[component]) {
                        overlapComponent = -overlapComponent
                    }

                    transform.position[component] = transform.position[component] + overlapComponent / 2f
                    otherTransform.position[component] = otherTransform.position[component] - overlapComponent / 2f

                    //TODO introduce simple elasticity - multiply previous position correction with factor
                    collider.previousPosition[component] = transform.position[component]
                    otherCollider.previousPosition[component] = otherTransform.position[component]
                }
            }
        }

    }

}
