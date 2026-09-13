package io.github.etieskrill.sandbox

import java.nio.file.FileSystems
import java.nio.file.StandardWatchEventKinds.*
import kotlin.io.path.Path

fun main() {

    val watchService = FileSystems.getDefault().newWatchService()!!
    val path = Path("assets", "shaders")

    val watchKey = path.register(watchService, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE)

    println("Process starts at %.0f".format(System.currentTimeMillis() / 1000f))

    while (true) {
        val events = watchService.take()
        events.pollEvents().forEach {
            println("Kind: ${it!!.kind()}, count: ${it.count()}, context: ${it.context()}")
        }
        println("Events generated at %.0f".format(System.currentTimeMillis() / 1000f))
        watchKey.reset()
    }

}