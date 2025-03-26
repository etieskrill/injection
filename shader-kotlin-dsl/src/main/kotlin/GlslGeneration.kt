package io.github.etieskrill.injection.extension.shader.dsl

import io.github.etieskrill.injection.extension.shader.ShaderStage
import io.github.etieskrill.injection.extension.shader.dsl.GlslStorageQualifier.CONST
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.backend.js.utils.valueArguments
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin.Companion.DIVEQ
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin.Companion.EQ
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin.Companion.MINUSEQ
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin.Companion.MULTEQ
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin.Companion.PERCEQ
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin.Companion.PLUSEQ
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.getArgumentsWithIr
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.IrVisitor

internal const val GL_POSITION_NAME = "gl_Position"
internal val POSITION_FIELD_NAME = ShaderVertexData::position.name

internal fun generateGlsl(data: VisitorData): String = buildString {
    data.stage = ShaderStage.NONE

    appendLine("#version ${data.version} ${data.profile.takeIf { it == GlslProfile.CORE }?.name?.lowercase()}")
    newline()

    appendLine(generateStruct(data.vertexDataStructType, data.vertexDataStruct))
    newline()

    data.constants.forEach { (name, constExpression) ->
        val resolver = GlslValueTranspiler()
        val value = constExpression.initialiser.accept(resolver, data)
        check(resolver.array == constExpression.array)

        val statement = when (constExpression.array) {
            false -> generateStatement(CONST, constExpression.type, name, value)
            true -> generateArrayStatement(CONST, constExpression.type, resolver.arraySize!!, name, value)
        }

        appendLine(statement)
    }
    newline()

    data.uniforms.forEach { (name, type) ->
        appendLine(generateStatement(GlslStorageQualifier.UNIFORM, type, name))
    }
    newline()

    data.stage = ShaderStage.VERTEX

    appendLine(generateStage(ShaderStage.VERTEX))
    newline()

    data.vertexAttributes.forEach { (name, type) ->
        appendLine(generateStatement(GlslStorageQualifier.IN, type, name))
    }
    newline()

    appendLine(generateStatement(GlslStorageQualifier.OUT, data.vertexDataStructType, "vertex"))
    newline()

    appendLine(generateMain(data.stages[ShaderStage.VERTEX]!!, data))
    newline()

    data.stage = ShaderStage.FRAGMENT

    appendLine(generateStage(ShaderStage.FRAGMENT))
    newline()

    appendLine(generateStatement(GlslStorageQualifier.IN, data.vertexDataStructType, "vertex"))
    newline()

    data.renderTargets.forEach {
        appendLine(generateStatement(GlslStorageQualifier.OUT, "vec4", it))
    }
    newline()

    appendLine(generateMain(data.stages[ShaderStage.FRAGMENT]!!, data))
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

private fun generateMain(statements: IrElement, data: VisitorData) = buildIndentedString {
    appendLine("void main() {")
    indent {
        appendMultiLine(statements.accept(GlslTranspiler(), data))
    }
    append("}")
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
private open class GlslTranspiler : IrVisitor<String, VisitorData>() {
    override fun visitElement(element: IrElement, data: VisitorData): String =
        TODO("Element of type ${element::class.simpleName} cannot be processed yet:\n${element.dump()}")

    override fun visitBlockBody(body: IrBlockBody, data: VisitorData): String =
        body.statements.joinToString(";\n") { it.accept(this, data) }

    override fun visitVariable(declaration: IrVariable, data: VisitorData): String {
        val initialiser = declaration.initializer
            ?.let { " = ${it.accept(this, data)}" }
            .orEmpty()
        return "${declaration.type.glslType} ${declaration.name}${initialiser}"
    }

    override fun visitCall(expression: IrCall, data: VisitorData): String {
        if (data.stage == ShaderStage.FRAGMENT && expression.type.fullName == RenderTarget::class.qualifiedName!!) {
            var renderTargetName: String? = null
            expression.acceptChildren(object : IrVisitor<Unit, VisitorData>() {
                override fun visitElement(element: IrElement, data: VisitorData) = error("no")
                override fun visitCall(expression: IrCall, data: VisitorData) {
                    renderTargetName = this@GlslTranspiler.visitCall(expression, data)
                }
            }, data)
            return renderTargetName!!
        }

        val functionName = expression.symbol.owner.name.asString()

        return when {
            expression.origin == IrStatementOrigin.GET_PROPERTY -> { //FIXME && owner is direct value (not call etc.)
                var propertyOwner: String? = null
                expression.acceptChildren(object : IrVisitor<Unit, VisitorData>() {
                    override fun visitElement(element: IrElement, data: VisitorData) {
                        propertyOwner = element.accept(this@GlslTranspiler, data)
                    }
                }, data)

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
                val p = eval(expression, data)!!
                "${if (functionName == "unaryPlus") "+" else "-"}$p"
            }

            functionName in operatorNames -> handleOperator(functionName, expression, data)
            functionName in functionNames -> handleFunction(functionName, expression, data)
            else -> error("Unexpected function ${expression.render()}")
        }
    }

//    override fun visitWhen(expression: IrWhen, data: VisitorData): String {
//        if (expression.origin!! != IrStatementOrigin.IF) TODO()
//        error(expression.dump())
//        expression.branch
//    }

    override fun visitReturn(expression: IrReturn, data: VisitorData): String =
        expression.value.accept(GlslReturnTranspiler(this), data)

    override fun visitConst(expression: IrConst, data: VisitorData): String =
        expression.value.toString()

    override fun visitGetValue(expression: IrGetValue, data: VisitorData): String =
        expression.symbol.owner.name.asString()

    private val assignments = mapOf(
        EQ to "=",
        PLUSEQ to "+=",
        MINUSEQ to "-=",
        MULTEQ to "*=",
        DIVEQ to "/=",
        PERCEQ to "%="
    )

    override fun visitSetValue(expression: IrSetValue, data: VisitorData): String {
        return when (expression.origin) {
            in assignments -> {
                val leftSide = expression.symbol.owner.name.asString()
                val rightSide = (expression.value as IrCall) //TODO wrapper funcs, especially for getting params
                    .getArgumentsWithIr()
                    .first { it.first.name.asString() != "<this>" } //TODO receiver helper funcs
                    .second.accept(this, data)
                "$leftSide ${assignments[expression.origin]} $rightSide"
            }

            else -> TODO()
        }
    }

    private fun handleOperator(functionName: String, expression: IrCall, data: VisitorData): String =
        "${(expression.dispatchReceiver ?: expression.extensionReceiver)!!.accept(this, data)} ${
            operatorNames[functionName]!!
        } ${expression.getValueArgument(0)!!.accept(this, data)}"

    private fun handleFunction(functionName: String, expression: IrCall, data: VisitorData): String = expression
        .valueArguments
        .joinToString(", ", prefix = "$functionName(", postfix = ")") {
            it!!.accept(this, data)
        }

}

private class GlslReturnTranspiler(val root: GlslTranspiler) : IrVisitor<String, VisitorData>() {
    override fun visitElement(element: IrElement, data: VisitorData): String {
        TODO("Element of type ${element::class.simpleName} cannot be processed yet:\n${element.dump()}")
    }

    override fun visitConstructorCall(expression: IrConstructorCall, data: VisitorData): String {
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
    override fun visitCall(expression: IrCall, data: VisitorData): String {
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

private fun GlslTranspiler.eval(expression: IrExpression, data: VisitorData): String? {
    var value: String? = null
    expression.acceptChildren(object : IrVisitor<Unit, VisitorData>() {
        override fun visitElement(element: IrElement, data: VisitorData) {
            value = element.accept(this@eval, data)
        }
    }, data)
    return value
}
