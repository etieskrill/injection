@file:OptIn(UnsafeDuringIrConstructionAPI::class)

package io.github.etieskrill.injection.extension.shader.dsl.generation

import io.github.etieskrill.injection.extension.shader.ShaderStage
import io.github.etieskrill.injection.extension.shader.ShaderStage.*
import io.github.etieskrill.injection.extension.shader.dsl.FragmentReceiver
import io.github.etieskrill.injection.extension.shader.dsl.GlslReceiver
import io.github.etieskrill.injection.extension.shader.dsl.RenderTarget
import io.github.etieskrill.injection.extension.shader.dsl.ShaderVertexData
import io.github.etieskrill.injection.extension.shader.dsl.VertexReceiver
import io.github.etieskrill.injection.extension.shader.dsl.generation.GlslStorageQualifier.CONST
import io.github.etieskrill.injection.extension.shader.vec3
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.backend.js.utils.valueArguments
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
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
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
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
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.IrVisitor
import java.lang.reflect.InvocationTargetException
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.full.valueParameters

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
    OROR("||", IrStatementOrigin.OROR, true),
    ANDAND("&&", IrStatementOrigin.ANDAND, true),

    ieee754equals("==", IrStatementOrigin.EQEQ, true); //just don't question it

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
    get() = WeirdIrResolvedOperatorType.entries.firstOrNull { it.name.equals(this, true) }
private val String.isResolvedGlslOperator get() = resolvedGlslOperator != null

private val unaryOperators = mapOf("not" to "!", "unaryPlus" to "+", "unaryMinus" to "-")

private val builtinFunctionNames = //actually includes all members, i.e. properties too
    GlslReceiver::class.memberNames + VertexReceiver::class.memberNames + FragmentReceiver::class.memberNames

private val KClass<*>.memberNames
    get() = members
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
    val evaluatedFunctions: Map<String, List<KCallable<*>>>,
    val templateFunctions: Map<String, Map<List<String>, String>>,
    val vertexDataStructName: String,
    val vertexDataStruct: Map<String, GlslType>,
    val renderTargetNames: Map<String, String>,
    val uniforms: Set<String>,
    var stage: ShaderStage = NONE,
    var operatorStack: MutableList<OperatorType> = mutableListOf(), //TODO remove brackets where neighboring operators are same or less binding
    var useFragmentVertexPositionAccessor: Boolean = false
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
    val genFragPosAccessor = programData.stageBodies[FRAGMENT]!!
        .findElement<IrCall> {
            val functionPropertyName = it.symbol.owner.name.asString()
                .removePrefix("<get-").removeSuffix(">")
            it.origin == IrStatementOrigin.GET_PROPERTY && functionPropertyName == POSITION_FIELD_NAME
        } != null

    val transpilerData = TranspilerData(
        pluginContext,
        messageCollector,
        programData.programParent,
        programData.file,
        programData.structTypes,
        programData.definedFunctions,
        programData.evaluatedFunctions,
        programData.templateFunctions,
        programData.vertexDataStructName,
        programData.vertexDataStruct.let {
            if (genFragPosAccessor) {
                mutableMapOf("position" to GlslType("vec4")).apply { putAll(it) }
            } else it
        },
        programData.renderTargets,
        programData.uniforms.keys,
        useFragmentVertexPositionAccessor = genFragPosAccessor
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

    val generateVertexStruct = transpilerData.vertexDataStruct.isNotEmpty()

    if (generateVertexStruct) {
        appendLine(generateStruct(programData.vertexDataStructType, transpilerData.vertexDataStruct))
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

    programData.arrayUniforms.forEach { (name, typeAndSize) ->
        appendLine(generateArrayStatement(GlslStorageQualifier.UNIFORM, typeAndSize.first, typeAndSize.second, name))
    }
    newline()

    programData.definedFunctions.filter { it.value.first == NONE }.forEach { (_, function) ->
        appendLine(generateFunction(function.second, transpilerData))
        newline()
    }

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
        newline()
    }

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
        appendLine(generateStatement(GlslStorageQualifier.OUT, "vec4", it.value))
    }
    newline()

    programData.definedFunctions.filter { it.value.first == FRAGMENT }.forEach { (_, function) ->
        appendLine(generateFunction(function.second, transpilerData))
        newline()
    }

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
    require(initialiser == null || qualifier == CONST) { "Only const statements may have an initialiser" }
    return "$qualifier $type $name[$size]${initialiser?.let { " = $initialiser" } ?: ""};"
}

