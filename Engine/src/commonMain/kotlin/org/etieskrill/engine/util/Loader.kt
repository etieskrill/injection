package org.etieskrill.engine.util

import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

abstract class Loader<T> {

    protected val map: MutableMap<String, T> = mutableMapOf()

    open fun load(name: String, supplier: () -> T): T {
        check(name.isNotBlank()) { "Identifier must not be blank" }

        if (name in map) {
            logger.trace { "$loaderName $name was already loaded" }
            return map[name]!!
        }

        val t = supplier()
        map[name] = t
        logger.debug { "Loaded ${loaderName.lowercase()} $name" }
        return t
    }

    open operator fun get(name: String) = map[name]

    abstract val loaderName: String

}
