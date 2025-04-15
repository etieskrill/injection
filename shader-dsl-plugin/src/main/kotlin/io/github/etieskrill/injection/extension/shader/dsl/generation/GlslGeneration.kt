@file:OptIn(UnsafeDuringIrConstructionAPI::class, ExperimentalStdlibApi::class)

package io.github.etieskrill.injection.extension.shader.dsl.generation

import io.github.etieskrill.injection.extension.shader.ShaderStage
import io.github.etieskrill.injection.extension.shader.ShaderStage.*
import io.github.etieskrill.injection.extension.shader.dsl.GlslReceiver
import io.github.etieskrill.injection.extension.shader.dsl.RenderTarget
import io.github.etieskrill.injection.extension.shader.dsl.ShaderVertexData
import io.github.etieskrill.injection.extension.shader.dsl.VertexReceiver
import io.github.etieskrill.injection.extension.shader.dsl.generation.GlslStorageQualifier.CONST
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrBlock
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetField
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrMemberAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.expressions.IrSetField
import org.jetbrains.kotlin.ir.expressions.IrSetValue
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin.Companion.DIVEQ
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin.Companion.EQ
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin.Companion.MINUSEQ
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin.Companion.MULTEQ
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin.Companion.PERCEQ
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin.Companion.PLUSEQ
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.expressions.IrVararg
import org.jetbrains.kotlin.ir.expressions.IrWhen
import org.jetbrains.kotlin.ir.expressions.IrWhileLoop
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.isArray
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.getAllArgumentsWithIr
import org.jetbrains.kotlin.ir.util.getArgumentsWithIr
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.IrVisitor

private const val GL_POSITION_NAME = "gl_Position"
internal val POSITION_FIELD_NAME = ShaderVertexData::position.name

private val builtinProperties = mapOf(
    VERTEX to mapOf(
        VertexReceiver::vertexID.name to "gl_VertexID"
    )
)

private enum class OperatorType(
    val operator: String,
    val assignmentOperator: IrStatementOrigin,
    val booleanOperator: Boolean = false
) {
    PLUS("+", PLUSEQ),
    MINUS("-", MINUSEQ),
    TIMES("*", MULTEQ),
    DIV("/", DIVEQ),
    REM("%", PERCEQ),

    EQEQ("==", IrStatementOrigin.EQEQ, true),
    OROR("||", IrStatementOrigin.OROR, true);

    override fun toString(): String = operator
}

private val assignmentOperators = mapOf(
    PLUSEQ to "+=",
    MINUSEQ to "-=",
    MULTEQ to "*=",
    DIVEQ to "/=",
    PERCEQ to "%="
)

private val String.glslOperator get() = OperatorType.entries.firstOrNull { it.name.equals(this, true) }
private val String.isGlslOperator get() = glslOperator != null
private val IrCall.glslOperator get() = symbol.owner.name.asString().glslOperator
private val IrCall.isGlslOperator get() = symbol.owner.name.asString().isGlslOperator

private enum class WeirdIrResolvedOperatorType(val operator: String) {
    GREATER(">"), LESS("<"), GREATEROREQUAL(">="), LESSOREQUAL("<=");

    override fun toString(): String = operator
}

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

internal fun String.isGlslBuiltin() =
    this == GL_POSITION_NAME ||
            this in builtinFunctionNames ||
            this in builtinProperties.flatMap { it.value.values }

private data class TranspilerData(
    val pluginContext: IrPluginContext,
    val messageCollector: MessageCollector,
    val programParent: IrDeclarationParent,
    val file: IrFile,
    val structTypes: List<IrType>,
    val definedFunctions: Map<String, Pair<ShaderStage, IrFunction>>,
    val vertexDataStructName: String,
    val uniforms: Set<String>,
    var stage: ShaderStage = NONE,
    var operatorStack: MutableList<OperatorType> = mutableListOf(), //TODO remove brackets where neighboring operators are same or less binding
) {
    val irFunctions: List<IrFunction>
        get() = definedFunctions.map { it.value.second }
}

