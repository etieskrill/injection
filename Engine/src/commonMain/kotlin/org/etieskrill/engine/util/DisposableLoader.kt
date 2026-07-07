package org.etieskrill.engine.util

import io.github.oshai.kotlinlogging.KotlinLogging
import org.etieskrill.engine.common.Disposable

private val logger = KotlinLogging.logger {}

abstract class DisposableLoader<T : Disposable> : Loader<T>(), Disposable {

    override fun dispose() {
        logger.debug { "Disposing loader $loaderName" }
        map.values.forEach { it.dispose() }
    }

}
