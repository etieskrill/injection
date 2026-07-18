package org.etieskrill.engine.common;

//FIXME dunno if this works cuz docs not downloaded and too lazy to look at web
class ResourceLoadException(message: String? = null, cause: Throwable? = null) : RuntimeException(message, cause) {
    constructor(message: String) : this(message, null)
    constructor(cause: Throwable) : this(null, cause)
}
