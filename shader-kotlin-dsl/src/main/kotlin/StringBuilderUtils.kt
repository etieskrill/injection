package io.github.etieskrill.injection.extension.shader.dsl

internal fun StringBuilder.newline() = appendLine()

internal fun buildIndentedString(indent: Int = 0, block: IndentedStringBuilder.() -> Unit): String {
    val builder = StringBuilder()
    block(IndentedStringBuilder(builder, indent))
    return builder.toString()
}

internal class IndentedStringBuilder(
    private val builder: StringBuilder,
    private var indent: Int
) : Appendable by builder {

    override fun append(csq: CharSequence?): java.lang.Appendable =
        builder.append("${"\t".repeat(indent)}${csq ?: "<null>"}")

    fun appendMultiLine(value: String) = value
        .lines()
        .joinTo(builder, separator = "\n", postfix = "\n") {
            "\t".repeat(indent) + it
        }

    fun indent(block: IndentedStringBuilder.() -> Unit) {
        indent++
        block(this)
        indent--
    }

}
