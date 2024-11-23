package org.etieskrill.engine.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

public abstract class Loader<T> {

    protected final Map<String, T> map = new HashMap<>();

    private static final Logger logger = LoggerFactory.getLogger(Loader.class);

    public T load(String name, Supplier<T> supplier) {
        Objects.requireNonNull(supplier, "Supplier must not be null");
        checkIdentifier(name);

        if (map.containsKey(name)) {
            logger.trace("{} {} was already loaded", getLoaderName(), name);
            return get(name);
        }

        T t = supplier.get();
        map.put(name, t);
        logger.debug("Loaded {} as {}", getLoaderName().toLowerCase(), name);
        return t;
    }

    public T get(String name) {
        return map.get(name);
    }

    protected abstract String getLoaderName();

    protected static void checkIdentifier(String name) {
        Objects.requireNonNull(name, "Name must not be null");
        if (name.isBlank()) throw new IllegalArgumentException("Identifier must not be blank");
    }

}
