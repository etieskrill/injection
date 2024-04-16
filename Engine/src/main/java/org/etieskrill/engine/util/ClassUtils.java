package org.etieskrill.engine.util;

import org.jetbrains.annotations.Nullable;

public final class ClassUtils {

    public static String getSimpleName(@Nullable Object o) {
        return o != null ? o.getClass().getSimpleName() : "null";
    }

    private ClassUtils() {
        //Not intended for instantiation
    }

}
