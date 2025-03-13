package io.github.etieskrill.injection.extension.shader.dsl

import io.github.etieskrill.injection.extension.shader.ShaderStage

internal fun generateGlsl(data: VisitorData): String = buildString {
    appendLine("#version ${data.version} ${data.profile.takeIf { it == GlslProfile.CORE }?.name?.lowercase()}")
    newline()

    appendLine(generateStruct(data.vertexDataStructType, data.vertexDataStruct))
    newline()

    data.uniforms.forEach { (name, type) ->
        appendLine(generateStatement(GlslStorageQualifier.UNIFORM, type, name))
    }
    newline()

    appendLine(generateStage(ShaderStage.VERTEX))
    newline()

    data.vertexAttributes.forEach { (name, type) ->
        appendLine(generateStatement(GlslStorageQualifier.IN, type, name))
    }
    newline()

    appendLine(generateStatement(GlslStorageQualifier.OUT, data.vertexDataStructType, "vertex"))
    newline()

    appendLine(generateMain(data.stages[ShaderStage.VERTEX] ?: emptyList()))
    newline()

    appendLine(generateStage(ShaderStage.FRAGMENT))
    newline()

    appendLine(generateStatement(GlslStorageQualifier.IN, data.vertexDataStructType, "vertex"))
    newline()

    data.renderTargets.forEach {
        appendLine(generateStatement(GlslStorageQualifier.OUT, "vec4", it))
    }
    newline()

    appendLine(generateMain(data.stages[ShaderStage.FRAGMENT] ?: emptyList()))
    newline()

    append("------------")
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
    append("}")
}

private fun generateStage(stage: ShaderStage) = "#pragma stage ${stage.name.lowercase()}"

internal enum class GlslStorageQualifier { IN, OUT, UNIFORM }

private fun generateStatement(qualifier: GlslStorageQualifier, type: GlslType, name: String) =
    generateStatement(qualifier, type.type, name)

private fun generateStatement(qualifier: GlslStorageQualifier, type: String, name: String) =
    "${qualifier.name.lowercase()} $type $name;"

private fun generateMain(statements: List<String>) = buildIndentedString {
    appendLine("void main() {")
    indent {
        statements.forEach {
            appendLine(it)
        }
    }
    append("}")
}
