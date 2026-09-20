package io.github.etieskrill.games.circles

import org.etieskrill.engine.entity.Entity
import org.etieskrill.engine.entity.component.Transform
import org.etieskrill.engine.entity.service.Service
import org.etieskrill.engine.entity.system.EntitySystem
import org.joml.Vector3f
import org.joml.Vector3fc
import org.joml.Vector4f
import kotlin.math.max
import kotlin.math.min

class CircleService(private val environment: VisEnvironment) : Service {

    private val supplyChains = mutableMapOf<Circle, List<SupplyChainNode>>() //receptacle to emitter for node tree

    override fun canProcess(entity: Entity) = entity.hasComponents(Transform::class.java, Circle::class.java)

    override fun preProcess(delta: Double, entities: List<Entity>) {
        environment.update(delta.toFloat())

        entities
            .mapNotNull { it.getComponent<Circle>() }
            .forEach { circle ->
                if (circle !in supplyChains) {
                    supplyChains[circle] = searchSupplyChains(circle)
                }

                supplyChains[circle]!!.forEachIndexed { index, receptacleNode ->
                    receptacleNode.types.forEach { type ->
                        val remainingCapacity =
                            receptacleNode.symbol.run { visCapacity - storedVis.getOrElse(type) { 0f } }
                        environment.requestVis(index, type, Vector3f(0f), 1f, remainingCapacity)
                    }
                }
            }
    }

    override fun process(
        targetEntity: Entity,
        entities: List<Entity>,
        delta: Double
    ) {
        val transform = targetEntity.getComponent<Transform>()!!
        val circle = targetEntity.getComponent<Circle>()!!
        val chains = supplyChains[circle]!!

        chains.forEachIndexed { index, receptacleNode ->
            when (val receptacle = receptacleNode.symbol) {
                is StreamSlot -> {
                    receptacleNode.types.forEach { type ->
                        val newAmount = (receptacle.storedVis[type] ?: 0f) + environment.getVis(index, type)
                        receptacle.storedVis[type] = min(newAmount, receptacle.visCapacity)
                    }
                }

                is ReceptacleRune -> TODO()
                else -> error("Unknown vis receptacle type: $receptacle")
            }
        }

        chains.forEach(::updateSupplyChain)
        updateRunes(transform, circle)
    }

    override fun entityRemoved(entity: Entity) {
        val circle = entity.getComponent<Circle>() ?: return
        supplyChains.remove(circle)
    }

    private data class SupplyChainNode(
        val symbol: CircleSlot,
        val children: List<SupplyChainNode>,
        val types: List<VisType>,
    ) {
        var parent: SupplyChainNode? = null

        init {
            children.forEach { it.parent = this }
        }
    }

    private fun updateSupplyChain(node: SupplyChainNode) {
        node.children.forEach(::updateSupplyChain)

        val parent = node.parent ?: return

        node.types.forEach { type ->
            node.symbol.storedVis.putIfAbsent(type, 0f)
            parent.symbol.storedVis.putIfAbsent(type, 0f)

            val capacity = node.symbol.visCapacity - node.symbol.storedVis[type]!!
            val available = parent.symbol.storedVis[type]!!
            val transferredAmount = min(capacity, available)

            node.symbol.storedVis[type] = node.symbol.storedVis[type]!! + transferredAmount
            parent.symbol.storedVis[type] = parent.symbol.storedVis[type]!! - transferredAmount
        }
    }

    private fun updateRunes(transform: Transform, circle: Circle, circleSlot: CircleSlot = circle) {
        when (circleSlot) {
            is EmitterRune -> {
                if (!circleSlot.consumption.all { (type, amount) ->
                        (circleSlot.storedVis[type] ?: 0f) >= amount
                    }) return

                circleSlot.consumption.forEach { type, amount ->
                    circleSlot.storedVis[type] = max(0f, (circleSlot.storedVis[type] ?: 0f) - amount)
                }
                circleSlot.effect(transform.position, transform.up, circle.placedOn)
            }

            is Circle -> {
                circleSlot.focalRune?.let { updateRunes(transform, circleSlot, it) }
                circleSlot.runes
                    .filterNotNull()
                    .filterIsInstance<EmitterRune>()
                    .forEach { updateRunes(transform, circleSlot, it) }
                circleSlot.runes
                    .filterNotNull()
                    .filterIsInstance<Circle>()
                    .forEach { updateRunes(transform, circleSlot, it) }
            }

            else -> {}
        }
    }

    private fun searchSupplyChains(circle: Circle): List<SupplyChainNode> {
        // 1. search every path from an emitter to a receiver for every element by bfs
        // 2. remove all non-compatible vis paths

//        return TODO()
        return listOf(
            SupplyChainNode(
                circle, listOf(
                    SupplyChainNode(circle.focalRune!!, emptyList(), listOf(VisType("fire", Vector4f(1f, 0.5f, 0f, 1f))))
                ), listOf(VisType("fire", Vector4f(1f, 0.5f, 0f, 1f)))
            )
        )
    }

    override fun toString(): String {
        return "CircleService(environment=$environment, supplyChains=$supplyChains)"
    }

}

private val auxRuneGebo = AuxiliaryRune("gebo")
private val auxRuneNauthiz = AuxiliaryRune("nauthiz")

fun main() {
    val entitySystem = EntitySystem()

    val environment = object : Environment {
        override fun getTemperature(position: Vector3fc) = 10f
        override fun addHeatEnergy(position: Vector3fc, energy: Float) = Unit
        override fun update(delta: Float) = Unit

    }
    val visEnvironment = object : VisEnvironment {
        override fun requestVis(id: Int, type: VisType, position: Vector3fc, strength: Float, max: Float) = Unit
        override fun getVis(id: Int, type: VisType): Float = 5f
        override fun update(delta: Float) = Unit
    }
    entitySystem.addServices(CircleService(visEnvironment), HeatService(environment))

    val cauldron = entitySystem.createEntity {
        +Transform()
        +Heatable(mass = 10f)
    }

    entitySystem.createEntity {
        +Transform()
        +Circle(
            placedOn = cauldron, visCapacity = 100f, streamUpkeepAbsolute = 1f, streamUpkeepRelative = 0.2f,
            focalRune = EmitterRune(
                "fire", 10f, mapOf(VisType("fire", Vector4f(1f, 0f, 0f, 1f)) to 1f)
            ) { pos, dir, placedOn ->
                placedOn.getComponent<Heatable>()?.apply {
                    energy += 100
                    return@EmitterRune
                }

                placedOn.getComponent<Environment>()
                    ?.apply { //FIXME no idea if this is agreeable with env entity/entities
                        addHeatEnergy(pos, 100f)
                        return@EmitterRune
                    }
            },
            runes = listOf(), numRings = 1, auxRunes = listOf(
                listOf(
                    listOf(auxRuneGebo, auxRuneNauthiz, auxRuneGebo, auxRuneNauthiz, auxRuneGebo, auxRuneNauthiz)
                )
            )
        )
    }

//    val pacer = SystemNanoTimePacer(1.0 / 60.0)
//    pacer.start()
    while (true) {
//        entitySystem.update(pacer.deltaTimeSeconds)
        entitySystem.update(1.0 / 60.0)
        println("cauldron temp: ${cauldron.getComponent<Heatable>()!!.temperature}")
//        pacer.nextFrame()
    }
}
