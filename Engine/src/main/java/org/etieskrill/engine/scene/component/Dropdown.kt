package org.etieskrill.engine.scene.component

import io.github.oshai.kotlinlogging.KotlinLogging
import org.etieskrill.engine.graphics.Batch
import org.etieskrill.engine.graphics.text.Fonts
import org.etieskrill.engine.input.Key
import org.etieskrill.engine.input.Keys
import org.joml.Vector2f

private val logger = KotlinLogging.logger {}

class Dropdown(
    private val options: List<String>,
    private val action: (Int, String) -> Unit
) : Node<Dropdown>() {

    var currentOption: String = ""
        set(value) {
            if (value !in options) {
                logger.warn { "Option '$value' is not in possible options $options" }
                return
            }

            field = value
            action(currentOptionIndex, field)
        }

    var currentOptionIndex: Int
        get() = options.indexOf(currentOption)
        set(value) {
            if (value !in 0..options.size) {
                logger.warn { "Index $value is out of bounds for ${options.size} options" }
            }
        }

    private val font = Fonts.getDefault(16)
    private val container = WidgetContainer(
        VBox(options.map { option ->
            Button(Label(option, font)).apply {
                setAction { currentOption = option }
            }
        })
    ).also { it.parent = this }

    init {
        check(options.isNotEmpty()) { "Dropdown must have at least one option" }
        currentOption = options[0]
        currentOptionIndex = 0
    }

    override fun format() {
        if (!shouldFormat()) return

        (container.child as VBox).children.forEach {
            it.size = Vector2f(size.x, size.y / options.size)
        }

        container.child.size = size

        container.format()
    }

    override fun render(batch: Batch) {
        container.render(batch)
    }

    override fun handleHit(button: Key, action: Keys.Action, posX: Double, posY: Double): Boolean {
        return container.handleHit(button, action, posX, posY)
    }

}
