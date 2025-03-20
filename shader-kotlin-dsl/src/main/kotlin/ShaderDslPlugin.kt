package io.github.etieskrill.injection.extension.shader.dsl

import io.github.etieskrill.injection.extension.shader.ShaderStage
import io.github.etieskrill.injection.extension.shader.ShaderStage.FRAGMENT
import io.github.etieskrill.injection.extension.shader.ShaderStage.VERTEX
import io.github.etieskrill.injection.extension.shader.ShadowMap
import io.github.etieskrill.injection.extension.shader.glslType
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.findDeclaration
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.ir.visitors.IrVisitor
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
            val data = VisitorData(
                vertexAttributes = types.vertexAttributesType.getGlslFields(),
                vertexDataStructType = types.vertexDataType.name.asString(),
                vertexDataStructName = "vertex",
                vertexDataStruct = types.vertexDataType.getGlslFields { it.name.asString() != POSITION_FIELD_NAME },
                renderTargets = types.renderTargetsType.getGlslFields().keys.toList(),
                uniforms = shader.properties
                    .filter { it.isDelegated }
                    .filter { it.backingField?.type?.fullName == UniformDelegate::class.qualifiedName!! }
                    .associate {
                        it.name.asString() to it.backingField?.type?.let {
                            when (it) {
                                is IrSimpleType -> it.arguments[0].typeOrFail.glslType
                                    ?: error("Shader uniforms may only be of GLSL primitive type") //TODO i just assume the delegate's type is always first
                                else -> error("Unexpected type: $this")
                            }
                        }!!
                    }
            )

            val programBodies = shader
                .findDeclaration<IrFunction> { it.name.asString() == "program" }!!

            val vertexProgram = programBodies
                .findElement<IrCall> { it.symbol.owner.name.asString() == "vertex" }
                .let { it ?: error("Shader program must have a vertex stage") }
                .findElement<IrBlockBody>()!!

            data.stages[VERTEX] = vertexProgram

            val fragmentProgram = programBodies
                .findElement<IrCall> { it.symbol.owner.name.asString() == "fragment" }
                .let { it ?: error("Shader program must have a fragment stage") }
                .findElement<IrBlockBody>()!!

            data.stages[FRAGMENT] = fragmentProgram

            val shaderDir = File(options.generatedResourceDir, "shaders")
            if (!shaderDir.exists()) shaderDir.mkdirs()
            val shaderFile = File(shaderDir, "${shader.name.asString().removeSuffix("Shader")}.glsl")
            if (!shaderFile.exists()) shaderFile.createNewFile()

            val shaderSource =
                generateGlsl(data) //TODO maybe a little validation??? creating a context without a window would be a pain, so some lib mayhaps?

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

internal enum class GlslVersion { `330` }
internal enum class GlslProfile { CORE, COMPATIBILITY }

@JvmInline
internal value class GlslType internal constructor(val type: String) {
    override fun toString(): String = type
}

internal data class VisitorData(
    var depth: Int = 0,
    var stage: ShaderStage = ShaderStage.NONE,

    val version: GlslVersion = GlslVersion.`330`,
    val profile: GlslProfile = GlslProfile.CORE,

    val vertexAttributes: Map<String, GlslType>,

    val vertexDataStructType: String,
    val vertexDataStructName: String,
    val vertexDataStruct: Map<String, GlslType>,

    val renderTargets: List<String>,

    val uniforms: Map<String, GlslType>,

    val stages: MutableMap<ShaderStage, IrElement> = mutableMapOf(),
)

private inline fun <reified T : IrElement> IrElement.findElement(
    data: RecursiveFinderVisitorData<T> = RecursiveFinderVisitorData(),
    noinline condition: (T) -> Boolean = { true }
): T? = accept(RecursiveFinderVisitor(T::class, condition), data)

private data class RecursiveFinderVisitorData<T : IrElement>(var element: T? = null)
private class RecursiveFinderVisitor<T : IrElement>(val elementType: KClass<T>, val condition: (T) -> Boolean) :
    IrVisitor<T?, RecursiveFinderVisitorData<T>>() {
    override fun visitElement(element: IrElement, data: RecursiveFinderVisitorData<T>): T? {
        element.accept(object : IrVisitor<Unit, RecursiveFinderVisitorData<T>>() {
            override fun visitElement(element: IrElement, data: RecursiveFinderVisitorData<T>) = when {
                data.element != null -> {}
                elementType.isInstance(element) && condition(elementType.cast(element)) ->
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
