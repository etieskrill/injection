package org.etieskrill.engine.common

import kotlin.annotation.AnnotationTarget.FIELD
import kotlin.annotation.AnnotationTarget.FUNCTION

/**
 * Resources held by fields or returned by methods that have this annotation are disposed automatically when the
 * application shuts down, making manually {@link Disposable#dispose() disposing} them unnecessary.
 * <p>
 * This applies only when applications either extend {@link App}, or {@link Loaders#disposeDefaultLoaders()}
 * is called during termination.
 */
@Target(FIELD, FUNCTION)
annotation class ApplicationDisposed()
