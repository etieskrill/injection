package io.github.etieskrill.injection.extension.shader.reflection

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import io.github.etieskrill.injection.extension.shader.AbstractShader
import java.io.OutputStream

class ShaderReflectorProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        ShaderReflectorProcessor(environment.codeGenerator, environment.logger)
}

class ShaderReflectorProcessor(
    private val generator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val shaderClasses = resolver.getSymbolsWithAnnotation(ReflectShader::class.qualifiedName!!).toList()

        val processedClasses = shaderClasses
            .filterIsInstance<KSClassDeclaration>()
            .filter {
                if (it.hasSupertype<AbstractShader>()) true
                else throw IllegalStateException("Class ${it.simpleName.asString()} has @ReflectShader annotation, but does not implement ${AbstractShader::class.simpleName}")
            }
            .associateWith { it.generateFile() }
            .map { (classDeclaration, output) ->
                val fields = listOf(
                    classDeclaration.packageName.asString(),
                    classDeclaration.simpleName.asString(),
                    classDeclaration.fullClassName,
                    classDeclaration.annotations
                        .find { it.shortName.asString() == ReflectShader::class.simpleName!! }!!
                        .arguments
                        .find { it.name?.asString() == "files" && it.value != null }
                        .run {
                            when (val value = this!!.value) {
                                is String -> value
                                is List<*> -> value.joinToString(",") //FIXME more type-safe solution
                                else -> throw IllegalArgumentException("Unsupported shader file type: $value::class, must be one of [String, String[] (List<String> in kotlin)]")
                            }
                        }
                )

                output.write(fields.joinToString(",").toByteArray())
                classDeclaration
            }

        return shaderClasses - processedClasses
    }

    private inline fun <reified T> KSClassDeclaration.hasSupertype(): Boolean {
        var types = superTypes.map { it.resolve() }.toMutableList()

        while (types.isNotEmpty()) {
            val type = types.removeFirst().declaration
            if (type !is KSClassDeclaration) continue
            if (type.qualifiedName!!.asString() == T::class.qualifiedName!!)
                return true

            types.addAll(type.superTypes.map { it.resolve() })
        }

        return false
    }

    private fun KSClassDeclaration.generateFile(): OutputStream =
        generator.createNewFile(
            dependencies = Dependencies(false, this.containingFile!!),
            packageName = packageName.asString(),
            fileName = "$META_FILE_PREFIX$fullClassName",
            extensionName = "csv"
        )

    //also includes outer classes
    private val KSClassDeclaration.fullClassName: String?
        get() = qualifiedName?.asString()?.removePrefix("${packageName.asString()}.")

}
