package io.github.etieskrill.injection.extension.shader.dsl

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.IrWhen
import org.jetbrains.kotlin.ir.expressions.impl.IrSetValueImpl
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.visitors.IrTransformer

internal enum class InlineStatementType { VARIABLE, SETTER, PARAMETER }

internal data class InlineConditionalTransformerData(
    var type: InlineStatementType? = null,
    var inlineCondition: IrWhen? = null,
    var helperVariable: IrVariable? = null,
    var helperVariableName: String? = null
)

internal class InlineConditionalTransformer(
    val pluginContext: IrPluginContext
) : IrTransformer<InlineConditionalTransformerData>() {
    override fun visitBlockBody(body: IrBlockBody, data: InlineConditionalTransformerData): IrBody {
        super.visitBlockBody(body, data)

        when (data.type) {
            InlineStatementType.VARIABLE -> {
                val condition = data.inlineCondition!!
                val helperVariable = data.helperVariable!!

                helperVariable.initializer = null

                condition.apply {
                    branches.map {
                        it.apply {
                            result = IrSetValueImpl(
                                UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                                type = it.result.type,
                                symbol = helperVariable.symbol,
                                value = it.result,
                                origin = IrStatementOrigin.EQ
                            )
                        }
                    }
                }

                val statementIndex = body.statements.indexOfFirst { it == helperVariable }
                check(statementIndex != -1) { data.inlineCondition!!.dump() }
                body.statements.add(statementIndex + 1, condition)
            }

            InlineStatementType.SETTER, InlineStatementType.PARAMETER -> TODO()
            else -> return body
        }

//        error(buildString {
//            appendLine(data.helperVariableName!!)
//            appendLine(data.inlineCondition!!.dump())
//            appendLine()
//            appendLine(body.dump())
//        })

        return body
    }

    override fun visitVariable(declaration: IrVariable, data: InlineConditionalTransformerData): IrStatement {
        if (data.inlineCondition == null && declaration.initializer is IrWhen) {
            data.type = InlineStatementType.VARIABLE
            data.inlineCondition = declaration.initializer as IrWhen
            data.helperVariable = declaration
            data.helperVariableName = declaration.name.toString()
            return declaration
        }
        return super.visitVariable(declaration, data)
    }

//    override fun visitSetValue(expression: IrSetValue, data: InlineConditionalTransformerData): IrExpression {
//        if (data.inlineCondition == null && expression.value is IrWhen) {
//            data.inlineCondition = expression.value as IrWhen
//            return expression
//        }
//
//        return super.visitSetValue(expression, data)
//    }

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
