package org.etieskrill.engine.entity.component

import org.etieskrill.engine.entity.Script

@Suppress("JavaDefaultMethodsNotOverriddenByDelegation") //deprecated anyway
data class Scripts(
    val scripts: MutableList<Script> = mutableListOf()
) : MutableList<Script> by scripts {

    fun update(delta: Double) = forEach { it(delta) }

}
