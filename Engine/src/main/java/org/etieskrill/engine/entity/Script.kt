package org.etieskrill.engine.entity;

import java.util.function.Consumer;

/**
 * A snippet of code local to an entity, which is queried with the delta time once every frame.
 * <p>
 * Useful for avoiding the overhead of creating an entire service for one-off functionality.
 */
//Unconventional in terms of ECSs, but meh. If it works, it works. And if it works while being intuitive to use,
//it really works.
@FunctionalInterface
public interface Script extends Consumer<Double> {
}
