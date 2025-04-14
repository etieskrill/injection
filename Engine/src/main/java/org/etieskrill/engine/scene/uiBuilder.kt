package org.etieskrill.engine.scene

import org.etieskrill.engine.input.Keys
import org.etieskrill.engine.scene.component.Node
import org.etieskrill.engine.scene.component.Node.Alignment

fun main() {
    var wrapping = 0

//    var fps by property()
//    var mode by property()
    var fps = 0
    var mode = 0

    vbox {
        label(fps, mode) {
            +"FPS: %.0f".format(fps)
            +"Current mode: $mode"
            +"Press '${Keys.E}' to cycle mode"
            +"Mouse drag to move"
        }
        button {
            label {
                +"Previous mode"
                alignment = Alignment.CENTER
            }
            onAction { wrapping = wrapping-- }
            alignment = Alignment.BOTTOM_LEFT
        }
        button {
            label {
                +"Next mode"
                alignment = Alignment.CENTER
            }
            onAction { wrapping = wrapping++ }
            alignment = Alignment.BOTTOM_RIGHT
        }
    }
}

abstract class NodeBuilder<T : NodeBuilder<T>>(
    protected open val block: T.() -> Unit,
    open val properties: List<Any>
) {
    var alignment: Node.Alignment = Node.Alignment.TOP_LEFT
}

class VBoxBuilder(
    block: VBoxBuilder.() -> Unit,
    properties: List<Any>
) : NodeBuilder<VBoxBuilder>(block, properties)

fun vbox(vararg properties: Any, block: VBoxBuilder.() -> Unit) = block(VBoxBuilder(block, properties.toList()))

class LabelBuilder(
    override val block: LabelBuilder.() -> Unit,
    override val properties: List<Any>
) : NodeBuilder<LabelBuilder>(block, properties) {
    private val text = StringBuilder()

    operator fun String.unaryPlus(): StringBuilder = text.append(this)
}

fun label(vararg properties: Any, block: LabelBuilder.() -> Unit) = block(LabelBuilder(block, properties.toList()))

class ButtonBuilder(
    override val block: ButtonBuilder.() -> Unit,
    override val properties: List<Any>
) : NodeBuilder<ButtonBuilder>(block, properties) {
    private var action = {}

    fun onAction(action: () -> Unit) {
        this.action = action
    }
}

fun button(vararg properties: Any, block: ButtonBuilder.() -> Unit) = block(ButtonBuilder(block, properties.toList()))
