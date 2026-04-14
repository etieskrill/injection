package io.github.etieskrill.injection.extension.shader.dsl.generation

import org.jetbrains.kotlin.backend.jvm.ir.hasChild
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.declarations.nameWithPackage
import org.jetbrains.kotlin.ir.expressions.IrBlock
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrContainerExpression
import org.jetbrains.kotlin.ir.expressions.IrDeclarationReference
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrSetValue
import org.jetbrains.kotlin.ir.expressions.IrStatementContainer
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin.Companion.IF
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin.Companion.WHEN
import org.jetbrains.kotlin.ir.expressions.IrWhen
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrSetValueImpl
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.visitors.IrTransformer

internal enum class InlineStatementType { VARIABLE, SETTER, PARAMETER }

internal data class InlineConditionalTransformerData(
    val messageCollector: MessageCollector,
    val file: IrFile,
    val shader: IrClass,
    val iteration: Int,
    var container: IrStatementContainer? = null,
    var type: InlineStatementType? = null,
    var inlineCondition: IrWhen? = null,
    var helperVariable: IrElement? = null,
    var helperVariableName: String? = null
)

internal class InlineConditionalTransformer : IrTransformer<InlineConditionalTransformerData>() {
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    override fun visitBlockBody(body: IrBlockBody, data: InlineConditionalTransformerData): IrBody {
        if (data.container == null) data.container = body
        if (data.type == null) data.container = body
        super.visitBlockBody(body, data)
        if (data.type == null) data.container = body

        if (data.type == null) return body

        val condition = data.inlineCondition!!
        val helperVariable = data.helperVariable!!

        val symbol = when (data.type) {
            InlineStatementType.VARIABLE -> (helperVariable as IrVariable).symbol
            InlineStatementType.SETTER -> (helperVariable as IrSetValue).symbol
            InlineStatementType.PARAMETER -> TODO()
            else -> error("bruh")
        }

        condition.branches.map {
            it.apply {
                result = IrBlockImpl(
                    startOffset, endOffset, result.type,
                    statements = listOf(
                        IrSetValueImpl(
                            startOffset, endOffset,
                            type = result.type,
                            symbol = symbol,
                            value = result,
                            origin = IrStatementOrigin.EQ
                        )
                    )
                )
            }
        }

        when (data.type) {
            InlineStatementType.VARIABLE -> {
                (helperVariable as IrVariable).initializer = null
                val statementIndex = data.container!!.statements.indexOfFirst { it == helperVariable }
                check(statementIndex != -1) { data.container!!.dump() }
                data.container!!.statements.add(statementIndex + 1, condition)
            }
            InlineStatementType.SETTER -> {
                val statementIndex = data.container!!.statements
                    .filterIsInstance<IrDeclarationReference>()
                    .indexOfFirst { it == helperVariable }
                check(statementIndex != -1) { helperVariable.dump() + "\n\n" + data.container!!.dump() }
                data.container!!.statements.remove(helperVariable)
                data.container!!.statements.add(statementIndex, data.inlineCondition!!)
            }
            InlineStatementType.PARAMETER -> TODO()
            else -> error("oh noes")
        }

        return body
    }

    override fun visitContainerExpression(
        expression: IrContainerExpression,
        data: InlineConditionalTransformerData
    ): IrExpression { //IrBlock is IrContainerExpression
        //FIXME this is hyper ugly and quite difficult to understand, and can surely be done in a simpler way?
        if (data.container == null) data.container = expression
        if (data.type == null && expression.origin in listOf(IF, WHEN, null)) data.container = expression
        val new = super.visitContainerExpression(expression, data)
        //FIXME: with multiple inline ifs in the same block: i think data.container is not reset properly when moving to an outer scope
        if (data.type == null && expression.origin in listOf(IF, WHEN, null)) data.container = expression
        return new
    }

    override fun visitVariable(declaration: IrVariable, data: InlineConditionalTransformerData): IrStatement {
        if ((data.inlineCondition == null && declaration.initializer is IrWhen
                    || declaration.initializer is IrBlock && (declaration.initializer as IrBlock).hasChild { it is IrWhen })
            && (declaration.initializer as IrWhen).origin in listOf(IF, WHEN)
        ) {
            check(data.container != null) { data.file.nameWithPackage }
            data.type = InlineStatementType.VARIABLE
            data.inlineCondition = declaration.initializer as IrWhen
            data.helperVariable = declaration
            data.helperVariableName = declaration.name.toString()
            return declaration
        }
        return super.visitVariable(declaration, data)
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    override fun visitSetValue(expression: IrSetValue, data: InlineConditionalTransformerData): IrExpression {
        if (data.inlineCondition == null && expression.value is IrWhen
            || (expression.value is IrBlock
                    && (expression.value as IrBlock).hasChild { it is IrWhen && it.origin in listOf(IF, WHEN) })
        ) {
            check(data.container != null) { data.file.nameWithPackage }
            data.type = InlineStatementType.SETTER
            data.inlineCondition = (expression.value as IrBlock).findElement<IrWhen>(atDepth = 1)!!
            data.helperVariable = expression
            data.helperVariableName = expression.symbol.owner.name.toString()
            return expression
        }

        return super.visitSetValue(expression, data)
    }

    //TODO basic idea:
    // - find first (top down) inline condition
    // - memorise and return to parent
    // - in all possible parents (IrSetValue, IrVariable etc.) look for memorised condition
    //   - if found, transform to non-inline
    //   - (opt) if only two branches and if both args are vars/consts or below ... 10 chars rendered, then do ternary
    // and this probably for only one single expression per transformer run - for simplicity
//    override fun visitExpression(expression: IrExpression, data: InlineConditionalTransformerData): IrExpression {
//        if (data.inlineCondition == null && expression is IrWhen) {
//            data.inlineCondition = expression
//            return expression
//        }
//
//        super.visitExpression(expression, data)
//        return expression
//    }

//    override fun visitCall(expression: IrCall, data: InlineConditionalTransformerData): IrElement {
//        val inlineWhenParam = expression.getArgumentsWithIr()
//            .map { it.second }
//            .findIsInstanceAnd<IrWhen> { true }
//            ?: return super.visitCall(expression, data)
//
//        data.inlineCondition = inlineWhenParam
//
//        return super.visitCall(expression, data)
//    }

}
