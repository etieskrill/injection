package io.github.etieskrill.injection.extension.shader.dsl

import io.github.etieskrill.injection.extension.shader.ShaderStage
import io.github.etieskrill.injection.extension.shader.ShaderStage.*
import io.github.etieskrill.injection.extension.shader.ShadowMap
import io.github.etieskrill.injection.extension.shader.glslType
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.getCompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeProjection
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.classOrFail
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.isArray
import org.jetbrains.kotlin.ir.types.typeOrFail
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.findDeclaration
import org.jetbrains.kotlin.ir.util.getArgumentsWithIr
import org.jetbrains.kotlin.ir.util.isStrictSubtypeOfClass
import org.jetbrains.kotlin.ir.util.isSubtypeOfClass
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.util.superTypes
import org.jetbrains.kotlin.ir.visitors.IrVisitor
import org.jetbrains.kotlin.name.Name
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

@OptIn(UnsafeDuringIrConstructionAPI::class)
internal class IrShaderGenerationExtension(
    private val options: ShaderDslCompilerOptions,
    private val messageCollector: MessageCollector
) : IrGenerationExtension {
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        try {
            val (shaderClasses, files) = getShaderClasses(moduleFragment)

            for ((shader, types) in shaderClasses) {
                val data = analyseShaderClasses(shader, types, files)

                val shaderSource = generateGlsl(
                    data,
                    pluginContext,
                    messageCollector
                ) //TODO maybe a little validation??? creating a context without a window would be a pain, so some lib mayhaps?

                val shaderDir = File(options.generatedResourceDir, "shaders")
                if (!shaderDir.exists()) shaderDir.mkdirs()
                val shaderFile = File(shaderDir, "${shader.name.asString().removeSuffix("Shader")}.glsl")
                if (!shaderFile.exists()) shaderFile.createNewFile()

                shaderFile.writeText(shaderSource)
            }
        } catch (_: CompilerAbortException) {
        }
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun getShaderClasses(moduleFragment: IrModuleFragment): Pair<Map<IrClass, ShaderDataTypes>, Map<IrDeclaration, IrFile>> {
        val files = mutableMapOf<IrDeclaration, IrFile>()
        val shaderClasses = moduleFragment
            .files //TODO just passing a copy of the ir would avoid the compilation issues that often arise after my "transforms" - what would be even better is to just stop the compiler from trying to emit code for ShaderBuilder classes
            .flatMap { file -> file.declarations.onEach { files[it] = file } } //TODO find non top-level decls as well
            .filterIsInstance<IrClass>()
            .associateWith { clazz -> clazz.findSuperType<ShaderBuilder<*, *, *>>() }
            .filterValues { it != null }
            .filterKeys { it.typeParameters.isEmpty() } //just disallow type params altogether for now FIXME
            .mapValues { (clazz, shaderType) ->
                when (shaderType) {
                    is IrSimpleType -> shaderType
                        .arguments
                        .map { it.typeOrFail.classifierOrFail.resolveTypeParameter(clazz.symbol, shaderType) }
                        .run { ShaderDataTypes(get(0), get(1), get(2)) }

                    else -> error("Unsupported shader data type $shaderType")
                }
            }

        return shaderClasses to files
    }

    private inline fun <reified T : Any> IrClass.findSuperType(): IrType? {
        val alreadyVisited = mutableSetOf<IrType>()
        return superTypes.firstNotNullOfOrNull { it.findSuperType(T::class, alreadyVisited) }
    }

    private fun IrClassifierSymbol.resolveTypeParameter(clazz: IrClassSymbol, baseType: IrType): IrClass = when (this) {
        is IrClassSymbol -> owner
        is IrTypeParameterSymbol -> {
            if (baseType.isStrictSubtypeOfClass(clazz))
                error("Could not resolve type parameter ${this.owner.index}: got to base class and type parameter is still not direct class")

            val superType = clazz.owner.superTypes.single { clazz.isSubtypeOfClass(it.classOrFail) }
            val extractedType = when (superType) {
                is IrSimpleType -> {
                    if (owner.index > superType.arguments.size - 1) error("TODO proper message")
                    val type = superType.arguments[owner.index] as IrTypeProjection
                    if (type is IrSimpleType && type.classifier is IrClassSymbol) {
                        return (type.classifier as IrClassSymbol).owner
                    }
                    type
                }

                else -> error("Unexpected")
            }

            extractedType.type.classifierOrFail.resolveTypeParameter(
                superType.classifierOrFail as IrClassSymbol,
                baseType
            )
        }

        else -> error("More unexpected")
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun IrType.findSuperType(ancestor: KClass<*>, alreadyVisited: MutableSet<IrType>): IrType? = when {
        fullName == ancestor.qualifiedName -> this
        this in alreadyVisited -> null
        else -> {
            alreadyVisited.add(this)
            superTypes().mapNotNull { it as? IrSimpleType }
                .firstNotNullOfOrNull { it.findSuperType(ancestor, alreadyVisited) }
        }
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun analyseShaderClasses(
        shader: IrClass,
        types: ShaderDataTypes,
        files: Map<IrDeclaration, IrFile>,
    ): VisitorData {
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
            file = files[shader]!!,
            vertexAttributes = types.vertexAttributesType.getGlslFields(),
            vertexDataStructType = types.vertexDataType.name.asString(),
            vertexDataStructName = "vertex",
            vertexDataStruct = types.vertexDataType.getGlslFields {
                requireToCompile(
                    it,
                    { !isVar },
                    "Vertex data struct may only have val members: ${it.name.asString()} is var",
                    files[shader]!!
                )
                it.name.asString() != POSITION_FIELD_NAME
            },
            renderTargets = types.renderTargetsType.getGlslFields {
                requireToCompile(
                    it,
                    { !isVar },
                    "Render target struct may only have val members: ${it.name.asString()} is var",
                    files[shader]!!
                )
                requireToCompile(
                    it,
                    { backingField!!.type.equals<RenderTarget>() },
                    "Render target struct may only have members of type RenderTarget: ${it.name.asString()} is ${it.backingField!!.type.simpleName}",
                    files[shader]!!
                )
                true
            }.keys.toList(),
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

        return data
    }

    private fun <T : IrElement> requireToCompile(
        element: T,
        condition: T.() -> Boolean,
        message: String,
        file: IrFile
    ) {
        if (!condition(element)) messageCollector.compilerError(message, element, file)
    }
}

private class CompilerAbortException : RuntimeException()

//i realised that with CompilerMessageSeverity#EXCEPTION, this behaviour is exactly mirrored - but if a return value is
//expected, e.g. in case of a when expression branch which should always lead to an error, this is still useful
//PS. nvmd it does not log the location for some reason, and thus intellij does not automatically jump there, which is
//quite the nono for such a yesyes feature
internal fun MessageCollector.compilerError(message: String, element: IrElement, file: IrFile): Nothing {
//    error("$message\n${element.dump()}") //uncomment to debug these
    val location = element.getCompilerMessageLocation(file)!!
    report(CompilerMessageSeverity.ERROR, message, location)
    throw CompilerAbortException()
}

internal fun MessageCollector.reportWarning(message: String, element: IrElement, file: IrFile) {
    val location = element.getCompilerMessageLocation(file)!!
    report(CompilerMessageSeverity.WARNING, message, location)
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
private fun IrClass.getGlslFields(condition: (IrProperty) -> Boolean = { true }) = properties
    .filter(condition)
    .associate {
        it.name.checkNotGlslBuiltin()
        it.name.asString() to (it.backingField!!.type.glslType
            ?: error("Shader data type may only have fields of GLSL primitive type"))
    }

private fun Name.checkNotGlslBuiltin() =
    check(!asString().isGlslBuiltin() && !asString().startsWith("gl_"))
    { "Field/variable name may not equal a builtin symbol or start with gl_: ${asString()}" }

@OptIn(UnsafeDuringIrConstructionAPI::class)
private inline fun <reified T, R> IrClass.getDelegatedProperties(
    noinline mapper: (IrProperty) -> Pair<String, R>
): Map<String, R> = properties
    .filter { it.isDelegated }
    .filter { it.backingField?.type.equals<T>() }
    .onEach { it.name.checkNotGlslBuiltin() }
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
    val file: IrFile,

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
    atDepth: Int? = null,
    data: RecursiveFinderVisitorData<T> = RecursiveFinderVisitorData(),
    noinline condition: (T) -> Boolean = { true }
): T? = accept(RecursiveFinderVisitor(T::class, includeSelf.ifFalse { this }, atDepth, condition), data)

internal data class RecursiveFinderVisitorData<T : IrElement>(var element: T? = null, var depth: Int = -1)
internal class RecursiveFinderVisitor<T : IrElement>(
    val elementType: KClass<T>,
    val self: IrElement?,
    val atDepth: Int?,
    val condition: (T) -> Boolean
) : IrVisitor<T?, RecursiveFinderVisitorData<T>>() {
    override fun visitElement(element: IrElement, data: RecursiveFinderVisitorData<T>): T? {
        element.accept(object : IrVisitor<Unit, RecursiveFinderVisitorData<T>>() {
            override fun visitElement(element: IrElement, data: RecursiveFinderVisitorData<T>) {
                ++data.depth

                when {
                    data.element != null -> {}
                    elementType.isInstance(element)
                            && element != self && condition(elementType.cast(element))
                            && atDepth?.let { it == data.depth } ?: true -> {
                        data.element = elementType.cast(element)
                    }

                    else -> element.acceptChildren(this, data)
                }
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

        if (name == "double") { //TODO either check extensions or downcast - well, that's this here
            return@getOrPutNullable GlslType("float")
        }

        GlslType(name)
    }

internal val IrType.fullName: String
    get() = classFqName!!.asString()

internal val IrType.simpleName: String
    get() = fullName.split(".").last()
