package org.etieskrill.engine.util

/**
 * File path without file name and extension.
 */
val String.path get() = substringBeforeLast("/", "")

/**
 * File path and name without extension.
 */
val String.pathName get() = substringBeforeLast(".") //TODO handle dotfiles

/**
 * File name and extension without path.
 */
val String.name get() = substringAfterLast("/")

/**
 * File name without extension or path.
 */
val String.fileName get() = name.substringBefore(".") //TODO handle dotfiles

/**
 * File extension without name or path.
 */
val String.extension get() = name.substringAfterLast(".", "")

/**
 * File sub-extensions without path, name or primary extension.
 */
val String.subExtension get() = name
    .substringAfter(".", "")
    .substringBeforeLast(".", "") //TODO handle dotfiles
