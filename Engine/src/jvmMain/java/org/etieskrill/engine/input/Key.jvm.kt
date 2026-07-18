package org.etieskrill.engine.input

import org.etieskrill.engine.input.Key.*
import org.etieskrill.engine.input.KeyEventAction.*
import org.lwjgl.glfw.GLFW.*

fun parseGlfwKey(keyCode: Int) = when (keyCode) {
    GLFW_KEY_A -> A; GLFW_KEY_B -> B; GLFW_KEY_C -> C; GLFW_KEY_D -> D; GLFW_KEY_E -> E; GLFW_KEY_F -> F; GLFW_KEY_G -> G;
    GLFW_KEY_H -> H; GLFW_KEY_I -> I; GLFW_KEY_J -> J; GLFW_KEY_K -> K; GLFW_KEY_L -> L; GLFW_KEY_M -> M; GLFW_KEY_N -> N;
    GLFW_KEY_O -> O; GLFW_KEY_P -> P; GLFW_KEY_Q -> Q; GLFW_KEY_R -> R; GLFW_KEY_S -> S; GLFW_KEY_T -> T; GLFW_KEY_U -> U;
    GLFW_KEY_V -> V; GLFW_KEY_W -> W; GLFW_KEY_X -> X; GLFW_KEY_Y -> Y; GLFW_KEY_Z -> Z

    GLFW_KEY_0 -> _0; GLFW_KEY_1 -> _1; GLFW_KEY_2 -> _2; GLFW_KEY_3 -> _3; GLFW_KEY_4 -> _4
    GLFW_KEY_5 -> _5; GLFW_KEY_6 -> _6; GLFW_KEY_7 -> _7; GLFW_KEY_8 -> _8; GLFW_KEY_9 -> _9

    GLFW_KEY_SPACE -> SPACE

    GLFW_KEY_ESCAPE -> ESCAPE

    GLFW_KEY_F1 -> F1; GLFW_KEY_F2 -> F2; GLFW_KEY_F3 -> F3; GLFW_KEY_F4 -> F4; GLFW_KEY_F5 -> F5; GLFW_KEY_F6 -> F6
    GLFW_KEY_F7 -> F7; GLFW_KEY_F8 -> F8; GLFW_KEY_F9 -> F9; GLFW_KEY_F10 -> F10; GLFW_KEY_F11 -> F11; GLFW_KEY_F12 -> F12

    GLFW_MOUSE_BUTTON_LEFT -> LEFT_MOUSE; GLFW_MOUSE_BUTTON_RIGHT -> RIGHT_MOUSE; GLFW_MOUSE_BUTTON_MIDDLE -> MIDDLE_MOUSE
    GLFW_MOUSE_BUTTON_4 -> MOUSE_4; GLFW_MOUSE_BUTTON_5 -> MOUSE_5

    GLFW_KEY_LEFT_SHIFT -> LEFT_SHIFT; GLFW_KEY_RIGHT_SHIFT -> RIGHT_SHIFT
    GLFW_KEY_LEFT_CONTROL -> LEFT_CONTROL; GLFW_KEY_RIGHT_CONTROL -> RIGHT_CONTROL
    GLFW_KEY_LEFT_ALT -> LEFT_ALT; GLFW_KEY_RIGHT_ALT -> RIGHT_ALT
    GLFW_KEY_LEFT_SUPER -> LEFT_SUPER; GLFW_KEY_RIGHT_SUPER -> RIGHT_SUPER
    GLFW_MOD_CAPS_LOCK -> CAPSLOCK
    GLFW_KEY_NUM_LOCK -> NUMLOCK

    GLFW_KEY_ENTER -> ENTER; GLFW_KEY_BACKSPACE -> BACKSPACE

    GLFW_KEY_UP -> UP; GLFW_KEY_DOWN -> DOWN; GLFW_KEY_LEFT -> LEFT; GLFW_KEY_RIGHT -> RIGHT
    GLFW_KEY_HOME -> HOME; GLFW_KEY_END -> END
    GLFW_KEY_PAGE_UP -> PAGE_UP; GLFW_KEY_PAGE_DOWN -> PAGE_DOWN
    GLFW_KEY_INSERT -> INSERT; GLFW_KEY_DELETE -> DELETE

    else -> null
}

private val glfwModKeys = mapOf(
    GLFW_MOD_SHIFT to SHIFT,
    GLFW_MOD_CONTROL to CONTROL,
    GLFW_MOD_ALT to ALT,
    GLFW_MOD_SUPER to SUPER,
    GLFW_MOD_CAPS_LOCK to CAPSLOCK,
    GLFW_MOD_NUM_LOCK to NUMLOCK
)

fun parseGlfwModifierKeys(modsCode: Int) = glfwModKeys
    .filterKeys { glfwModKey -> glfwModKey and modsCode != 0 }
    .values
    .toList()

fun parseGlfwAction(actionCode: Int) = when (actionCode) {
    GLFW_PRESS -> PRESS
    GLFW_RELEASE -> RELEASE
    GLFW_REPEAT -> REPEAT
    else -> null
}
