package org.etieskrill.engine.util;

import org.etieskrill.engine.Disposable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class DisposableLoader<T extends Disposable> extends Loader<T> implements Disposable {

    private static final Logger logger = LoggerFactory.getLogger(DisposableLoader.class);

    @Override
    public void dispose() {
        logger.debug("Disposing loader'{}'", getLoaderName());
        map.values().forEach(T::dispose);
    }

}
