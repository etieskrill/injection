package org.etieskrill.engine.util

/**
 * @return `true` if the resource at [path] exists, `false` if not
 */
expect fun resourceExists(path: String): Boolean

/**
 * Returns all regular files in [path] if it is a directory in resources.
 * Subdirectories are not searched. Links are resolved.
 *
 * @throws org.etieskrill.engine.common.ResourceLoadException if [path] is not an existing directory
 */
expect fun listResources(path: String): List<String>

/**
 * Reads the entire content of the resource at [path] into a [ByteArray].
 *
 * @throws org.etieskrill.engine.common.ResourceLoadException if [path] is not an existing file
 */
expect fun readResource(path: String): ByteArray
