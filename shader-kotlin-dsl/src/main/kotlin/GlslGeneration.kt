package io.github.etieskrill.injection.extension.shader.dsl

import io.github.etieskrill.injection.extension.shader.ShaderStage
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.backend.js.utils.valueArguments
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.getArguments
import org.jetbrains.kotlin.ir.visitors.IrVisitor

internal fun generateGlsl(data: VisitorData): String = buildString {
    data.stage = ShaderStage.NONE

    appendLine("#version ${data.version} ${data.profile.takeIf { it == GlslProfile.CORE }?.name?.lowercase()}")
    newline()

    appendLine(generateStruct(data.vertexDataStructType, data.vertexDataStruct))
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

internal enum class GlslStorageQualifier { IN, OUT, UNIFORM }

private fun generateStatement(qualifier: GlslStorageQualifier, type: GlslType, name: String) =
    generateStatement(qualifier, type.type, name)

private fun generateStatement(qualifier: GlslStorageQualifier, type: String, name: String) =
    "${qualifier.name.lowercase()} $type $name;"

private fun generateMain(statements: IrElement, data: VisitorData) = buildIndentedString {
    appendLine("void main() {")
    indent {
        appendMultiLine(statements.accept(GlslTranspiler(), data))
    }
    append("}")
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
private class GlslTranspiler : IrVisitor<String, VisitorData>() {
    override fun visitElement(element: IrElement, data: VisitorData): String =
        TODO("Element of type ${element::class.simpleName} cannot be processed yet:\n${element.dump()}")

    override fun visitBlockBody(body: IrBlockBody, data: VisitorData): String =
        body.statements.joinToString(";\n") { it.accept(this, data) }

    override fun visitVariable(declaration: IrVariable, data: VisitorData): String =
        "${declaration.type.glslType} ${declaration.name} = ${declaration.initializer!!.accept(this, data)}"

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
            expression.origin == IrStatementOrigin.GET_PROPERTY -> {
                var propertyOwner: String? = null
                expression.acceptChildren(object : IrVisitor<Unit, VisitorData>() {
                    override fun visitElement(element: IrElement, data: VisitorData) = error("uh oh")
                    override fun visitGetValue(expression: IrGetValue, data: VisitorData) {
                        propertyOwner = expression.symbol.owner.name.asString()
                    }
                }, data)

                val propertyName = functionName.removePrefix("<get-").removeSuffix(">")

                when (propertyOwner) {
                    "<this>" -> propertyName //direct shader class members, e.g. uniforms
                    "it" -> when (data.stage) { //implicit lambda parameter
                        ShaderStage.VERTEX -> propertyName
                        ShaderStage.FRAGMENT -> "${data.vertexDataStructName}.$propertyName"
                        else -> TODO("Getter for implicit lambda parameter call not implemented for stage ${data.stage}")
                    }

                    else -> "$propertyOwner.$propertyName"
                }
            }

            functionName == "times" -> handleMul(expression, data)
            functionName == "vec4" -> handleVec4(expression, data)
            else -> error("Unexpected function $functionName")
        }
    }

    override fun visitReturn(expression: IrReturn, data: VisitorData): String =
        expression.value.accept(GlslReturnTranspiler(this), data)

    override fun visitConst(expression: IrConst, data: VisitorData): String =
        expression.value.toString()

    override fun visitGetValue(expression: IrGetValue, data: VisitorData): String =
        expression.symbol.owner.name.asString()

    private fun handleMul(expression: IrCall, data: VisitorData): String =
        "${expression.extensionReceiver!!.accept(this, data)} * ${
            expression.getValueArgument(0)!!.accept(this, data)
        }"

    private fun handleVec4(expression: IrCall, data: VisitorData): String = expression
        .valueArguments
        .joinToString(", ", prefix = "vec4(", postfix = ")") {
            it!!.accept(this, data)
        }
}

private class GlslReturnTranspiler(val root: GlslTranspiler) : IrVisitor<String, VisitorData>() {
    override fun visitElement(element: IrElement, data: VisitorData): String {
        TODO("Element of type ${element::class.simpleName} cannot be processed yet:\n${element.dump()}")
    }

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    override fun visitConstructorCall(expression: IrConstructorCall, data: VisitorData): String {
        return expression
            .getArguments()
            .associate { (param, expression) -> param.name.asString() to expression.accept(root, data) }
            .map { (param, expression) ->
                when (data.stage) {
                    ShaderStage.VERTEX -> "${data.vertexDataStructName}.$param = $expression;"
                    ShaderStage.FRAGMENT -> "$param = $expression;"
                    else -> TODO("Constructor return call not implemented for stage ${data.stage}")
                }
            }
            .joinToString("\n")
    }
}
