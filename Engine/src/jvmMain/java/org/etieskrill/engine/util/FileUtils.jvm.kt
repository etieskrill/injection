package org.etieskrill.engine.util

val separatorChar: Char = when (val platform = Platform.get()) {
    Platform.WINDOWS -> '\\'
    Platform.LINUX, Platform.MACOSX -> '/'
    else -> error("Unsupported architecture: $platform")
}
