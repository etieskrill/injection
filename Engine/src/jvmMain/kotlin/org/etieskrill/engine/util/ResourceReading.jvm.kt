package org.etieskrill.engine.util

import io.github.oshai.kotlinlogging.KotlinLogging
import org.etieskrill.engine.common.ResourceLoadException
import org.etieskrill.engine.config.ENGINE_RESOURCE_PATH
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import java.nio.file.FileSystems
import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.io.path.pathString

private val logger = KotlinLogging.logger {}

actual fun resourceExists(path: String): Boolean =
    ClassLoader.getSystemResource(path) != null
            || ClassLoader.getSystemResourceAsStream(ENGINE_RESOURCE_PATH + path) != null

actual fun resolveResource(path: String): String? =
    ClassLoader.getSystemResource(path)?.let { path }
        ?: ClassLoader.getSystemResource(ENGINE_RESOURCE_PATH + path)?.let { ENGINE_RESOURCE_PATH + path }

actual fun listResources(path: String): List<String> {
    val parentResource = resolveResource(path) ?: throw ResourceLoadException("'$path' does not exist")

    val realPath: URI
    try {
        realPath = ClassLoader.getSystemResource(parentResource).toURI()
    } catch (e: URISyntaxException) {
        throw ResourceLoadException(e)
    }

    //kinda hacky, dunno how reliable this is
    when (realPath.scheme) {
        "file" -> { //unarchived dev build
            return listFilePaths(realPath.path)
                .map {
                    it.substring(
                        realPath.path.length
                                - path.length
                                - 1 //slash inserted by path
                    )
                }
        }

        "jar" -> { //jar-packaged jvm build
            try {
                FileSystems.newFileSystem(realPath, mutableMapOf<String, Any>()).use {
                    return listFilePaths(it.getPath(parentResource).pathString)
                }
            } catch (e: IOException) {
                throw ResourceLoadException("Failed to open jar file system", e)
            }
        }

        else -> throw ResourceLoadException("Unsupported resource scheme: ${realPath.scheme}")
    }
}

private fun listFilePaths(path: String): List<String> {
    try {
        Files.walk(Path(path)).use { files ->
            return files
                .filter { Files.isRegularFile(it) }
                .map { it.toString() }
                .toList()
        }
    } catch (e: IOException) {
        throw ResourceLoadException(e)
    }
}

actual fun readResource(path: String): ByteArray {
    try {
        FileInputStream(path).use {
            logger.trace { "Loading $path from external file" }
            return it.readBytes()
        }
    } catch (_: FileNotFoundException) {
    }

    ClassLoader.getSystemResourceAsStream(path)?.use { stream ->
        logger.trace { "Loading $path from application classpath" }
        return stream.readBytes()
    }

    ClassLoader.getSystemResourceAsStream(ENGINE_RESOURCE_PATH + path)?.use { stream ->
        logger.trace { "Loading $path from engine classpath" }
        return stream.readBytes()
    }

    throw ResourceLoadException("Could not read resource: $path")
}
