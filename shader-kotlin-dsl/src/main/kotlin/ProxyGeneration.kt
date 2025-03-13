package io.github.etieskrill.injection.extension.shader.dsl

import io.github.etieskrill.injection.extension.shader.*
import io.github.etieskrill.injection.extension.shader.dsl.ProxyShaderBuilder.MethodProgramStatement
import io.github.etieskrill.injection.extension.shader.dsl.ProxyShaderBuilder.UniformStatement
import net.bytebuddy.ByteBuddy
import net.bytebuddy.implementation.InvocationHandlerAdapter
import net.bytebuddy.matcher.ElementMatchers
import org.intellij.lang.annotations.Language
import org.joml.Vector4f
import java.io.File
import java.lang.reflect.Method
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty

@JvmInline
internal value class ObjectRef(private val reference: String) {
    override fun toString(): String = reference
}

private typealias ProxyLookup = MutableMap<ObjectRef, Any>

@ShaderDslMarker
abstract class ProxyShaderBuilder<VA : Any, V : Any, RT : Any>(
    internal val vertexAttributesClass: KClass<VA>,
    internal val vertexClass: KClass<V>,
    internal val renderTargetsClass: KClass<RT>,
    shader: AbstractShader
) : AbstractShader by shader {

    var vertexData: VA? = null

    internal val proxyLookup: ProxyLookup = mutableMapOf()

    private val vertexAttributes: VA = proxyObject(vertexAttributesClass, "data", proxyLookup)
    private val vertex: V = proxyObject(vertexClass, "data", proxyLookup)

    private val vertexAttributeStatements = mutableMapOf<String, KClass<*>>()
    private val vertexDataStatements = mutableMapOf<String, KClass<*>>()
    private val renderTargetStatements = mutableMapOf<String, KClass<*>>()

    internal var interceptProxyCalls = false
    private var generationDone = false

    internal var stage = ShaderStage.NONE
    internal var callDepth = -1

    internal val uniformStatements = mutableMapOf<ObjectRef, UniformStatement>()

    internal val programStatements = mutableListOf<ProgramStatement>()

    internal data class UniformStatement(
        val type: KClass<*>,
        val proxy: Any,
        var name: String? = null
    ) {
        val used: Boolean
            get() = name != null
    }

    internal open class ProgramStatement(
        open val callDepth: Int,
        open val stage: ShaderStage
    )

    private data class UniformProgramStatement(
        val uniform: UniformStatement,
        override val callDepth: Int,
        override val stage: ShaderStage
    ) : ProgramStatement(callDepth, stage)

    internal data class MethodProgramStatement(
        val returnType: KClass<*>,
        val returnValue: Any,
        override val callDepth: Int,
        override val stage: ShaderStage,
        val method: Method?,
        val name: String,
        val `this`: Any?,
        val args: List<*>
    ) : ProgramStatement(callDepth, stage)

    internal data class ReturnValueProgramStatement(
        val value: Any,
        override val callDepth: Int,
        override val stage: ShaderStage
    ) : ProgramStatement(callDepth, stage)

    init {
        vertexAttributesClass.check()
        vertexClass.check()
        renderTargetsClass.checkOpen()

        vertexAttributeStatements.putAll(vertexAttributesClass.getFields("vertex attributes"))
        vertexDataStatements.putAll(vertexClass.getFields("vertex data fields"))
        renderTargetStatements.putAll(renderTargetsClass.getFields("render targets"))
    }

    private fun KClass<*>.check() {
        checkOpen()
        require(constructors.any {
            it.parameters.isEmpty() || it.parameters.all { it.isOptional }
        }) { "${this.simpleName} constructor must have a no-arg constructor" }
    }

    private fun KClass<*>.checkOpen() =
        require(isOpen) { "${this.simpleName} class must be open" }

    private fun KClass<*>.getFields(type: String): Map<String, KClass<*>> = members
        .filterNot { it.name in listOf("equals", "hashCode", "toString") }
        .onEach { field ->
            require(field !is KMutableProperty)
            { "All $type should be immutable: ${simpleName}.${field.name} is mutable" }
            require(field.isOpen)
            { "All $type must be open: ${simpleName}.${field.name} is not open" }
        }
        .associate { it.name to it.returnType.classifier as KClass<*> }

    inner class UniformDelegate<T : Any>(clazz: KClass<T>) :
        ReadWriteProperty<ProxyShaderBuilder<VA, V, RT>, T> { //FIXME would have to be protected and internal... has this really never been a usecase?
        private var value: T = proxyObject(clazz, "uniform", proxyLookup)

        init {
            this@ProxyShaderBuilder.uniformStatements[value.id] = UniformStatement(
                clazz,
                value
            ) //using the system object id, because hashCode logically returns identical values for identical matrices
        }

        override fun getValue(thisRef: ProxyShaderBuilder<VA, V, RT>, property: KProperty<*>): T =
            if (!generationDone) programGetter(property)
            else getter()

        private fun programGetter(property: KProperty<*>): T {
            val uniformStatement = this@ProxyShaderBuilder.uniformStatements[value.id]!!
            if (!uniformStatement.used) uniformStatement.name = property.name
            this@ProxyShaderBuilder.programStatements.add(
                UniformProgramStatement(
                    uniformStatement,
                    callDepth,
                    ShaderStage.NONE
                )
            )
            return value
        }

        private fun getter(): T = TODO("AbstractShader is gonna have to grow some getters")

        override fun setValue(thisRef: ProxyShaderBuilder<VA, V, RT>, property: KProperty<*>, value: T) {
            check(generationDone) { "Uniforms must not be set in program block" }
            this.value =
                value //property is only writeable to allow uniform to actually be set from outside without requiring another field
            setUniform(property.name, value)
        }
    }

    protected inline fun <reified T : Any> uniform() = UniformDelegate(T::class)

    protected fun vertex(block: ProxyVertexReceiver.(VA) -> V) {
        stage = ShaderStage.VERTEX
        interceptProxyCalls = true
        val vertex = block(ProxyVertexReceiver(this), vertexAttributes)
        programStatements.add(ReturnValueProgramStatement(vertex, callDepth, ShaderStage.VERTEX))
        interceptProxyCalls = false
    }

    protected fun fragment(block: ProxyFragmentReceiver.(V) -> RT) {
        stage = ShaderStage.FRAGMENT
        interceptProxyCalls = true
        val renderTargets = block(ProxyFragmentReceiver(this), vertex)
        programStatements.add(ReturnValueProgramStatement(renderTargets, callDepth, ShaderStage.FRAGMENT))
        interceptProxyCalls = false
    }

//    init {
//        program()
//        check(callDepth == -1) { "Not all proxied function calls returned. This... should not happen." }
//    }

    protected abstract fun program()

    internal fun generateGlsl() {
        val source = buildString {
            glslVersion()
            newline()
            glslStruct(vertexClass, vertexDataStatements)
            newline()
            glslUniforms(uniformStatements.values.toList())
            newline()

            glslStage(ShaderStage.VERTEX)
            newline()
            glslVertexAttributes(vertexAttributeStatements)
            newline()
            glslStatement(GlslStorageQualifier.OUT, vertexClass, "vertex")
            newline()
            glslMain(ShaderStage.VERTEX, this@ProxyShaderBuilder)
            newline()

//            glslStage(ShaderStage.FRAGMENT)
//            newline()
//            glslStatement(GlslStorageQualifier.IN, vertexClass, "vertex")
//            newline()
//            glslRenderTargets(renderTargetStatements)
//            newline()
//            glslMain(ShaderStage.FRAGMENT, this@ShaderBuilder)
        }

        File("Test.glsl").writeText(source)

        generationDone = true
    }

}

