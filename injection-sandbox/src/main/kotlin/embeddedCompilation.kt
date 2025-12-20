import kotlinx.coroutines.runBlocking
import org.joml.Vector2f
import kotlin.script.experimental.api.CompiledScript
import kotlin.script.experimental.api.EvaluationResult
import kotlin.script.experimental.api.ResultValue
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.ScriptEvaluationConfiguration
import kotlin.script.experimental.api.providedProperties
import kotlin.script.experimental.api.valueOr
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.baseClassLoader
import kotlin.script.experimental.jvm.dependenciesFromCurrentContext
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost
import kotlin.time.measureTime

object KotlinScriptCompilationConfiguration : ScriptCompilationConfiguration({
    jvm {
        dependenciesFromCurrentContext(wholeClasspath = true, unpackJarCollections = true)
    }
    providedProperties(
        "position" to Vector2f::class,
        "targetPosition" to Vector2f::class
    )
})

//object KotlinScriptEvaluationConfiguration : ScriptEvaluationConfiguration({
//    jvm {
//        baseClassLoader(ClassLoader.getSystemClassLoader())
//    }
//})
//
//@KotlinScript(
//    compilationConfiguration = KotlinScriptCompilationConfiguration::class,
//    evaluationConfiguration = KotlinScriptEvaluationConfiguration::class
//)
//abstract class KotlinScriptTemplate

fun main() {
    val host = BasicJvmScriptingHost()

    val script = """
        import org.joml.minus

        (targetPosition - position).normalize()
    """.trimIndent().toScriptSource()

    val compiledScript: CompiledScript
    val compileTime = runBlocking {
        measureTime {
            compiledScript = host.compiler(script, KotlinScriptCompilationConfiguration).valueOr { failure ->
                val reports = failure.reports.joinToString("\n") { "[${it.severity}] ${it.message}" }
                error("Script compilation failed:\n${reports}")
            }
        }
    }
    println("Script compilation took ${compileTime.inWholeMilliseconds}ms")

    //the evaluator apparently loads the script class anew each time it is run, pinning the ClassLoader did not help,
    //and some other options i tried didn't either. maybe using a proper script template does something

    val evalConfig = ScriptEvaluationConfiguration {
        jvm {
            baseClassLoader(ClassLoader.getSystemClassLoader()!!)
        }
        providedProperties(
            "position" to Vector2f(-1f, -3f),
            "targetPosition" to Vector2f(1f, 2f)
        )
    }

    val firstTime = measureTime {
        runBlocking {
            host.evaluator(compiledScript, evalConfig).valueOr { failure ->
                val reports = failure.reports.joinToString("\n") { "[${it.severity}] ${it.message}" }
                error("Script evaluation failed:\n${reports}")
            }
        }
    }
    println("First script evaluation took ${firstTime.inWholeMilliseconds}ms")

    val result: EvaluationResult
    val evalTime = runBlocking {
        measureTime {
            result = host.evaluator(compiledScript, evalConfig).valueOr { failure ->
                val reports = failure.reports.joinToString("\n") { "[${it.severity}] ${it.message}" }
                error("Script evaluation failed:\n${reports}")
            }
        }
    }
    println("Script evaluation took ${evalTime.inWholeMilliseconds}ms")

    when (val value = result.returnValue) {
        is ResultValue.Value -> println("Result value: ${value.value}")
        is ResultValue.Unit -> println("Result value: $value")
        is ResultValue.Error -> println("Evaluation failure: ${value.error.stackTraceToString()}")
        else -> error("Unexpected result type: $value")
    }

    val repeatedTime = measureTime {
        repeat(100) {
            runBlocking {
                host.evaluator(compiledScript, evalConfig).valueOr { failure ->
                    val reports = failure.reports.joinToString("\n") { "[${it.severity}] ${it.message}" }
                    error("Script evaluation failed:\n${reports}")
                }
            }
        }
    }
    println("Repeated execution took ${repeatedTime.inWholeMilliseconds}ms (${repeatedTime.inWholeMilliseconds / 100}ms per script on average)")
}