private fun generateFunction(function: IrFunction, data: TranspilerData) = buildIndentedString {
    val returnType = data.resolveGlslType(function.returnType)
    val parameters = function.valueParameters
        .onEach { check(it.defaultValue == null) { TODO("Default value splitting... mangling i think it's called?") } }
        .joinToString(",") { "${data.resolveGlslType(it.type)} ${it.name.asString()}" }

    val body = function.body!!.findElement<IrBlockBody>()!!

    appendLine("$returnType ${function.name}($parameters) {")
    indent {
        appendMultiLine(body.accept(GlslTranspiler(), data).appendIfNotEndsIn(";"))
    }
    append("}")
}

private fun generateMain(body: IrBlockBody, data: TranspilerData) = buildIndentedString {
    appendLine("void main() {")
    indent {
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
                val propertyOwner = expression.evalChildren(this, data)
                var propertyName = functionPropertyName

                if (propertyName.startsWith("get")) { //when accessor is defined in java
                    propertyName = propertyName
                        .removePrefix("get")
                        .replaceFirstChar { it.lowercase() }
                }

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
                        FRAGMENT -> "${data.vertexDataStructName}.$propertyName"
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

            functionName in unaryOperators -> unaryOperators[functionName]!! + expression.evalChildren(this, data)!!
            functionName in data.definedFunctions -> handleFunction(functionName, expression, data)
            functionName in data.evaluatedFunctions -> evaluateFunction(functionName, expression, data)
            functionName in data.templateFunctions -> applyFunctionTemplate(functionName, expression, data)
            functionName.isGlslOperator -> handleOperator(functionName.glslOperator!!, expression, data)
            functionName.isResolvedGlslOperator -> {
                handleResolvedOperator(functionName.resolvedGlslOperator!!, expression, data)
            }

            functionName == "CHECK_NOT_NULL" -> expression.arguments[0]!!.accept(this, data)
            functionName in listOf("toFloat", "toDouble") -> expression.receiver!!.accept(this, data)
            functionName == "discard" -> "discard"
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
        val booleanOperator = OperatorType.entries
            .filter { it.booleanOperator }
            .find { it.assignmentOperator == expression.origin!! }
        if (booleanOperator != null) {
            check(expression.branches.size == 2)

            val leftSide: String
            val rightSide: String

            when (booleanOperator) {
                OperatorType.OROR -> {
                    check(expression.branches[0].result.accept(this, data) == "true")
                    leftSide = expression.branches[0].condition.accept(this, data)

                    check(expression.branches[1].condition.accept(this, data) == "true")
                    rightSide = expression.branches[1].result.accept(this, data)
                }

                OperatorType.ANDAND -> {
                    leftSide = expression.branches[0].condition.accept(this, data)
                    rightSide = expression.branches[0].result.accept(this, data)

                    check(expression.branches[1].condition.accept(this, data) == "true") //artificial else branch
                }

                else -> TODO(expression.dump())
            }

            return "($leftSide ${booleanOperator.operator} $rightSide)"
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

            buildIndentedString {
                appendLine("$conditionStatement {")
                indent {
                    appendMultiLine(result.appendIfNotEndsIn(";"))
                }
                append("} ")
            }
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
        val leftSide: String
        val operator: String
        val rightSideExpression: IrExpression

        when (expression.origin) {
            EQ -> {
                leftSide = expression.symbol.owner.name.asString()
                operator = "="
                rightSideExpression = expression.value
            }

            in assignmentOperators -> {
                leftSide = expression.symbol.owner.name.asString()
                operator = assignmentOperators[expression.origin]!!
                val expressionValue: IrCall = when (val expressionValue = expression.value) {
                    is IrCall -> expressionValue //TODO wrapper funcs, especially for getting params //FIXME if assignment op is passed, this will be IrGetValue instead of IrCall
                    is IrTypeOperatorCall -> {
                        check(expressionValue.operator == IrTypeOperator.IMPLICIT_NOTNULL)
                        expressionValue.argument as IrCall
                    }

                    else -> error("Unknown assignment operator right side:\n${expression.dump()}")
                }
                rightSideExpression = expressionValue
                    .getArgumentsWithIr()
                    .first { it.first.name.asString() != "<this>" } //TODO receiver helper funcs
                    .second
            }

            else -> TODO()
        }

        return "$leftSide $operator ${rightSideExpression.accept(this, data)}"
    }

    override fun visitGetField(expression: IrGetField, data: TranspilerData): String =
        expression.receiver?.let { "${it.accept(this, data)}." } + expression.symbol.owner.name.asString()

    override fun visitSetField(expression: IrSetField, data: TranspilerData): String {
        val receiverRepr = when (val receiver = expression.receiver!!) {
            is IrGetValue -> receiver.symbol.owner.name.asString()
            is IrTypeOperatorCall -> receiver.argument.accept(this, data)
            else -> TODO()
        }
        val leftSide = "$receiverRepr.${expression.symbol.owner.name.asString()}"
        val operator: String
        val rightSideExpression: IrExpression

        when (expression.origin) {
            EQ -> {
                operator = "="
                rightSideExpression = expression.value
            }

            in assignmentOperators -> {
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
            return handleFunction(rightSideExpression.symbol.owner.name.asString(), rightSideExpression, data)
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

        val leftSide: String
        val rightSide: String

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
    ): String {
        val leftSide: String
        val rightSide: String

        val firstArg = expression.getValueArgument(0)!!
        if (firstArg is IrCall && firstArg.symbol.owner.name.asString() == "compareTo") {
            //happens during lowering to only use compareTo depending on the operator
            leftSide = firstArg.receiver!!.accept(this, data)
            rightSide = firstArg.getValueArgument(0)!!.accept(this, data)
        } else {
            leftSide = firstArg.accept(this, data)
            rightSide = expression.getValueArgument(1)!!.accept(this, data)
        }

        return "($leftSide $operator $rightSide)"
    }

    private fun handleFunction(
        functionName: String,
        expression: IrCall,
        data: TranspilerData,
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

        return "$functionName($parameters)"
    }

    private fun evaluateFunction(
        functionName: String,
        expression: IrCall,
        data: TranspilerData
    ): String {
        val function = data.evaluatedFunctions[functionName]!!
            .single { function ->
                function.valueParameters.map { (it.type.classifier!! as KClass<*>).qualifiedName!! }
                    .containsAll(expression.valueArguments.filterNotNull().map { it.type.fullName })
            }

        val arguments: List<Any> = expression.valueArguments.filterNotNull().map {
            if (!it.type.equals<String>()) TODO("Parse value type: ${it.type.simpleName}")
            it.accept(this, data)
        }

        val result: Any
        try {
            result = function.call(*arguments.toTypedArray())!!
        } catch (e: InvocationTargetException) {
            data.messageCollector.compilerError(e.targetException.message ?: "<No message>", expression, data.file)
        }

        return stringify(result)
    }

    private fun applyFunctionTemplate(
        functionName: String,
        expression: IrCall,
        data: TranspilerData
    ): String {
        val argumentTypeNames = expression.valueArguments.filterNotNull().map { it.type.fullName }
        var template: String = data.templateFunctions[functionName]!!
            .let { templates -> templates[argumentTypeNames] } //extrapolated for clarity
            ?: error("Failed to find matching overload for template function '$functionName(${argumentTypeNames.joinToString()})'")

        val arguments = expression.symbol.owner.valueParameters.associate { it.index to it.name }
        arguments.forEach { (index, name) ->
            template = template.replace("\$$name", expression.getValueArgument(index)!!.accept(this, data))
        }

        check(!template.contains('$')) { "Not all parameters for function template could be resolved: $template" }

        return template
    }

    private fun stringify(obj: Any) = when (obj) {
        is vec3 -> "vec3(%.3f, %.3f, %.3f)".format(obj.x(), obj.y(), obj.z())
        else -> TODO("stringify type ${obj::class.simpleName}?")
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
                            POSITION_FIELD_NAME -> {
                                "$GL_POSITION_NAME = $expression;" + if (data.useFragmentVertexPositionAccessor) {
                                    "\n${data.vertexDataStructName}.$POSITION_FIELD_NAME = $GL_POSITION_NAME;"
                                } else ""
                            }
                            else -> "${data.vertexDataStructName}.$param = $expression;"
                        }
                    }

                    FRAGMENT -> "${data.renderTargetNames[param]} = $expression;"
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
    this?.fullName.let { it == T::class.qualifiedName!! }

internal inline fun <reified T> IrExpression.returns() = type.equals<T>()

private fun TranspilerData.resolveGlslType(type: IrType): String = when {
    type.equals<Unit>() -> "void"
    type in structTypes -> type.simpleName
    type.glslType != null -> type.glslType!!.type
    else -> error("Could not resolve type in shader: ${type.render()}")
}

private val IrMemberAccessExpression<*>.receiver: IrExpression?
    get() = extensionReceiver ?: dispatchReceiver