private fun StringBuilder.glslVersion(): StringBuilder = appendLine("#version 330 core") //TODO

private fun StringBuilder.glslUniforms(uniforms: List<UniformStatement>) = appendLine(
    uniforms
        .filter { it.used }
        .joinToString("\n") { "uniform ${it.type.glslName} ${it.name};" })

private fun StringBuilder.glslStage(stage: ShaderStage): StringBuilder =
    appendLine("#pragma stage ${stage.name.lowercase()}")

private fun StringBuilder.glslVertexAttributes(attributes: Map<String, KClass<*>>): StringBuilder =
    append(attributes.map { (name, type) ->
        "in ${type.glslName} $name;"
    }.joinToString("\n", postfix = "\n"))

private fun StringBuilder.glslStruct(type: KClass<*>, members: Map<String, KClass<*>>): StringBuilder =
    appendLine(buildString {
        appendLine("struct ${type.simpleName} {")
        appendLine(
            members
                .map { (name, type) -> "\t${type.glslName} $name;" }
                .joinToString("\n"))
        append("};")
    })

private fun StringBuilder.glslMain(stage: ShaderStage, shader: ProxyShaderBuilder<*, *, *>) = appendLine(buildString {
    appendLine("void main() {")

    val programStatements = shader.programStatements
        .filter { it.stage == stage }
        .filter { it.callDepth <= 0 }

    val vertexAttributes = programStatements
        .filterIsInstance<MethodProgramStatement>()
        .filter { it.`this` != null && it.`this`::class == shader.vertexAttributesClass }
        .associate {
            it.returnValue.id to it.name.removePrefix("get").replaceFirstChar { it.lowercase() }
        } //TODO crosscheck name with context

    val context = ProgramContext(shader, vertexAttributes)

    val statements = programStatements
        .filterNot { it is MethodProgramStatement && (it.method?.name ?: it.name).startsWith("get") }
        .take(4)
        .map { statement ->
            when (statement) {
                is MethodProgramStatement -> generateMethodStatement(statement, context)
                is ProxyShaderBuilder.ReturnValueProgramStatement -> generateReturnValueStatement(statement, context)
                else -> error("Unknown program statement type: ${statement::class.simpleName}")
            }
        }
    appendLine(statements.filterNotNull().joinToString("\n"))

    append("}")
})