//TODO:
// - detect usages for uniforms, consts and functions and if only used in one stage, move there
// - evidently functions with dependencies on stage-specific vars/ins/outs should be in said stage - also the vertexData variable (better name?) must be treated properly as property getter
internal fun generateGlsl(
    programData: VisitorData,
    pluginContext: IrPluginContext,
    messageCollector: MessageCollector
): String = buildIndentedString {
    val transpilerData = TranspilerData(
        pluginContext,
        messageCollector,
        programData.programParent,
        programData.file,
        programData.structTypes,
        programData.definedFunctions,
        programData.vertexDataStructName,
        programData.uniforms.keys
    )

    appendLine(
        "#version ${programData.version} ${
            programData.profile
                .takeIf { it == GlslProfile.CORE }?.name?.lowercase().orEmpty()
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

private open class GlslTranspiler(
    val ignoredBlockStatements: List<IrStatement> = listOf(), //FIXME quite ugly, but effective as i do not know how to property transform the ir tree, and i'm walking a little on the edge with the ones i am doing (and actually, in the case of the godawful "value-return" call convention of glsl; i don't think a valid transform is possible)
    val unwrapReturn: Boolean = false
) : IrVisitor<String, TranspilerData>() {
    override fun visitElement(element: IrElement, data: TranspilerData): String =
        TODO("Element of type ${element::class.simpleName} cannot be processed yet:\n${element.dump()}")

    override fun visitBlock(expression: IrBlock, data: TranspilerData): String = when (expression.origin) {
        IrStatementOrigin.FOR_LOOP -> expression.accept(GlslForLoopTranspiler(this), data)
        in assignmentOperators -> expression.accept(GlslTemporaryVariableTranspiler(this, expression.origin!!), data)
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
        if (declaration.type.glslType == null) data.messageCollector.compilerError(
            "Variable may not have an erased upper bound; change e.g. conditional branches with different return types",
            declaration, data.file
        )
        return "${declaration.type.glslType!!} ${declaration.name}${initialiser}"
    }

    override fun visitCall(expression: IrCall, data: TranspilerData): String {
        if (data.stage == FRAGMENT && expression.returns<RenderTarget>()
        ) return expression.evalChildren(this, data)!!

        val functionName = expression.symbol.owner.name.asString()
        val functionPropertyName = functionName
            .removePrefix("<get-").removeSuffix(">")
            .removePrefix("<set-").removeSuffix(">")

        return when {
            expression.origin == IrStatementOrigin.GET_PROPERTY -> {
                var propertyOwner = expression.evalChildren(this, data)
                val propertyName = functionPropertyName

                if (propertyOwner!!.startsWith("\$context_receiver_")) { //accessing a stage receiver property
                    return builtinProperties[data.stage]!![propertyName]!!
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
                val p = expression.evalChildren(this, data)!!
                "${if (functionName == "unaryPlus") "+" else "-"}$p"
            }

            functionName in data.definedFunctions -> handleFunction(functionName, expression, data)
            functionName.isGlslOperator -> handleOperator(functionName.glslOperator!!, expression, data)
            functionName.isResolvedGlslOperator -> handleResolvedOperator(
                functionName.resolvedGlslOperator!!,
                expression,
                data
            )

            functionName in listOf("toFloat", "toDouble") -> expression.receiver!!.accept(this, data)
            functionName in builtinFunctionNames -> handleFunction(functionName, expression, data)
            functionPropertyName in builtinFunctionNames -> { //could split using KClass::memberFunctions and KClass::memberProperties if needed
                when {
                    functionName.contains("set") -> "${expression.receiver!!.accept(this, data)}.${
                        functionPropertyName
                    } = ${expression.getValueArgument(0)!!.accept(this, data)}"

                    functionName.contains("get") -> "${
                        expression.receiver!!.accept(this, data)
                    }.${functionPropertyName}"

                    else -> error("nuh")
                }
            }

            functionPropertyName in data.uniforms -> {
                data.messageCollector.compilerError(
                    "Uniforms may not be set in program blocks: $functionPropertyName",
                    expression,
                    data.file
                )
            }

            functionName == "equals" -> data.messageCollector.compilerError(
                "Use '==' operator instead of 'equals' function call", expression, data.file
            )

            else -> error("Unexpected function $functionName:\n${expression.dump()}")
        }
    }

    override fun visitWhen(expression: IrWhen, data: TranspilerData): String {
        if (expression.origin!! == IrStatementOrigin.OROR) {
            check(expression.branches.size == 2)

            check(expression.branches[0].result.accept(this, data) == "true")
            val leftSide = expression.branches[0].condition.accept(this, data)

            check(expression.branches[1].condition.accept(this, data) == "true")
            val rightSide = expression.branches[1].result.accept(this, data)

            return "($leftSide || $rightSide)"
        }

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

    override fun visitGetValue(expression: IrGetValue, data: TranspilerData): String {
        return expression.symbol.owner.name.asString()
    }

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

    override fun visitGetField(expression: IrGetField, data: TranspilerData): String =
        expression.receiver?.let { "${it.accept(this, data)}." } + expression.symbol.owner.name.asString()

    override fun visitSetField(expression: IrSetField, data: TranspilerData): String {
        val receiver = expression.receiver!!
        val receiverRepr = when (receiver) {
            is IrGetValue -> receiver.symbol.owner.name.asString()
            is IrTypeOperatorCall -> receiver.argument.accept(this, data)
            else -> TODO()
        }
        var leftSide = "$receiverRepr.${expression.symbol.owner.name.asString()}"
        var operator: String
        var rightSideExpression: IrExpression

        when (expression.origin) {
            EQ -> {
                operator = "="
                rightSideExpression = expression.value
            }

            in assignmentOperators -> {
                error("${expression.dump()}\n$leftSide\n${expression.value.accept(this, data)}")

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

    override fun visitTypeOperator(expression: IrTypeOperatorCall, data: TranspilerData): String =
        expression.argument.accept(this, data)

    private fun handleOperator(operator: OperatorType, expression: IrCall, data: TranspilerData): String {
        if (expression.origin == null) { //TODO oo, look at the CompilerMessageSeverity#OUTPUT description
            data.messageCollector.reportWarning(
                "Consider using an operator instead of a function call",
                expression,
                data.file
            )
        }

        var leftSide: String
        var rightSide: String

        if (!operator.booleanOperator) {
            check(expression.origin?.debugName?.lowercase() !in OperatorType.entries.map { it.name.lowercase() }
                    || expression.receiver != null)
            { "Operator function did not have a receiver: $operator:\n${expression.render()}" }

            leftSide = expression.receiver!!.accept(this, data)
            rightSide = expression.getValueArgument(0)!!.accept(this, data)
        } else {
            leftSide = expression.getValueArgument(0)!!.accept(this, data)
            rightSide = expression.getValueArgument(1)!!.accept(this, data)
        }

        return when (operator) {
            //TODO cosmetics: do proper operator for ints
            OperatorType.REM -> "mod($leftSide, $rightSide)" // on intel iris this just... worked. todd would be proud
            else -> "($leftSide $operator $rightSide)"
        }
    }

    private fun handleResolvedOperator(
        operator: WeirdIrResolvedOperatorType,
        expression: IrCall,
        data: TranspilerData
    ): String =
        "(${expression.getValueArgument(0)!!.accept(this, data)} $operator ${
            expression.getValueArgument(1)!!.accept(this, data)
        })"

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

private class GlslForLoopTranspiler(
    val generalTranspiler: GlslTranspiler
) : IrVisitor<String, TranspilerData>() {
    override fun visitElement(element: IrElement, data: TranspilerData): String = TODO("Not yet implemented")

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

private class GlslTemporaryVariableTranspiler(
    val generalTranspiler: GlslTranspiler,
    val operator: IrStatementOrigin
) : IrVisitor<String, TranspilerData>() {
    override fun visitElement(element: IrElement, data: TranspilerData): String = TODO("Not yet implemented")

    override fun visitBlock(expression: IrBlock, data: TranspilerData): String {
        val temporaryVariable = expression.findElement<IrVariable> { it.name.asString() == "<receiver>" }
            ?: error(expression.dump())
        val temporaryVariableName = temporaryVariable.evalChildren(generalTranspiler, data)!!

        val accessedProperty = expression.findElement<IrCall>()!!.symbol.owner.name.asString()
            .removePrefix("<set-").removeSuffix(">")

        val rightSide = expression.statements.single { it != temporaryVariable }
            .findElement<IrCall>(atDepth = 3) { it.glslOperator?.assignmentOperator == operator }!!
            .getValueArgument(0)!!
            .accept(generalTranspiler, data)
            .replace("<receiver>", temporaryVariableName)

        data.messageCollector.reportWarning(expression.dump(), expression, data.file)
        return "$temporaryVariableName.$accessedProperty ${assignmentOperators[operator]!!} $rightSide"
    }
}

private fun IrStatement.evalChildren(visitor: IrVisitor<String, TranspilerData>, data: TranspilerData): String? {
    var value: String? = null
    acceptChildren(object : IrVisitor<Unit, TranspilerData>() {
        override fun visitElement(element: IrElement, data: TranspilerData) {
            value = element.accept(visitor, data)
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
