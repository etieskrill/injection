package org.etieskrill.engine.util;

import org.etieskrill.engine.Disposable;

public abstract class DisposableLoader<T extends Disposable> extends Loader<T> implements Disposable {

    @Override
    public void dispose() {
        map.values().forEach(T::dispose);
    }

}
