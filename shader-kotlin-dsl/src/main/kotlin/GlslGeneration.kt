package io.github.etieskrill.injection.extension.shader.dsl

import io.github.etieskrill.injection.extension.shader.ShaderStage
import io.github.etieskrill.injection.extension.shader.ShaderStage.*
import io.github.etieskrill.injection.extension.shader.dsl.GlslStorageQualifier.CONST
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin.Companion.DIVEQ
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin.Companion.EQ
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin.Companion.MINUSEQ
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin.Companion.MULTEQ
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin.Companion.PERCEQ
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin.Companion.PLUSEQ
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.isArray
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrVisitor

private const val GL_POSITION_NAME = "gl_Position"
internal val POSITION_FIELD_NAME = ShaderVertexData::position.name

private val properties = mapOf(
    VERTEX to mapOf(
        VertexReceiver::vertexID.name to "gl_VertexID"
    )
)

private enum class OperatorType(
    val operator: String
) { PLUS("+"), MINUS("-"), TIMES("*"), DIV("/"), REM("%"), FUN("<fun>") }

private val String.glslOperator get() = OperatorType.entries.firstOrNull { it.name.equals(this, true) }
private val String.isGlslOperator get() = glslOperator != null

private enum class WeirdIrResolvedOperatorType(
    val operator: String
) { GREATER(">"), LESS("<"), GREATEROREQUAL(">="), LESSOREQUAL("<=") }

private val String.resolvedGlslOperator
    get() = WeirdIrResolvedOperatorType.entries.firstOrNull {
        it.name.equals(
            this,
            true
        )
    }
private val String.isResolvedGlslOperator get() = resolvedGlslOperator != null

private val builtinFunctionNames = GlslReceiver::class.members
    .filterNot { it.name in listOf("equals", "hashCode", "toString") }
    .map { it.name }

private data class TranspilerData(
    val pluginContext: IrPluginContext,
    val programParent: IrDeclarationParent,
    val structTypes: List<IrType>,
    val definedFunctions: Map<String, Pair<ShaderStage, IrFunction>>,
    val vertexDataStructName: String,
    var stage: ShaderStage = NONE,
    var operatorStack: MutableList<OperatorType> = mutableListOf(), //TODO remove brackets where neighboring operators are same or less binding
) {
    val irFunctions: List<IrFunction>
        get() = definedFunctions.map { it.value.second }
}

//TODO:
// - detect usages for uniforms, consts and functions and if only used in one stage, move there
// - evidently functions with dependencies on stage-specific vars/ins/outs should be in said stage - also the vertexData variable (better name?) must be treated properly as property getter
internal fun generateGlsl(programData: VisitorData, pluginContext: IrPluginContext): String = buildIndentedString {
    val transpilerData = TranspilerData(
        pluginContext,
        programData.programParent,
        programData.structTypes,
        programData.definedFunctions,
        programData.vertexDataStructName
    )

    appendLine(
        "#version ${programData.version} ${
            programData.profile
                .takeIf { it == GlslProfile.CORE }?.name?.lowercase()
        }"
    )
    newline()

    appendLine(
        """
        // This is automatically generated code. 
        // It should not be manually edited, and any changes will be lost upon recompilation.
    """.trimIndent()
    )
    newline()

    val generateVertexStruct = programData.vertexDataStruct.isNotEmpty()

    if (generateVertexStruct) {
        appendLine(generateStruct(programData.vertexDataStructType, programData.vertexDataStruct))
        newline()
    }

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

    programData.definedFunctions.filter { it.value.first == NONE }.forEach { (_, function) ->
        appendLine(generateFunction(function.second, transpilerData))
    }
    newline()

    transpilerData.stage = VERTEX

    appendLine(generateStage(VERTEX))
    newline()

    programData.vertexAttributes.forEach { (name, type) ->
        appendLine(generateStatement(GlslStorageQualifier.IN, type, name))
    }
    newline()

    if (generateVertexStruct) {
        appendLine(generateStatement(GlslStorageQualifier.OUT, programData.vertexDataStructType, "vertex"))
        newline()
    }

    programData.definedFunctions.filter { it.value.first == VERTEX }.forEach { (_, function) ->
        appendLine(generateFunction(function.second, transpilerData))
    }
    newline()

    appendLine(generateMain(programData.stageBodies[VERTEX]!!, transpilerData))
    newline()

    transpilerData.stage = FRAGMENT

    appendLine(generateStage(FRAGMENT))
    newline()

    if (generateVertexStruct) {
        appendLine(generateStatement(GlslStorageQualifier.IN, programData.vertexDataStructType, "vertex"))
        newline()
    }

    programData.renderTargets.forEach {
        appendLine(generateStatement(GlslStorageQualifier.OUT, "vec4", it))
    }
    newline()

    programData.definedFunctions.filter { it.value.first == FRAGMENT }.forEach { (_, function) ->
        appendLine(generateFunction(function.second, transpilerData))
    }
    newline()

    appendLine(generateMain(programData.stageBodies[FRAGMENT]!!, transpilerData))
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

