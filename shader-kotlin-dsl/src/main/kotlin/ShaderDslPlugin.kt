package io.github.etieskrill.injection.extension.shader.dsl

import io.github.etieskrill.injection.extension.shader.ShaderStage
import io.github.etieskrill.injection.extension.shader.ShaderStage.*
import io.github.etieskrill.injection.extension.shader.ShadowMap
import io.github.etieskrill.injection.extension.shader.glslType
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrVisitor
import org.jetbrains.kotlin.utils.addToStdlib.ifFalse
import org.jetbrains.kotlin.utils.filterIsInstanceAnd
import org.jetbrains.kotlin.utils.getOrPutNullable
import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.cast

private data class ShaderDataTypes(
    val vertexAttributesType: IrClass,
    val vertexDataType: IrClass,
    val renderTargetsType: IrClass
)

internal class IrShaderGenerationExtension(
    private val options: ShaderDslCompilerOptions
) : IrGenerationExtension {
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        val shaderClasses = moduleFragment
            .files
            .map { it.declarations } //TODO find non top-level decls as well
            .flatten()
            .filterIsInstance<IrClass>()
            .associateWith { clazz -> clazz.superTypes.find { it.classFqName!!.asString() == ShaderBuilder::class.qualifiedName } } //TODO include indirect superclasses
            .filterValues { it != null }
            .mapValues { (_, shaderType) ->
                when (shaderType) {
                    is IrSimpleType -> shaderType
                        .arguments
                        .map { it.typeOrFail.classifierOrFail }
                        .map {
                            when (it) {
                                is IrClassSymbol -> it.owner
                                else -> error("Unexpected class: $it")
                            }
                        }
                        .run { ShaderDataTypes(get(0), get(1), get(2)) }

                    else -> error("Unsupported shader data type $shaderType")
                }
            }

        for ((shader, types) in shaderClasses) {
            val constDeclarations = shader.getDelegatedProperties<ConstDelegate<*>, GlslTypeInitialiser> { property ->
                val initialiser = property.backingField!!.initializer!!.expression
                property.name.asString() to (initialiser as IrCall)
                    .getArgumentsWithIr()
                    .toMap()
                    .mapKeys { it.key.name.asString() }
                    .firstNotNullOf { param -> if (param.key == "value"/*!= "<this>"*/) param else null }
                    .run {
                        val type = when {
                            value.type.isArray() -> (value.type as IrSimpleType).arguments.first().typeOrFail.glslType!!
                            else -> value.type.glslType!!
                        }

                        GlslTypeInitialiser(type, value.type.isArray(), value)
                    }
            }

            val data = VisitorData(
                programParent = shader,
                vertexAttributes = types.vertexAttributesType.getGlslFields(),
                vertexDataStructType = types.vertexDataType.name.asString(),
                vertexDataStructName = "vertex",
                vertexDataStruct = types.vertexDataType.getGlslFields {
                    require(!it.isVar) { "Vertex data struct may only have val members: ${it.name.asString()} is var" }
                    it.name.asString() != POSITION_FIELD_NAME
                },
                renderTargets = types.renderTargetsType.getGlslFields().keys.toList(),
                constants = constDeclarations,
                uniforms = shader.getDelegatedProperties<UniformDelegate<*>, GlslType> { property ->
                    property.name.asString() to property.backingField?.type?.let { type ->
                        when (type) {
                            is IrSimpleType -> requireNotNull(type.arguments[0].typeOrFail.glslType)
                            { "Shader uniforms may only be of GLSL primitive type: ${type.arguments[0].typeOrFail.simpleName} is not" } //TODO i just assume the delegate's type is always first
                            else -> error("Unexpected type: $this")
                        }
                    }!!
                },
                definedFunctions = shader.declarations
                    .filterIsInstanceAnd<IrFunctionImpl> { it.name.asString() != "program" } //impl because exact type is needed
                    .onEach { check(it.typeParameters.isEmpty()) { "Shader functions may not have type parameters, but this one does:\n${it.render()}" } }
                    .associate { function ->
                        val funcWrapper = function.body!!.findElement<IrCall>() ?: error(
                            "Could not find function wrapper for ${
                                function.name.asString()
                            }. Add one like 'fun someFunction() = func[Vert,Frag] { ... }'"
                        )
                        val funcType = when (funcWrapper.symbol.owner.name.asString()) {
                            "func" -> NONE
                            "vertFunc" -> VERTEX
                            "fragFunc" -> FRAGMENT
                            else -> error(
                                "Could not infer wrapper function type for '${
                                    funcWrapper.symbol.owner.name
                                }', must be one of [func, funcVert, funcFrag]"
                            )
                        }

                        function.name.asString() to (funcType to function)
                    }
            )

            data.structTypes += types.vertexDataType.defaultType

            val programBodies = shader
                .findDeclaration<IrFunction> { it.name.asString() == "program" }!!

            val vertexProgram = programBodies
                .findElement<IrCall> { it.symbol.owner.name.asString() == "vertex" }
                .let { it ?: error("Shader program must have a vertex stage") }
                .findElement<IrBlockBody>()!!

            data.stageBodies[VERTEX] = vertexProgram

            val fragmentProgram = programBodies
                .findElement<IrCall> { it.symbol.owner.name.asString() == "fragment" }
                .let { it ?: error("Shader program must have a fragment stage") }
                .findElement<IrBlockBody>()!!

            data.stageBodies[FRAGMENT] = fragmentProgram

            val shaderDir = File(options.generatedResourceDir, "shaders")
            if (!shaderDir.exists()) shaderDir.mkdirs()
            val shaderFile = File(shaderDir, "${shader.name.asString().removeSuffix("Shader")}.glsl")
            if (!shaderFile.exists()) shaderFile.createNewFile()

            val shaderSource = generateGlsl(
                data,
                pluginContext
            ) //TODO maybe a little validation??? creating a context without a window would be a pain, so some lib mayhaps?

            shaderFile.writeText(shaderSource)
        }
    }
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
private fun IrClass.getGlslFields(condition: (IrProperty) -> Boolean = { true }) = properties
    .filter(condition)
    .associate {
        it.name.asString() to (it.backingField!!.type.glslType
            ?: error("Shader data type may only have fields of GLSL primitive type"))
    }

