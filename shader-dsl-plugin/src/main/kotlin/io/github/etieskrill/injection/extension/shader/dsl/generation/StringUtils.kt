package io.github.etieskrill.injection.extension.shader.dsl.generation

private const val SPACE_INDENT = 4

internal fun buildIndentedString(indent: Int = 0, block: IndentedStringBuilder.() -> Unit): String {
    val builder = StringBuilder()
    block(IndentedStringBuilder(builder, indent))
    return builder.toString()
}

internal class IndentedStringBuilder(
    private val builder: StringBuilder,
    private var indent: Int,
    private var lastWasNewline: Boolean = false
) : Appendable by builder {

    override fun append(csq: CharSequence?): java.lang.Appendable {
        lastWasNewline = false
        return builder.append("${getIndent(indent)}${csq ?: "<null>"}")
    }

    fun appendMultiLine(value: String): StringBuilder {
        lastWasNewline = false
        return value
            .lines()
            .joinTo(builder, separator = "\n", postfix = "\n") {
                getIndent(indent) + it
            }
    }

    fun indent(block: IndentedStringBuilder.() -> Unit) {
        indent++
        block(this)
        indent--
    }

    fun newline() {
        if (!lastWasNewline) appendLine()
        lastWasNewline = true
    }

}

private fun getIndent(indent: Int) = " ".repeat(indent * SPACE_INDENT)

internal fun String.appendIf(string: String, condition: String.() -> Boolean): String =
    if (condition(this)) "${this}$string"
    else this

internal fun String.appendIfNotEndsIn(string: String) =
    appendIf(string) { !endsWith(string) }
