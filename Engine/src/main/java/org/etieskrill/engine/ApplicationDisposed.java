package org.etieskrill.engine;

import org.etieskrill.engine.application.GameApplication;
import org.etieskrill.engine.util.Loaders;

import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;

/**
 * Resources held by fields or returned by methods that have this annotation are disposed automatically when the
 * application shuts down, making manually {@link Disposable#dispose() disposing} them unnecessary.
 * <p>
 * This applies only when applications either extend {@link GameApplication}, or {@link Loaders#disposeDefaultLoaders()}
 * is called during termination.
 */
@Target({FIELD, METHOD})
public @interface ApplicationDisposed {
}
