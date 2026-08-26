package org.etieskrill.engine.scene.container

import org.etieskrill.engine.graphics.text.Font
import org.etieskrill.engine.graphics.texture.Texture2D
import org.etieskrill.engine.graphics.texture.TextureType
import org.etieskrill.engine.input.KeyEvent
import org.etieskrill.engine.input.KeyEventAction
import org.etieskrill.engine.scene.Batch
import org.etieskrill.engine.scene.Node
import org.etieskrill.engine.scene.Node.ScaleMode.*
import org.etieskrill.engine.scene.getPreferredNodePosition
import org.joml.Math.toRadians
import org.joml.Vector2f
import org.joml.Vector4f
import org.joml.minusAssign

/**
 * A collapsible node with a draggable widget bar containing a single child.
 */
open class WidgetContainer(
    child: Node<*>? = null
) : Node<WidgetContainer>() {

    companion object {
        private val WIDGET_BAR_COLOUR = Vector4f(128 / 255f, 0f, 0f, 1f) //#800000
        private const val WIDGET_BAR_HEIGHT = 20
        private val WIDGET_CHEVRON_COLOUR = Vector4f(1f)
        private const val WIDGET_CHEVRON_MARGIN = 2
    }

    var child: Node<*>? = child
        set(value) {
            invalidate()
            value?.parent = this
            field = value
        }

    init {
        this.child = child //to call property getter
    }

    var collapsed = false
        set(value) {
            invalidate()
            field = value
        }

    var text: String? = null

    private val actualSize = Vector2f(-1f)
    private val barHeight = WIDGET_BAR_HEIGHT
    //TODO omg omg with an analysis extension you could validate resource paths at analysis time, or at least at compile time, right?
    // after some researching, this is trivial to do during compilation, but unfortunately, an analysis extension has no
    // way to query available resources out of the box. it may be possible using:
    // - an intellij plugin (or other ide specific task) to trigger a gradle task when resources change, then
    // - a gradle task to build an index of all resources, store it, and pass on the location, which is interpreted by
    // - an analysis compiler extension, which reads the index, finds all locations where a resource path is required,
    //   and checking whether the given resource path in code is present in the index
    //then: a ui element may be rendered by any number of completely independent contexts at any given time, meaning
    //that initialisation of resources such as textures cannot happen as it is now. two options are immediately evident:
    // - create a "ContextAware" box type or loader, which handles actually loading the resource for each context
    //   lazily. this would also delegate the conundrum of always having to find a graphics context to pass around from
    //   the user to a central loader class. all roads lead back to ugly factory methods. the other option is
    // - there is no other option. or i just forgot about it. pick your contestant.
    //so it will be a loader: it contains a map not just from a resource name to a resource, but from a name to a
    //resource within a given context. the initialisers are all passed as lambdas, and they receive a context variable.
    //with this in place, a resource could only be a shell until it is needed, and then it is lazily initialised for a
    //required context. if a context is known to be used at application start, it is also possible to eagerly load
    //resources associated with that context, in order to avoid loading stutters.
    //only when we actually try to render a context-aware system, it will actually grab the context and initialise everything.
    //so in order to have a type-safe environment, only constructors with explicit contexts arguments must be made
    //public, and a series of constructor-like factory methods can be made available within a context-aware initialiser.
    //this is of course the very liberal approach, such that any resource declared anywhere can be used in any place and
    //in any context.
    //
    //interface GraphicsContextAware<T> {
    //    fun initContext(context: GraphicsContext): T = context.withContext { init() }
    //    fun init(block: () -> T): T
    //}
    //
    //class GraphicsContextInitialiser<T, V : GraphicsContextAware>(
    //    private val initialiser: (GraphicsContext) -> V
    //) : ReadOnlyProperty<T, V> {
    //    private val contexts = mutableMapOf<GraphicsContext, V>()
    //
    //    fun getValue(thisRef: T, property: KProperty<*>): V {
    //        return contexts.getOrPut(GraphicsContext.current) { initialiser(GraphicsContext.current) }
    //    }
    //}
    private val chevronIcon =
        Texture2D.createFromFile("textures/icons/chevron-down-solid-black.png", TextureType.DIFFUSE)
    private val titleFont = Font.getDefault((barHeight) - 4)

    override fun update(delta: Double) {
        child?.update(delta)
    }

    override fun computeFixedSizes() { //FIXME bar height is still ignored somehow
        if (!shouldFormat) return

        child?.computeFixedSizes()

        computedFixedSize = false

        if (collapsed) {
            when (scaleMode) {
                FIXED -> {
                    formattedSize.set(size.x, barHeight.toFloat())
                    computedFixedSize = true
                }

                CONTENT -> {
                    when {
                        child == null -> {
                            formattedSize.set(100f, barHeight.toFloat())
                            computedFixedSize = true
                        }

                        child!!.scaleMode == GROW -> TODO("ScaleMode.GROW for WidgetContainer")

                        child!!.scaleMode != GROW -> {
                            if (!child!!.computedFixedSize) throw IllegalStateException("Child size was not computed")

                            formattedSize.set(child!!.formattedSize.x, barHeight.toFloat())
                            computedFixedSize = true
                        }
                    }
                }

                GROW -> TODO("ScaleMode.GROW for WidgetContainer")
            }
            return
        }

        when (scaleMode) {
            FIXED -> {
                formattedSize.set(size.x, size.y + barHeight)
                computedFixedSize = true
            }

            CONTENT -> {
                when {
                    child == null -> {
                        formattedSize.set(100f, barHeight.toFloat() + 5f)
                        computedFixedSize = true
                    }

                    child!!.scaleMode == GROW -> TODO("ScaleMode.GROW for WidgetContainer")

                    child!!.scaleMode != GROW -> {
                        if (!child!!.computedFixedSize) throw IllegalStateException("Child size was not computed")

                        formattedSize.set(child!!.formattedSize.x, child!!.formattedSize.y + barHeight)
                        computedFixedSize = true
                    }
                }
            }

            GROW -> TODO("ScaleMode.GROW for WidgetContainer")
        }
    }

    override fun layout() {
        if (!shouldFormat()) return

        child?.let {
            it.layout()
            it.position = getPreferredNodePosition(formattedSize, it).apply { y += barHeight.toFloat() }
        }
    }

    override fun render(batch: Batch) {
        val position = absolutePosition

        batch.renderBox(position, Vector2f(formattedSize.x, barHeight.toFloat()), WIDGET_BAR_COLOUR)
        batch.blit(
            chevronIcon,
            Vector2f(position).add(WIDGET_CHEVRON_MARGIN.toFloat(), WIDGET_CHEVRON_MARGIN.toFloat()),
            Vector2f(barHeight - 2f * WIDGET_CHEVRON_MARGIN),
            if (collapsed) toRadians(90f) else toRadians(-180f),
            WIDGET_CHEVRON_COLOUR
        )
        text?.takeIf { it.isNotBlank() }?.let {
            //TODO use label with autoscaling font instead of hardcoding - scale bar height too actually
            batch.renderText(it, titleFont, Vector2f(position).add(barHeight + 2f, -3f))
        }

        if (!collapsed) {
            if (renderedColour.w != 0f) {
                batch.renderBox(
                    Vector2f(absolutePosition).apply { y += barHeight.toFloat() },
                    Vector2f(formattedSize).apply { y -= barHeight.toFloat() },
                    renderedColour
                )
            }
            child?.render(batch)
        }
    }

    override fun handleHit(event: KeyEvent, posX: Double, posY: Double): Boolean {
        if (!doesHit(posX, posY)) return false

        val position = absolutePosition
        if (event.action == KeyEventAction.PRESS
            && posX > position.x && posX <= position.x + barHeight
            && posY > position.y && posY <= position.y + barHeight
        ) {
            collapsed = !collapsed
            requestFocus() //any reason for a resetFocus instead?
            return true
        }

        return child?.handleHit(event, posX, posY) ?: false //container itself is not hittable
    }

    override fun handleKey(event: KeyEvent): Boolean =
        child?.handleKey(event) ?: false

    override fun handleDrag(deltaX: Double, deltaY: Double, posX: Double, posY: Double): Boolean {
        if (alignment != Alignment.FIXED_POSITION) return false

        if (!doesHit(posX, posY)) return false

        if (posX !in (position.x + barHeight)..(position.x + formattedSize.x)
            || posY !in position.y..(position.y + barHeight)
        ) {
            return false
        }

        position -= Vector2f(deltaX.toFloat(), deltaY.toFloat())

        return true
    }

}
