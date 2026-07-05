package org.etieskrill.engine.scene.element

import io.github.oshai.kotlinlogging.KotlinLogging
import org.etieskrill.engine.graphics.Batch
import org.etieskrill.engine.graphics.text.Fonts
import org.etieskrill.engine.input.Key
import org.etieskrill.engine.input.Keys
import org.etieskrill.engine.scene.Node
import org.etieskrill.engine.scene.container.VBox
import org.etieskrill.engine.scene.container.WidgetContainer
import org.joml.Vector4f

private val logger = KotlinLogging.logger {}

class Dropdown(
    private val options: List<String>,
    private val action: suspend (Int, String) -> Unit
) : Node<Dropdown>() {

    var currentOption: String = ""
        set(value) {
            if (value !in options) {
                logger.warn { "Option '$value' is not in possible options $options" }
                return
            }

            field = value
            ui { action(currentOptionIndex, field) }
        }

    var currentOptionIndex: Int
        get() = options.indexOf(currentOption)
        set(value) {
            if (value !in 0..options.size) {
                logger.warn { "Index $value is out of bounds for ${options.size} options" }
            }
            currentOption = options[value]
        }

    private val font = Fonts.getDefault(16)
    private val container = WidgetContainer(
        VBox(options.map { option ->
            Button(Label(option, font)) { currentOption = option }
        })
    ).also { it.parent = this }

    init {
        check(options.isNotEmpty()) { "Dropdown must have at least one option" }
        check(options.none { it.isBlank() }) { "Dropdown options must not be blank" }

//        currentOption = options[0]
//        currentOptionIndex = 0

        container.colour = Vector4f(0.2f)
    }

    override fun computeFixedSizes() {
        if (currentOption == "") { //FIXME there is a need for a manual init after scene graph is built, an equivalent of Godot's ready
            currentOption = options[0]
            currentOptionIndex = 0
        }

        if (!shouldFormat()) return

        container.computeFixedSizes()
        computedFixedSize = false

        when (scaleMode) {
            ScaleMode.FIXED -> {
                formattedSize = size
                computedFixedSize = true
            }

            ScaleMode.CONTENT -> {
                if (container.scaleMode == ScaleMode.GROW || !container.computedFixedSize) {
                    TODO("ScaleMode.GROW for WidgetContainer and Dropdown")
                }

                formattedSize = container.formattedSize
                computedFixedSize = true
            }

            ScaleMode.GROW -> TODO("ScaleMode.GROW for WidgetContainer and Dropdown")
        }
    }

    override fun layout() {
        container.layout()
    }

    override fun render(batch: Batch) {
        container.render(batch)
    }

    override fun handleHit(button: Key, action: Keys.Action, posX: Double, posY: Double): Boolean {
        return container.handleHit(button, action, posX, posY)
    }

}