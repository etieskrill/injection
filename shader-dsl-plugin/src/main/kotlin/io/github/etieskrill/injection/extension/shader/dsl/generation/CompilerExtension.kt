package io.github.etieskrill.injection.extension.shader.dsl.generation

import io.github.etieskrill.injection.extension.shader.ShaderStage
import io.github.etieskrill.injection.extension.shader.ShaderStage.*
import io.github.etieskrill.injection.extension.shader.dsl.ConstDelegate
import io.github.etieskrill.injection.extension.shader.dsl.RenderTarget
import io.github.etieskrill.injection.extension.shader.dsl.ShaderBuilder
import io.github.etieskrill.injection.extension.shader.dsl.UniformArrayDelegate
import io.github.etieskrill.injection.extension.shader.dsl.UniformDelegate
import io.github.etieskrill.injection.extension.shader.dsl.gradle.ShaderDslCompilerOptions
import io.github.etieskrill.injection.extension.shader.dsl.std.ConstEval
import io.github.etieskrill.injection.extension.shader.dsl.std.Template
import io.github.etieskrill.injection.extension.shader.dsl.std.stdMethods
import io.github.etieskrill.injection.extension.shader.glslType
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.getCompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.fir.lazy.Fir2IrLazySimpleFunction
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
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
import org.jetbrains.kotlin.ir.util.getNameWithAssert
import org.jetbrains.kotlin.ir.util.isStrictSubtypeOfClass
import org.jetbrains.kotlin.ir.util.isSubtypeOfClass
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.util.superTypes
import org.jetbrains.kotlin.ir.visitors.IrVisitor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.ifFalse
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue
import org.jetbrains.kotlin.utils.filterIsInstanceAnd
import org.jetbrains.kotlin.utils.getOrPutNullable
import java.io.File
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.cast
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.valueParameters

private data class ShaderDataTypes(
    val vertexAttributesType: IrClass,
    val vertexDataType: IrClass,
    val renderTargetsType: IrClass
)

