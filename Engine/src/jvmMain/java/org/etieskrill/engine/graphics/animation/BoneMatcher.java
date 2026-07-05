package org.etieskrill.engine.graphics.animation;

import java.util.function.BiPredicate;

@FunctionalInterface
public interface BoneMatcher extends BiPredicate<String, String> {
}
