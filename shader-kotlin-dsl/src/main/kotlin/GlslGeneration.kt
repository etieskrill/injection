package io.github.etieskrill.injection.extension.shader.dsl

import io.github.etieskrill.injection.extension.shader.ShaderStage
import io.github.etieskrill.injection.extension.shader.dsl.GlslStorageQualifier.CONST
import io.github.etieskrill.injection.extension.shader.dsl.OperatorType.entries
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.backend.js.utils.valueArguments
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin.Companion.DIVEQ
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin.Companion.EQ
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin.Companion.MINUSEQ
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin.Companion.MULTEQ
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin.Companion.PERCEQ
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin.Companion.PLUSEQ
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.getArgumentsWithIr
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.IrVisitor

private const val GL_POSITION_NAME = "gl_Position"
internal val POSITION_FIELD_NAME = ShaderVertexData::position.name

private val properties = mapOf(
    ShaderStage.VERTEX to mapOf(
        VertexReceiver::vertexID.name to "gl_VertexID"
    )
)

private enum class OperatorType(
    val operator: String
) { PLUS("+"), MINUS("-"), TIMES("*"), DIV("/"), REM("%"), FUN("<fun>") }

private val String.glslOperator get() = entries.firstOrNull { it.name.equals(this, true) }
private val String.isGlslOperator get() = glslOperator != null

private val functionNames = GlslReceiver::class.members.map { it.name }

private data class TranspilerData(
    val pluginContext: IrPluginContext,
    val programParent: IrDeclarationParent,
    val vertexDataStructName: String,
    var stage: ShaderStage = ShaderStage.NONE,
    var operatorStack: MutableList<OperatorType> = mutableListOf(), //TODO remove brackets where neighboring operators are same or less binding
    var ifWhenVariableName: String? = null
)

internal fun generateGlsl(programData: VisitorData, pluginContext: IrPluginContext): String = buildString {
    val transpilerData = TranspilerData(pluginContext, programData.programParent, programData.vertexDataStructName)

    appendLine(
        "#version ${programData.version} ${
            programData.profile
                .takeIf { it == GlslProfile.CORE }?.name?.lowercase()
        }"
    )
    newline()

    appendLine(generateStruct(programData.vertexDataStructType, programData.vertexDataStruct))
    newline()

    programData.constants.forEach { (name, constExpression) ->
        val resolver = GlslValueTranspiler()
        val value = constExpression.initialiser.accept(resolver, transpilerData)
        check(resolver.array == constExpression.array)

        val statement = when (constExpression.array) {
            false -> generateStatement(CONST, constExpression.type, name, value)
            true -> generateArrayStatement(CONST, constExpression.type, resolver.arraySize!!, name, value)
        }

        appendLine(statement)
    }
    newline()

    programData.uniforms.forEach { (name, type) ->
        appendLine(generateStatement(GlslStorageQualifier.UNIFORM, type, name))
    }
    newline()

    transpilerData.stage = ShaderStage.VERTEX

    appendLine(generateStage(ShaderStage.VERTEX))
    newline()

    programData.vertexAttributes.forEach { (name, type) ->
        appendLine(generateStatement(GlslStorageQualifier.IN, type, name))
    }
    newline()

    appendLine(generateStatement(GlslStorageQualifier.OUT, programData.vertexDataStructType, "vertex"))
    newline()

    appendLine(generateMain(programData.stageBodies[ShaderStage.VERTEX]!!, transpilerData))
    newline()

    transpilerData.stage = ShaderStage.FRAGMENT

    appendLine(generateStage(ShaderStage.FRAGMENT))
    newline()

    appendLine(generateStatement(GlslStorageQualifier.IN, programData.vertexDataStructType, "vertex"))
    newline()

    programData.renderTargets.forEach {
        appendLine(generateStatement(GlslStorageQualifier.OUT, "vec4", it))
    }
    newline()

    appendLine(generateMain(programData.stageBodies[ShaderStage.FRAGMENT]!!, transpilerData))
    newline()
}

private fun generateStruct(
    name: String,
    members: Map<String, GlslType>
) = buildIndentedString {
    appendLine("struct $name {")
    indent {
        members.forEach { (name, type) ->
            appendLine("$type $name;")
        }
    }
    append("};")
}

private fun generateStage(stage: ShaderStage) = "#pragma stage ${stage.name.lowercase()}"

internal enum class GlslStorageQualifier {
    NONE, IN, OUT, UNIFORM, CONST;

    override fun toString(): String = when (this) {
        NONE -> ""
        else -> name.lowercase()
    }
}

private fun generateStatement(
    qualifier: GlslStorageQualifier,
    type: GlslType,
    name: String,
    initialiser: String? = null
) = generateStatement(qualifier, type.type, name, initialiser)

private fun generateStatement(
    qualifier: GlslStorageQualifier,
    type: String,
    name: String,
    initialiser: String? = null
): String {
    require(initialiser == null || qualifier == CONST) {
        "Only const statements may have an initialiser"
    }
    return "$qualifier $type $name${initialiser?.let { " = $initialiser" } ?: ""};"
}