@OptIn(UnsafeDuringIrConstructionAPI::class)
private inline fun <reified T, R> IrClass.getDelegatedProperties(
    noinline mapper: (IrProperty) -> Pair<String, R>
): Map<String, R> = properties
    .filter { it.isDelegated }
    .filter { it.backingField?.type.equals<T>() }
    .associate(mapper)

internal enum class GlslVersion { `330` }
internal enum class GlslProfile { CORE, COMPATIBILITY }

@JvmInline
internal value class GlslType internal constructor(val type: String) {
    override fun toString(): String = type
}

internal data class GlslTypeInitialiser internal constructor(
    val type: GlslType,
    val array: Boolean,
    val initialiser: IrExpression
) {
    override fun toString(): String = type.type
}

internal data class VisitorData(
    val programParent: IrDeclarationParent,

    val version: GlslVersion = GlslVersion.`330`,
    val profile: GlslProfile = GlslProfile.CORE,

    val vertexAttributes: Map<String, GlslType>,

    val vertexDataStructType: String,
    val vertexDataStructName: String,
    val vertexDataStruct: Map<String, GlslType>,

    val renderTargets: List<String>,

    val constants: Map<String, GlslTypeInitialiser>,

    val uniforms: Map<String, GlslType>,

    val definedFunctions: Map<String, Pair<ShaderStage, IrFunction>>,

    val structTypes: MutableList<IrType> = mutableListOf(),

    val stageBodies: MutableMap<ShaderStage, IrBlockBody> = mutableMapOf(),
)

internal inline fun <reified T : IrElement> IrElement.findElement(
    includeSelf: Boolean = false,
    data: RecursiveFinderVisitorData<T> = RecursiveFinderVisitorData(),
    noinline condition: (T) -> Boolean = { true }
): T? = accept(RecursiveFinderVisitor(T::class, includeSelf.ifFalse { this }, condition), data)

internal data class RecursiveFinderVisitorData<T : IrElement>(var element: T? = null)
internal class RecursiveFinderVisitor<T : IrElement>(
    val elementType: KClass<T>,
    val self: IrElement?,
    val condition: (T) -> Boolean
) :
    IrVisitor<T?, RecursiveFinderVisitorData<T>>() {
    override fun visitElement(element: IrElement, data: RecursiveFinderVisitorData<T>): T? {
        element.accept(object : IrVisitor<Unit, RecursiveFinderVisitorData<T>>() {
            override fun visitElement(element: IrElement, data: RecursiveFinderVisitorData<T>) = when {
                data.element != null -> {}
                elementType.isInstance(element) && element != self && condition(elementType.cast(element)) ->
                    data.element = elementType.cast(element)

                else -> element.acceptChildren(this, data)
            }
        }, data)

        return data.element
    }
}

private val glslTypes = mutableMapOf<String, GlslType>()

internal val IrType.glslType: GlslType?
    get() = glslTypes.getOrPutNullable(fullName) {
        if (fullName == RenderTarget::class.qualifiedName) return GlslType("vec4")
        if (fullName == ShadowMap::class.qualifiedName) TODO("shadow types")
        val name = this.fullName.glslType ?: return null
        GlslType(name)
    }

internal val IrType.fullName: String
    get() = classFqName!!.asString()

internal val IrType.simpleName: String
    get() = fullName.split(".").last()
