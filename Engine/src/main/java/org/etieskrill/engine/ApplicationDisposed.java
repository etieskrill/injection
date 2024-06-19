package org.etieskrill.engine;

import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;

/**
 * Resources held by fields or returned by methods that have this annotation are disposed when the application shuts
 * <p>
 * This applies only when applications extend {@link org.etieskrill.engine.application.GameApplication GameApplication}.
 */
@Target({FIELD, METHOD})
public @interface ApplicationDisposed {
}
