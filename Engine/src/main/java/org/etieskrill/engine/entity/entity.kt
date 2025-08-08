package org.etieskrill.engine.entity

inline fun <reified T> Entity.getComponent() = getComponent(T::class.java)
