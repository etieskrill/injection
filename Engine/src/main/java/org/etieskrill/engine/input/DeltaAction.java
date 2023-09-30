package org.etieskrill.engine.input;

import java.util.function.Consumer;

@FunctionalInterface
public interface DeltaAction extends Action, Consumer<Double> {}
