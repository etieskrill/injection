package org.etieskrill.engine.input

enum class Key(
    val type: KeyType = KeyType.KEYBOARD,
    val mod: ModifierKey? = null,
    val alias: Key? = null
) {
    A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V, W, X, Y, Z,
    _0, _1, _2, _3, _4, _5, _6, _7, _8, _9,
    SPACE,

    ESCAPE, ESC(alias = ESCAPE),

    F1, F2, F3, F4, F5, F6, F7, F8, F9, F10, F11, F12,

    LEFT_MOUSE(KeyType.MOUSE), RIGHT_MOUSE(KeyType.MOUSE),
    MIDDLE_MOUSE(KeyType.MOUSE),
    MOUSE_4(KeyType.MOUSE), MOUSE_5(KeyType.MOUSE),

    LEFT_SHIFT(mod = ModifierKey.SHIFT), RIGHT_SHIFT(mod = ModifierKey.SHIFT),
    SHIFT(mod = ModifierKey.SHIFT, alias = LEFT_SHIFT),

    LEFT_CONTROL(mod = ModifierKey.CONTROL), RIGHT_CONTROL(mod = ModifierKey.CONTROL),
    CONTROL(mod = ModifierKey.CONTROL, alias = LEFT_CONTROL), CTRL(mod = ModifierKey.CONTROL, alias = CONTROL),

    LEFT_ALT(mod = ModifierKey.ALT), RIGHT_ALT(mod = ModifierKey.ALT), ALT(mod = ModifierKey.ALT, alias = LEFT_ALT),
    LEFT_SUPER(mod = ModifierKey.SUPER), RIGHT_SUPER(mod = ModifierKey.SUPER),
    SUPER(mod = ModifierKey.SUPER, alias = LEFT_SUPER),

    CAPSLOCK(mod = ModifierKey.CAPSLOCK),

    NUMLOCK(mod = ModifierKey.NUMLOCK),

    ENTER, BACKSPACE,

    UP, DOWN, LEFT, RIGHT,
    HOME, END,
    PAGE_UP, PAGE_DOWN,
    INSERT, DELETE
}

enum class ModifierKey { SHIFT, CONTROL, ALT, SUPER, CAPSLOCK, NUMLOCK }

enum class KeyType { KEYBOARD, MOUSE }
