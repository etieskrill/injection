package org.etieskrill.game.horde.util

import org.etieskrill.engine.entity.Entity

inline fun <reified T> Entity.getComponent() = getComponent(T::class.java)
