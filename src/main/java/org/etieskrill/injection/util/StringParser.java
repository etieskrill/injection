package org.etieskrill.injection.util;

@FunctionalInterface
public interface StringParser<T> {
    
    T parse(String[] parseable);
    
}
