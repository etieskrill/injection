package org.etieskrill.engine.util;

import org.jetbrains.annotations.Nullable;

import static java.util.Objects.requireNonNullElse;

public final class ClassUtils {

    public static String getSimpleName(@Nullable Object o) {
        return o != null ? o.getClass().getSimpleName() : "null";
    }

    public static String getFullName(@Nullable Object o) {
        return o == null ? "null" :
                requireNonNullElse(o.getClass().getCanonicalName(), "null")
                        .replace(o.getClass().getPackageName() + ".", "");
    }

    private ClassUtils() {
        //Not intended for instantiation
    }

}