private fun generateMethodStatement(
    statement: MethodProgramStatement,
    context: ProgramContext
): String? {
    val helperVariableType = when (statement.returnType) {
        context.shader.vertexAttributesClass, context.shader.vertexClass, context.shader.renderTargetsClass
            -> statement.returnType.simpleName

        else -> statement.returnType.glslName
    }!!

    val helperVariableName =
        helperVariableType + "_" + context.variableCounters
            .compute(statement.returnType) { _, counter ->
                if (counter == null) 0 else counter + 1
            }

    context.variables[statement.returnValue.id] = helperVariableName to statement.returnType

    val methodName = statement.method?.name ?: statement.name

    val operator = methodName.glslOperator
    if (operator != null) {
        check(statement.`this` != null) { "Operators must have a left-hand value" }
        check(statement.args.size == 2) { "Operators must have exactly one argument" } //FIXME a bit risky to assume all methods would have other + receiver parameters

        val thisProxy = context.shader.proxyLookup[statement.`this`.id] ?: statement.`this`
        val thisName = thisProxy.resolveNameOrValue(context)
            ?: error("Could not resolve reference for method $methodName: ${thisProxy.id}")

        val argName = statement.args[0]!!.resolveNameOrValue(context)!!
        return "${"\t".repeat(context.callDepth)}${statement.returnType.glslName} $helperVariableName = $thisName $operator $argName;"
    }

    //FIXME especially matrix multiplications should be inlined in hope that a vector is at the front... i really need a better parse tree api. well, not that i have an "api" at all really. we love brute-force parsing here.
    if (statement.`this` == null) { //no receiver means a regular (non-operator) function call, which is probably always inlined
        context.variableCounters.remove(statement.returnType) //much pretty pattern, i know. halp
        context.variables.remove(statement.returnValue.id)

        val args = statement.args
            .map inline@{
                it?.resolveNameOrValue(context)
                    ?: error("Could not resolve argument for method $methodName: ${it?.id}")
            }

        context.inlineMethodCalls[statement.returnValue.id] = "$methodName(${args.joinToString()})"
        return null
    }

    error("Never should have come here: $statement")
}

private fun generateReturnValueStatement(
    statement: ProxyShaderBuilder.ReturnValueProgramStatement,
    context: ProgramContext
): String {
    val args = statement.value::class.members
        .filterIsInstance<KProperty<*>>()
        .filterNot { it.name in listOf("equals", "hashCode", "toString") }
        .map { it.getter.call(statement.value)!! }
        .map { it.resolveNameOrValue(context) }

    return buildString {
        newline()
        appendIndentedLine(context.callDepth, "${statement.value::class.simpleName} vertexData = {")
        args.forEachIndexed { i, arg ->
            appendIndented(context.callDepth + 1, arg)
            if (i < args.size - 1) appendLine(",")
            else appendLine()
        }
        appendIndentedLine(context.callDepth, "};")
        newline()

        appendIndented(context.callDepth, "vertex = vertexData;")
    }
}

private val String.glslOperator
    get() = when (this) {
        "mul", "transform" -> "*"
        else -> null
    }

private data class ProgramContext(
    val shader: ProxyShaderBuilder<*, *, *>,
    val vertexAttributes: Map<ObjectRef, String>,
    val variableCounters: MutableMap<KClass<*>, Int> = mutableMapOf(),
    val variables: MutableMap<ObjectRef, Pair<String, KClass<*>>> = mutableMapOf(),
    val inlineMethodCalls: MutableMap<ObjectRef, String> = mutableMapOf(),
    var callDepth: Int = 1 //we start in main
)

private fun Any.resolveNameOrValue(context: ProgramContext): String? =
    if (this::class.isGlslPrimitive) {
        toString() //FIXME joml types ain't gonna like this
    } else if (id in context.shader.uniformStatements) {
        context.shader.uniformStatements[id]!!.name!!
    } else if (id in context.vertexAttributes) {
        context.vertexAttributes[id]!!
    } else if (id in context.inlineMethodCalls) {
        context.inlineMethodCalls[id]!!
    } else if (id in context.variables) {
        check(id !in context.inlineMethodCalls) { "Method call that was inlined is being accessed as a variable" }
        context.variables[id]!!.first
    } else null