private fun generateArrayStatement(
    qualifier: GlslStorageQualifier,
    type: GlslType,
    size: Int,
    name: String,
    initialiser: String? = null
): String {
    require(initialiser == null || qualifier == CONST) {
        "Only const statements may have an initialiser"
    }
    return "$qualifier $type $name[$size]${initialiser?.let { " = $initialiser" } ?: ""};"
}

private fun generateMain(body: IrBlockBody, data: TranspilerData) = buildIndentedString {
    appendLine("void main() {")
    indent {
        val transformer = InlineConditionalTransformer(data.pluginContext)
        var transformerData: InlineConditionalTransformerData
        var transformerIteration = 0
        do {
            check(transformerIteration++ < 1000) { "Inline condition transformer ran for more than a thousand iterations: something is probably bricked" }
            transformerData = InlineConditionalTransformerData()
            body.transform(transformer, transformerData)
        } while (transformerData.inlineCondition != null)
        data.programParent.patchDeclarationParents()

        val statements = body.statements.associateWith { it.accept(GlslTranspiler(), data) }

        val formattedStatements = mutableListOf<String>()
        statements.forEach { (element, statement) ->
            when (element) {
                is IrWhen -> {
                    formattedStatements += ""
                    formattedStatements += statement
                    formattedStatements += ""
                }

                else -> formattedStatements += statement.appendIfNotEndsIn(";")
            }
        }

        appendMultiLine(formattedStatements.joinToString("\n"))
    }
    append("}")
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
private open class GlslTranspiler : IrVisitor<String, TranspilerData>() {
    override fun visitElement(element: IrElement, data: TranspilerData): String =
        TODO("Element of type ${element::class.simpleName} cannot be processed yet:\n${element.dump()}")

    override fun visitBlock(expression: IrBlock, data: TranspilerData): String =
        expression.statements.joinToString(";\n") { it.accept(this, data) }

    override fun visitBlockBody(body: IrBlockBody, data: TranspilerData): String =
        body.statements.joinToString(";\n") { it.accept(this, data) }

    override fun visitVariable(declaration: IrVariable, data: TranspilerData): String {
        val initialiser = declaration.initializer
            ?.let { " = ${it.accept(this, data)}" }
            .orEmpty()
        return "${declaration.type.glslType} ${declaration.name}${initialiser}"
    }

    override fun visitCall(expression: IrCall, data: TranspilerData): String {
        if (data.stage == ShaderStage.FRAGMENT && expression.returns<RenderTarget>()
        ) return evalChildren(expression, data)!!

        val functionName = expression.symbol.owner.name.asString()

        return when {
            expression.origin == IrStatementOrigin.GET_PROPERTY -> {
                var propertyOwner = evalChildren(expression, data)
                val propertyName = functionName.removePrefix("<get-").removeSuffix(">")

                if (propertyOwner!!.startsWith("\$this$")) { //accessing a stage receiver property
                    return properties[data.stage]!![propertyName]!!
                }

                when (propertyOwner) {
                    "<this>" -> propertyName //direct shader class members, e.g. uniforms
                    "it" -> when (data.stage) { //implicit lambda parameter //FIXME find and use declared name if not implicit
                        ShaderStage.VERTEX -> propertyName
                        ShaderStage.FRAGMENT -> when (propertyName) {
                            POSITION_FIELD_NAME -> GL_POSITION_NAME
                            else -> "${data.vertexDataStructName}.$propertyName"
                        }

                        else -> TODO("Getter for implicit lambda parameter call not implemented for stage ${data.stage}")
                    }

                    else -> "$propertyOwner.$propertyName"
                }
            }

            expression.origin == IrStatementOrigin.GET_ARRAY_ELEMENT -> {
                val arguments = expression.getArgumentsWithIr().toMap().mapKeys { it.key.name.asString() }

                val arrayName: String = arguments["<this>"]!!.accept(this, data)
                val arrayIndex: String = arguments["index"]!!.accept(this, data)

                return "$arrayName[$arrayIndex]"
            }

            functionName in listOf("unaryPlus", "unaryMinus") -> {
                val p = evalChildren(expression, data)!!
                "${if (functionName == "unaryPlus") "+" else "-"}$p"
            }

            functionName.isGlslOperator -> handleOperator(functionName.glslOperator!!, expression, data)
            functionName in functionNames -> handleFunction(functionName, expression, data)
            else -> error("Unexpected function ${expression.render()}")
        }
    }

    override fun visitWhen(expression: IrWhen, data: TranspilerData): String {
        if (expression.origin!! != IrStatementOrigin.IF) TODO(expression.dump())
        //TODO filter/check inline ifs/whens

        val numBranches = expression.branches.size
        val statement = expression.branches.mapIndexed { i, it ->
            val condition = it.condition.accept(this, data)
            val conditionStatement = when {
                i == numBranches - 1 && condition == "true" -> "else"
                i != 0 -> "else if ($condition)"
                else -> "if ($condition)"
            }

            val result = it.result.accept(this, data)

            """
                $conditionStatement {
                    $result;
                } """.trimIndent()
        }.joinToString("")

        return statement
    }

    override fun visitReturn(expression: IrReturn, data: TranspilerData): String =
        expression.value.accept(GlslReturnTranspiler(this), data)

    override fun visitConst(expression: IrConst, data: TranspilerData): String =
        expression.value.toString()

    override fun visitGetValue(expression: IrGetValue, data: TranspilerData): String =
        expression.symbol.owner.name.asString()

    private val assignmentOperators = mapOf(
        PLUSEQ to "+=",
        MINUSEQ to "-=",
        MULTEQ to "*=",
        DIVEQ to "/=",
        PERCEQ to "%="
    )

    override fun visitSetValue(expression: IrSetValue, data: TranspilerData): String {
        return when (expression.origin) {
            EQ -> {
                val leftSide = expression.symbol.owner.name.asString()
                val rightSide = expression.value.accept(this, data)

                "$leftSide = $rightSide"
            }

            in assignmentOperators -> {
                val leftSide = expression.symbol.owner.name.asString()
                val rightSide =
                    (expression.value as IrCall) //TODO wrapper funcs, especially for getting params //FIXME if assignment op is passed, this will be IrGetValue instead of IrCall
                        .getArgumentsWithIr()
                        .first { it.first.name.asString() != "<this>" } //TODO receiver helper funcs
                        .second.accept(this, data)
                "$leftSide ${assignmentOperators[expression.origin]!!} $rightSide"
            }

            else -> TODO()
        }
    }

    private fun handleOperator(operator: OperatorType, expression: IrCall, data: TranspilerData): String {
        check(
            expression.origin!!.debugName.lowercase() !in OperatorType.entries.map { it.name.lowercase() }
                    || expression.extensionReceiver != null
                    || expression.dispatchReceiver != null
        )
        { "Operator function did not have a receiver: $operator:\n${expression.render()}" }
        return "(${(expression.extensionReceiver ?: expression.dispatchReceiver)!!.accept(this, data)} ${
            operator.operator
        } ${expression.getValueArgument(0)!!.accept(this, data)})"
    }

    private fun handleFunction(functionName: String, expression: IrCall, data: TranspilerData): String = expression
        .valueArguments
        .joinToString(", ", prefix = "$functionName(", postfix = ")") {
            it!!.accept(this, data)
        }

}