//TODO could be an AnalysisHandlerExtension, perchance
@OptIn(UnsafeDuringIrConstructionAPI::class)
internal class IrShaderGenerationExtension(
    private val options: ShaderDslCompilerOptions,
    private val messageCollector: MessageCollector
) : IrGenerationExtension {
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
                        .run { ShaderDataTypes(get(0), get(1), get(2)) } //FIXME not found for java classes?

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
        val file = files[shader]!!

        val constDeclarations = shader.getDelegatedProperties<ConstDelegate<*>, GlslTypeInitialiser>(
            messageCollector,
            file
        ) { property ->
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

        fun IrProperty.getUniformNameAndType() =
            name.asString() to backingField?.type?.let { type ->
                when (type) {
                    is IrSimpleType -> requireNotNull(type.arguments[0].typeOrFail.glslType) {
                        "Shader uniforms may only be of GLSL primitive type: ${
                            type.arguments[0].typeOrFail.simpleName
                        } is not ${
                            type.arguments[0].typeOrFail
                                .isArray()
                                .ifTrue { "(use uniformArray<...>() for arrays instead)" }
                                .orEmpty()
                        }"
                    } //TODO i just assume the delegate's type is always first
                    else -> error("Unexpected type: $this")
                }
            }!!

        val uniforms = shader.getDelegatedProperties<UniformDelegate<*>, GlslType>(
            messageCollector, file, IrProperty::getUniformNameAndType
        )

        val arrayUniforms = shader.getDelegatedProperties<UniformArrayDelegate<*>, Pair<GlslType, Int>>(
            messageCollector, file
        ) {
            val (name, type) = it.getUniformNameAndType()
            val size = when (val sizeArgument = (it.backingField!!.initializer!!.expression as IrCall)
                .getValueArgument(0)!!) {
                is IrConst -> sizeArgument.value.toString()
                else -> messageCollector.compilerError(
                    "Uniform array size must be compile constant for now", sizeArgument, file
                )
            }.toInt()

            name to (type to size)
        }

        val data = VisitorData(
            programParent = shader,
            file = file,
            vertexAttributes = types.vertexAttributesType.getGlslFields()
                .onEach {
                    if (it.key in uniforms.keys) {
                        messageCollector.compilerError(
                            "Vertex attribute name '${it.key}' clashes with uniform of same name, rename for now",
                            if (types.vertexAttributesType.startOffset != UNDEFINED_OFFSET)
                                types.vertexAttributesType
                            else shader,
                            file
                        )
                    }
                    if (it.key in arrayUniforms.keys) {
                        messageCollector.compilerError(
                            "Vertex attribute name '${it.key}' clashes with array uniform of same name, rename for now",
                            if (types.vertexAttributesType.startOffset != UNDEFINED_OFFSET)
                                types.vertexAttributesType
                            else shader,
                            file
                        )
                    }
                }, //TODO attribute aliases (a_<attributeName> or similar)
            vertexDataStructType = types.vertexDataType.name.asString(),
            vertexDataStructName = "vertex",
            vertexDataStruct = types.vertexDataType.getGlslFields {
                requireToCompile(
                    it,
                    { !isVar },
                    "Vertex data struct may only have val members: ${it.name.asString()} is var",
                    file
                )
                it.name.asString() != POSITION_FIELD_NAME
            }.toMutableMap(),
            renderTargets = types.renderTargetsType.properties.onEach {
                requireToCompile(
                    it,
                    { !isVar },
                    "Render target struct may only have val members: ${it.name.asString()} is var",
                    file
                )
                requireToCompile(
                    it,
                    { backingField!!.type.equals<RenderTarget>() },
                    "Render target struct may only have members of type RenderTarget: ${it.name.asString()} is ${it.backingField!!.type.simpleName}",
                    file
                )
            }.map {
                val name = it.name.asString()

                val finalName = if (name in uniforms || name in arrayUniforms) {
                    val altName = "${name}RenderTarget"
                    if (altName in uniforms || altName in arrayUniforms)
                        messageCollector.compilerError(
                            "Why is there a uniform named $name AND one named $altName? " +
                                    "(render target alias to evade $name collides with $altName)", it, file
                        )
                    altName
                } else {
                    name
                }

                name to finalName
            }.toMap(),
            constants = constDeclarations,
            uniforms = uniforms,
            arrayUniforms = arrayUniforms,
            definedFunctions = shader.declarations
                .filterIsInstanceAnd<IrFunctionImpl> { it.name.asString() != "program" } //impl because exact type is needed
                .onEach { check(it.typeParameters.isEmpty()) { "Shader functions may not have type parameters, but this one does:\n${it.render()}" } }
                .groupBy { it.name.asString() }
                .flatMap { (functionName, overloads) ->
                    overloads.mapIndexed { i, function ->
                        val funcWrapper = function.body!!.findElement<IrCall>() ?: messageCollector.compilerError(
                            "Could not find function wrapper for ${
                                function.name.asString()
                            }(${
                                function.valueParameters.joinToString { it.type.simpleName }
                            }). Add one like 'fun someFunction() = func[Vert,Frag] { ... }'",
                            function, files[shader]!!
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

                        val name = if (i == 0) functionName
                        else "$functionName<overload${i - 1}>"

                        name to (funcType to function)
                    }
                }.toMap(),
            evaluatedFunctions = stdMethods //TODO configurable to allow exclusion of stdlib and for custom extensions
                .filter { it.hasAnnotation<ConstEval>() }
                .groupBy { it.name },
            templateFunctions = stdMethods
                .filter { it.hasAnnotation<Template>() }
                .groupBy { it.name }
                .mapValues { (_, functions) ->
                    functions.associate {
                        val parameters = it.valueParameters.map { (it.type.classifier as KClass<*>).qualifiedName!! }

                        val template = it.findAnnotation<Template>()!!.template

                        val parameterNames = it.valueParameters.map { it.name!! }
                        var checkTemplate = template
                        for (name in parameterNames) {
                            checkTemplate = checkTemplate.replace("\$$name", "<replaced>")
                        }
                        check(!checkTemplate.contains('$'))
                        {
                            "Shader function template must not contain any variables that are not value-parameters of " +
                                    "the function:\n'$checkTemplate'"
                        }

                        parameters to template
                    }
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

        postProcess(data)

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

private fun postProcess(data: VisitorData) {
    data.definedFunctions.values.forEach { (_, function) -> unwrapInlineConditions(function.body!!.findElement<IrBlockBody>()!!) }
    data.stageBodies.values.forEach { unwrapInlineConditions(it) }
    data.programParent.patchDeclarationParents()
}

private fun unwrapInlineConditions(body: IrBlockBody) {
    val transformer = InlineConditionalTransformer()
    var transformerData: InlineConditionalTransformerData
    var transformerIteration = 0
    do {
        check(transformerIteration++ < 1000) { "Inline condition transformer ran for more than a thousand iterations: something is probably bricked" }
        transformerData = InlineConditionalTransformerData()
        body.transform(transformer, transformerData)
    } while (transformerData.inlineCondition != null)
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
    } + declarations //TODO condition for this
    .filter { (it is Fir2IrLazySimpleFunction || it is IrFunction) && it.name.asString().startsWith("get") }
    .associate {
        val type = when (it) {
            is Fir2IrLazySimpleFunction -> it.returnType
            is IrFunction -> it.returnType
            else -> error("Could not parse Java object property getter function")
        }.glslType ?: error("Field is not GLSL primitive: ${it.returnType.fullName}")

        val name = it
            .getNameWithAssert().asString()
            .removePrefix("get")
            .replaceFirstChar { it.lowercase() }

        name to type
    }

private fun Name.checkNotGlslBuiltin() =
    check(!asString().isGlslBuiltin() && !asString().startsWith("gl_"))
    { "Field/variable name may not equal a builtin symbol or start with gl_: ${asString()}" }

@OptIn(UnsafeDuringIrConstructionAPI::class)
private inline fun <reified T, R> IrClass.getDelegatedProperties(
    messageCollector: MessageCollector,
    file: IrFile,
    noinline mapper: (IrProperty) -> Pair<String, R>
): Map<String, R> = properties
    .filter { it.isDelegated }
    .filter { it.backingField?.type.equals<T>() }
    .onEach {
        try {
            it.name.checkNotGlslBuiltin()
        } catch (e: IllegalStateException) {
            messageCollector.compilerError(e.message ?: "<No message>", it, file)
        }
    }
    .associate(mapper)

internal enum class GlslVersion { `330` }
internal enum class GlslProfile { CORE, COMPATIBILITY }

@JvmInline
internal value class GlslType internal constructor(val type: String) {
    override fun toString(): String = type
}

@ConsistentCopyVisibility
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
    val vertexDataStruct: MutableMap<String, GlslType>,

    val renderTargets: Map<String, String>, //may be alias on collision as class is unwrapped

    val constants: Map<String, GlslTypeInitialiser>,

    val uniforms: Map<String, GlslType>,
    val arrayUniforms: Map<String, Pair<GlslType, Int>>,

    val definedFunctions: Map<String, Pair<ShaderStage, IrFunction>>,

    val evaluatedFunctions: Map<String, List<KCallable<*>>>,
    val templateFunctions: Map<String, Map<List<String>, String>>,

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
        if (equals<RenderTarget>()) return GlslType("vec4")
        if (equals<Number>()) return GlslType("float")
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