//private infix fun Any?.refEquals(other: Any?) =
//    this != null && other != null &&
//    System.identityHashCode(this) == System.identityHashCode(other)

//private data class MethodSignature

private fun StringBuilder.glslStatement(qualifier: GlslStorageQualifier, type: KClass<*>, name: String) = appendLine(
    "${qualifier.name.lowercase()} ${type.simpleName} $name;"
)

private fun StringBuilder.glslRenderTargets(renderTargets: Map<ObjectRef, KClass<*>>) = appendLine(
    renderTargets
        .map { (name, _) -> "out vec4 $name;" }
        .joinToString("\n"))

internal fun StringBuilder.appendIndented(indent: Int, value: String?) =
    append("${"\t".repeat(indent)}${value ?: "<null>"}")

internal fun StringBuilder.appendIndentedLine(indent: Int, value: String?) =
    appendIndented(indent, value).newline()

@ShaderDslMarker
open class ProxyGlslContext(
    private val shader: ProxyShaderBuilder<*, *, *>,
    private val stage: ShaderStage
) {
    fun vec4(x: float, y: float, z: float, w: float): vec4 = register({ Vector4f(x, y, z, w) }, "vec4", x, y, z, w)
    fun vec4(vec: vec3, w: float): vec4 = register({ Vector4f(vec, w) }, "vec4", vec, w)

    private inline fun <reified T : Any> register(value: () -> T, name: String, vararg args: Any): T = shader.frame {
        val proxy = shader.proxy(value(), "instance", shader.proxyLookup)
//        val method = GlslContext::class.java.getMethod(name, *args.map { it::class.java }.toTypedArray()) //this seems like a pretty shit idea
        shader.programStatements.add(
            MethodProgramStatement(
                T::class,
                proxy,
                shader.callDepth,
                stage,
                null,
                name,
                null,
                args.toList()
            )
        )
        proxy
    }

    fun <T> code(@Language("GLSL") code: String): T = TODO("log call and return wrapper proxy")
}

@ShaderDslMarker
class ProxyVertexReceiver(builder: ProxyShaderBuilder<*, *, *>) : ProxyGlslContext(builder, ShaderStage.VERTEX)

@ShaderDslMarker
class ProxyFragmentReceiver(builder: ProxyShaderBuilder<*, *, *>) : ProxyGlslContext(builder, ShaderStage.FRAGMENT)


@OptIn(ExperimentalStdlibApi::class)
internal val Any.id: ObjectRef
    get() = ObjectRef(
        "${this::class.qualifiedName}@${
            System.identityHashCode(this).toHexString()
        }"
    )


private fun <T : Any> ProxyShaderBuilder<*, *, *>.proxyObject(
    clazz: KClass<T>,
    subject: String,
    proxyLookup: ProxyLookup
): T = proxy(clazz.java.newInstance(), subject, proxyLookup)

private fun <T : Any> ProxyShaderBuilder<*, *, *>.proxy(obj: T, subject: String, proxyLookup: ProxyLookup): T {
    if (obj::class.qualifiedName!!.contains("ByteBuddy")) return obj //object is already a proxy

    val proxy = ByteBuddy()
        .subclass(obj::class.java) //ALRIGHT, so do *NOT* use reified types to get the classes for this
        .method(ElementMatchers.any())
        .intercept(InvocationHandlerAdapter.of { _, method, args ->
            if (!interceptProxyCalls) {
                return@of method.invoke(obj, *(args ?: emptyArray()))
            }

            frame {
                var returnValue = method.invoke(obj, *(args ?: emptyArray()))

                returnValue = when (returnValue) {
//                returnValue::class.isFinal -> returnValue
                    is Int -> returnValue
//                is Long -> returnValue
                    is Float -> returnValue
//                is Double -> returnValue
                    is Boolean -> returnValue
//                is Byte -> returnValue
//                is Char -> returnValue
                    is String -> returnValue
                    else -> proxy(returnValue, subject, proxyLookup)
                }

                programStatements.add(
                    MethodProgramStatement(
                        method.returnType.kotlin,
                        returnValue,
                        callDepth,
                        stage,
                        method,
                        method.name,
                        obj,
                        (args ?: emptyArray()).toList()
                    )
                )

                returnValue
            }
        })
        .make()
        .load(obj::class.java.classLoader)
        .loaded
        .newInstance() //TODO primitive arg detection & instantiation

    proxyLookup[obj.id] = proxy
    return proxy as T
}

internal inline fun <T> ProxyShaderBuilder<*, *, *>.frame(block: () -> T): T {
    callDepth++
    val value = block()
    callDepth--
    return value
}