@OptIn(UnsafeDuringIrConstructionAPI::class)
private fun generateFunction(function: IrFunction, data: TranspilerData) = buildIndentedString {
    val returnType = data.resolveGlslType(function.returnType)
    val parameters = function.valueParameters
        .onEach { check(it.defaultValue == null) { TODO("Default value splitting... mangling i think it's called?") } }
        .joinToString(",") { "${data.resolveGlslType(it.type)} ${it.name.asString()}" }

    val body = function.body!!.findElement<IrBlockBody>()!!

    val returnStatement = body.findElement<IrReturn>()!!
    val variableName = returnStatement.value.accept(GlslTranspiler(), data)
    val variable =
        body.findElement<IrVariable> { it.name == (returnStatement.value as? IrGetValue)?.symbol?.owner?.name }
    val ignore = if (variable == null) listOf(returnStatement) else listOf(variable, returnStatement)

    appendLine("void ${function.name}(inout $returnType $variableName, $parameters) {")
    indent {
        appendMultiLine(body.accept(GlslTranspiler(ignore), data))
    }
    append("}")
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

        val statements = body.statements.associateWith { it.accept(GlslTranspiler(unwrapReturn = true), data) }

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
private open class GlslTranspiler(
    val ignoredBlockStatements: List<IrStatement> = listOf(), //FIXME quite ugly, but effective as i do not know how to property transform the ir tree, and i'm walking a little on the edge with the ones i am doing (and actually, in the case of the godawful "value-return" call convention of glsl; i don't think a valid transform is possible)
    val unwrapReturn: Boolean = false
) : IrVisitor<String, TranspilerData>() {
    override fun visitElement(element: IrElement, data: TranspilerData): String =
        TODO("Element of type ${element::class.simpleName} cannot be processed yet:\n${element.dump()}")

    override fun visitBlock(expression: IrBlock, data: TranspilerData): String = when {
        expression.origin == IrStatementOrigin.FOR_LOOP -> expression.accept(GlslForLoopTranspiler(this), data)
        else -> expression
            .statements
            .filterNot { ignoredBlockStatements.contains(it) }
            .joinToString(";\n", postfix = ";") { it.accept(this, data) }
    }

    override fun visitBlockBody(body: IrBlockBody, data: TranspilerData): String =
        body.statements
            .filterNot { ignoredBlockStatements.contains(it) }
            .joinToString(";\n") { it.accept(this, data) }

    override fun visitVariable(declaration: IrVariable, data: TranspilerData): String {
        val initialiser = declaration.initializer
            ?.let { " = ${it.accept(this, data)}" }
            .orEmpty()
        return "${declaration.type.glslType} ${declaration.name}${initialiser}"
    }

    override fun visitCall(expression: IrCall, data: TranspilerData): String {
        if (data.stage == FRAGMENT && expression.returns<RenderTarget>()
        ) return evalChildren(expression, data)!!

        val functionName = expression.symbol.owner.name.asString()

        return when {
            expression.origin == IrStatementOrigin.GET_PROPERTY -> {
                var propertyOwner = evalChildren(expression, data)
                val propertyName = functionName.removePrefix("<get-").removeSuffix(">")

                if (propertyOwner!!.startsWith("\$context_receiver_")) { //accessing a stage receiver property
                    return properties[data.stage]!![propertyName]!!
                }

                if (expression.receiver?.type?.isArray() == true) {
                    when (propertyName) {
                        "size" -> return "$propertyOwner.length()"
                    }
                }

                when (propertyOwner) {
                    "<this>" -> propertyName //direct shader class members, e.g. uniforms
                    "it" -> when (data.stage) { //implicit lambda parameter //FIXME find and use declared name if not implicit
                        VERTEX -> propertyName
                        FRAGMENT -> when (propertyName) {
                            POSITION_FIELD_NAME -> GL_POSITION_NAME
                            else -> "${data.vertexDataStructName}.$propertyName"
                        }

                        NONE -> error("Implicit receiver called outside of any stage; presumably stage function trying to generate while not in respective stage")
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

            functionName in data.definedFunctions -> handleFunction(functionName, expression, data)
            functionName.isGlslOperator -> handleOperator(functionName.glslOperator!!, expression, data)
            functionName.isResolvedGlslOperator -> handleResolvedOperator(
                functionName.resolvedGlslOperator!!,
                expression,
                data
            )

            functionName == "toFloat" -> {
                "${expression.receiver!!.accept(this, data)}.0"
            }
            functionName in builtinFunctionNames -> handleFunction(functionName, expression, data)
            functionName.removePrefix("<set-")
                .removeSuffix(">") in builtinFunctionNames -> { //could split using KClass::memberFunctions and KClass::memberProperties if needed
                "${expression.receiver!!.accept(this, data)}.${
                    functionName.removePrefix("<set-").removeSuffix(">")
                } = ${expression.getValueArgument(0)!!.accept(this, data)}"
            }

            else -> error("Unexpected function $functionName:\n${expression.dump()}")
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

    override fun visitReturn(expression: IrReturn, data: TranspilerData): String = when {
        unwrapReturn -> expression.value.accept(GlslReturnTranspiler(this), data)
        else -> "return ${expression.value.accept(this, data)};"
    }

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
        var leftSide: String
        var operator: String
        var rightSideExpression: IrExpression

        when (expression.origin) {
            EQ -> {
                leftSide = expression.symbol.owner.name.asString()
                operator = "="
                rightSideExpression = expression.value
            }

            in assignmentOperators -> {
                leftSide = expression.symbol.owner.name.asString()
                operator = assignmentOperators[expression.origin]!!
                rightSideExpression =
                    (expression.value as IrCall) //TODO wrapper funcs, especially for getting params //FIXME if assignment op is passed, this will be IrGetValue instead of IrCall
                        .getArgumentsWithIr()
                        .first { it.first.name.asString() != "<this>" } //TODO receiver helper funcs
                        .second
            }

            else -> TODO()
        }

        if (rightSideExpression is IrCall && rightSideExpression.symbol.owner in data.irFunctions) {
            return handleFunction(rightSideExpression.symbol.owner.name.asString(), rightSideExpression, data, leftSide)
        }

        return "$leftSide $operator ${rightSideExpression.accept(this, data)}"
    }

    private fun handleOperator(operator: OperatorType, expression: IrCall, data: TranspilerData): String {
        check(
            expression.origin!!.debugName.lowercase() !in OperatorType.entries.map { it.name.lowercase() }
                    || expression.extensionReceiver != null
                    || expression.dispatchReceiver != null
        )
        { "Operator function did not have a receiver: $operator:\n${expression.render()}" }
        return "(${expression.receiver!!.accept(this, data)} ${
            operator.operator
        } ${expression.getValueArgument(0)!!.accept(this, data)})"
    }

    private fun handleResolvedOperator(
        operator: WeirdIrResolvedOperatorType,
        expression: IrCall,
        data: TranspilerData
    ): String =
        "(${expression.getValueArgument(0)!!.accept(this, data)} ${
            operator.operator
        } ${expression.getValueArgument(1)!!.accept(this, data)})"

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    private fun handleFunction(
        functionName: String,
        expression: IrCall,
        data: TranspilerData,
        prefixArgument: String? = null
    ): String {
        val parameters = expression
            .getAllArgumentsWithIr()
            .filterNot { it.first.name.isSpecial }
            .joinToString(", ") { (param, value) ->
                value?.accept(this, data)
                    ?: param.defaultValue?.toString() //FIXME
                    ?: error(
                        "Argument ${param.index} '${
                            param.name.asString()
                        }' of method $functionName was null, but no default value is present:\n${expression.dump()}"
                    )
            }

        return "$functionName(${prefixArgument?.let { "$it, " }.orEmpty()}$parameters)"
    }

    override fun visitValueParameter(declaration: IrValueParameter, data: TranspilerData): String {
        if (declaration.name.asString() == "lod") error(declaration.dump())
        if (declaration.defaultValue != null) error(declaration.dump())
        return super.visitValueParameter(declaration, data)
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
                    VERTEX -> {
                        when (param) {
                            POSITION_FIELD_NAME -> "$GL_POSITION_NAME = $expression;"
                            else -> "${data.vertexDataStructName}.$param = $expression;"
                        }
                    }

                    FRAGMENT -> "$param = $expression;"
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

@OptIn(UnsafeDuringIrConstructionAPI::class)
private class GlslForLoopTranspiler(
    val generalTranspiler: GlslTranspiler
) : IrVisitor<String, TranspilerData>() {
    override fun visitElement(element: IrElement, data: TranspilerData): String = TODO("Not yet implemented")

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    override fun visitBlock(expression: IrBlock, data: TranspilerData): String {
        val iterable = expression.findElement<IrCall> {
            it.origin == IrStatementOrigin.FOR_LOOP_ITERATOR && it.symbol.owner.name.asString() == "iterator"
        }!!.accept(this, data)

        val loop = expression.findElement<IrWhileLoop> { it.origin == IrStatementOrigin.FOR_LOOP_INNER_WHILE }!!

        val loopVariable = loop.body!!
            .findElement<IrVariable> { it.origin == IrDeclarationOrigin.FOR_LOOP_VARIABLE }!!
        val loopVariableType = data.resolveGlslType(loopVariable.type)
        val loopVariableName = loopVariable.name.asString()

        val loopBody = loop.body!!
            .findElement<IrBlock> { it.origin != IrStatementOrigin.FOR_LOOP_INNER_WHILE }!!
            .accept(generalTranspiler, data)

        return buildIndentedString {
            val (start, end) = iterable.split("|")

            appendLine("for ($loopVariableType $loopVariableName = $start; $loopVariableName < $end; $loopVariableName++) {")
            indent { appendMultiLine(loopBody) }
            append("}")
        }
    }

    override fun visitCall(expression: IrCall, data: TranspilerData): String {
        val receiver = expression.receiver as? IrCall
            ?: return super.visitCall(expression, data)

        return when (receiver.symbol.owner.name.asString()) {
            "rangeTo" -> {
                val start = receiver.receiver!!.accept(generalTranspiler, data)
                val end = receiver.getValueArgument(0)!!.accept(generalTranspiler, data)

                "$start|$end" //FIXME beautiful, i know
            }

            else -> TODO()
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

private fun TranspilerData.resolveGlslType(type: IrType): String = when {
    type.equals<Unit>() -> "void"
    type in structTypes -> type.simpleName
    type.glslType != null -> type.glslType!!.type
    else -> error("Could not resolve type in shader: ${type.render()}")
}

private val IrMemberAccessExpression<*>.receiver: IrExpression?
    get() = extensionReceiver ?: dispatchReceiver
