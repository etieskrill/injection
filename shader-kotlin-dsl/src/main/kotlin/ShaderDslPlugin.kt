package io.github.etieskrill.injection.extension.shader.dsl

import io.github.etieskrill.injection.extension.shader.*
import io.github.etieskrill.injection.extension.shader.ShaderStage.FRAGMENT
import io.github.etieskrill.injection.extension.shader.ShaderStage.VERTEX
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption
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
import org.joml.*
import kotlin.reflect.KClass
import kotlin.reflect.cast

@Suppress("unused")
class ShaderDslPlugin : KotlinCompilerPluginSupportPlugin {

    companion object {
        const val SERIALIZATION_GROUP_NAME = "io.github.etieskrill.injection.extension.shader.dsl"
        const val ARTIFACT_ID = "shader-kotlin-dsl"
        const val VERSION = "1.0.0-SNAPSHOT"
    }

    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
        return kotlinCompilation.target.project.provider {
            listOf() //TODO look into the SubPlugin thingamajig
        }
    }

    override fun apply(target: Project): Unit = target.run {
        dependencies.apply {
            add("implementation", "io.github.etieskrill.injection.extension.shader.dsl:shader-kotlin-dsl")
        }
    }

    override fun getCompilerPluginId(): String = "shaderKotlinDslPlugin"

    override fun getPluginArtifact(): SubpluginArtifact =
        SubpluginArtifact(SERIALIZATION_GROUP_NAME, ARTIFACT_ID, VERSION)

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = true
}

@OptIn(ExperimentalCompilerApi::class, ExperimentalCompilerApi::class)
class IrShaderGenerationRegistrar : CompilerPluginRegistrar() {
    override val supportsK2: Boolean = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        IrGenerationExtension.registerExtension(
            IrShaderGenerationExtension(configuration.messageCollector)
        )
    }
}

data class ShaderDataTypes(
    val vertexAttributesType: IrClass,
    val vertexDataType: IrClass,
    val renderTargetsType: IrClass
)

class IrShaderGenerationExtension(
    private val messageCollector: MessageCollector
) : IrGenerationExtension {
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        val shaderClasses = moduleFragment
            .files
            .map { it.declarations } //TODO find non top-level decls as well
            .flatten()
            .filterIsInstance<IrClass>()
            .associateWith { clazz -> clazz.superTypes.find { it.classFqName!!.asString() == ShaderBuilder::class.qualifiedName } }
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
                vertexDataStruct = types.vertexDataType.getGlslFields(),
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
                .findElement<IrCall> { it.symbol.owner.name.asString() == "vertex" }!!
                .findElement<IrBlockBody>()!!

            data.stages[VERTEX] = vertexProgram

            val fragmentProgram = programBodies
                .findElement<IrCall> { it.symbol.owner.name.asString() == "fragment" }!!
                .findElement<IrBlockBody>()!!

            data.stages[FRAGMENT] = fragmentProgram

//            generateGlsl(data).log() //TODO maybe a little validation??? creating a context without a window would be a pain, so some lib mayhaps?
        }
    }

    fun Any?.log() = messageCollector.report(CompilerMessageSeverity.ERROR, "$this")
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
private fun IrClass.getGlslFields(condition: (IrProperty) -> Boolean = { true }) = properties
    .filter(condition)
    .associate {
        it.name.asString() to (it.backingField!!.type.glslType
            ?: error("Shader data type may only have fields of GLSL primitive type"))
    }

enum class GlslVersion { `330` }
enum class GlslProfile { CORE, COMPATIBILITY }

@JvmInline
value class GlslType internal constructor(val type: String) {
    override fun toString(): String = type
}

data class VisitorData(
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

inline fun <reified T : IrElement> IrElement.findElement(
    data: RecursiveFinderVisitorData<T> = RecursiveFinderVisitorData<T>(),
    noinline condition: (T) -> Boolean = { true }
): T? = accept(RecursiveFinderVisitor(T::class, condition), data)

data class RecursiveFinderVisitorData<T : IrElement>(var element: T? = null)
class RecursiveFinderVisitor<T : IrElement>(val elementType: KClass<T>, val condition: (T) -> Boolean) :
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

val IrType.isGlslPrimitive: Boolean
    get() = when {
        classFqName!!.asString() in listOf(
            Int::class, Float::class, Boolean::class,
            Vector2f::class, Vector3f::class, Vector4f::class,
            Matrix3f::class, Matrix4f::class
        ).map { it.qualifiedName!! } -> true

        else -> false
    }

val glslTypes = mapOf(
    Int::class to GlslType("int"),
    Float::class to GlslType("float"),
    Boolean::class to GlslType("bool"),
    Vector2f::class to GlslType("vec2"),
    Vector3f::class to GlslType("vec3"),
    Vector4f::class to GlslType("vec4"),
    Matrix3f::class to GlslType("mat3"),
    Matrix4f::class to GlslType("mat4"),
    Texture2D::class to GlslType("sampler2D"),
    Texture2DArray::class to GlslType("sampler2DArray"),
    TextureCubeMap::class to GlslType("samplerCube"),
    TextureCubeMapArray::class to GlslType("samplerCubeArray"),
).mapKeys { (clazz, _) -> clazz.qualifiedName!! }

val IrType.glslType: GlslType?
    get() = when (fullName) {
        RenderTarget::class.qualifiedName!! -> GlslType("vec4")
        in glslTypes -> glslTypes[fullName]
        //TODO ShadowMap types
        else -> null
    }

val IrType.fullName: String
    get() = classFqName!!.asString()