private class GlslReturnTranspiler(val root: GlslTranspiler) : IrVisitor<String, TranspilerData>() {
    override fun visitElement(element: IrElement, data: TranspilerData): String {
        TODO("Element of type ${element::class.simpleName} cannot be processed yet:\n${element.dump()}")
    }

    override fun visitConstructorCall(expression: IrConstructorCall, data: TranspilerData): String {
        return expression
            .getArgumentsWithIr()
            .associate { (param, expression) -> param.name.asString() to expression.accept(root, data) }
            .map { (param, expression) ->
                when (data.stage) {
                    ShaderStage.VERTEX -> {
                        when (param) {
                            POSITION_FIELD_NAME -> "$GL_POSITION_NAME = $expression;"
                            else -> "${data.vertexDataStructName}.$param = $expression;"
                        }
                    }

                    ShaderStage.FRAGMENT -> "$param = $expression;"
                    else -> TODO("Constructor return call not implemented for stage ${data.stage}")
                }
            }
            .joinToString("\n")
    }
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
private class GlslValueTranspiler(
    var array: Boolean = false,
    var arraySize: Int? = null,
) : GlslTranspiler() {
    override fun visitCall(expression: IrCall, data: TranspilerData): String {
        val functionName = expression.symbol.owner.name.asString()
        return when {
            functionName == "arrayOf" -> {
                val elements = expression.getArgumentsWithIr()
                    .first()
                    .second as IrVararg

                array = true
                arraySize = elements.elements.size
                val type = requireNotNull(elements.varargElementType.glslType)
                { "Const array elements must be of GLSL primitive type" }

                val values = elements.elements.joinToString { it.accept(this, data) }

                "$type[]($values)"
            }

            else -> super.visitCall(expression, data)
        }
    }
}

private fun GlslTranspiler.evalChildren(expression: IrExpression, data: TranspilerData): String? {
    var value: String? = null
    expression.acceptChildren(object : IrVisitor<Unit, TranspilerData>() {
        override fun visitElement(element: IrElement, data: TranspilerData) {
            value = element.accept(this@evalChildren, data)
        }
    }, data)
    return value
}

internal inline fun <reified T> IrType?.equals() =
    this?.fullName.let { it == T::class.qualifiedName!! } == true

internal inline fun <reified T> IrExpression.returns() = type.equals<T>()
