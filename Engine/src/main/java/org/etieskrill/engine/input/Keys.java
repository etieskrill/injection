package org.etieskrill.engine.input;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.glfw.GLFW.*;

public enum Keys {
    A(GLFW_KEY_A), B(GLFW_KEY_B), C(GLFW_KEY_C), D(GLFW_KEY_D), E(GLFW_KEY_E), F(GLFW_KEY_F), G(GLFW_KEY_G),
    H(GLFW_KEY_H), I(GLFW_KEY_I), J(GLFW_KEY_J), K(GLFW_KEY_K), L(GLFW_KEY_L), M(GLFW_KEY_M), N(GLFW_KEY_N),
    O(GLFW_KEY_O), P(GLFW_KEY_P), Q(GLFW_KEY_Q), R(GLFW_KEY_R), S(GLFW_KEY_S), T(GLFW_KEY_T), U(GLFW_KEY_U),
    V(GLFW_KEY_V), W(GLFW_KEY_W), X(GLFW_KEY_X), Y(GLFW_KEY_Y), Z(GLFW_KEY_Z),
    _0(GLFW_KEY_0), _1(GLFW_KEY_1), _2(GLFW_KEY_2), _3(GLFW_KEY_3), _4(GLFW_KEY_4), _5(GLFW_KEY_5), _6(GLFW_KEY_6),
    _7(GLFW_KEY_7), _8(GLFW_KEY_8), _9(GLFW_KEY_9),
    SPACE(GLFW_KEY_SPACE),

    ESCAPE(GLFW_KEY_ESCAPE),
    ESC(ESCAPE),

    F1(GLFW_KEY_F1), F2(GLFW_KEY_F2), F3(GLFW_KEY_F3), F4(GLFW_KEY_F4), F5(GLFW_KEY_F5), F6(GLFW_KEY_F6),
    F7(GLFW_KEY_F7), F8(GLFW_KEY_F8), F9(GLFW_KEY_F9), F10(GLFW_KEY_F10), F11(GLFW_KEY_F11), F12(GLFW_KEY_F12),

    LEFT_MOUSE(GLFW_MOUSE_BUTTON_LEFT, Key.Type.MOUSE),
    RIGHT_MOUSE(GLFW_MOUSE_BUTTON_RIGHT, Key.Type.MOUSE),
    MIDDLE_MOUSE(GLFW_MOUSE_BUTTON_MIDDLE, Key.Type.MOUSE),
    MOUSE_4(GLFW_MOUSE_BUTTON_4, Key.Type.MOUSE), MOUSE_5(GLFW_MOUSE_BUTTON_5, Key.Type.MOUSE),

    LEFT_SHIFT(GLFW_KEY_LEFT_SHIFT, Mod.SHIFT), RIGHT_SHIFT(GLFW_KEY_RIGHT_SHIFT, Mod.SHIFT),
    SHIFT(GLFW_KEY_LEFT_SHIFT, Mod.SHIFT),
    LEFT_CONTROL(GLFW_KEY_LEFT_CONTROL, Mod.CONTROL), RIGHT_CONTROL(GLFW_KEY_RIGHT_CONTROL, Mod.CONTROL),
    CONTROL(GLFW_KEY_LEFT_CONTROL, Mod.CONTROL), CTRL(CONTROL),
    LEFT_ALT(GLFW_KEY_LEFT_ALT, Mod.ALT), RIGHT_ALT(GLFW_KEY_RIGHT_ALT),
    ALT(GLFW_KEY_LEFT_ALT),
    LEFT_SUPER(GLFW_KEY_LEFT_SUPER, Mod.SUPER), RIGHT_SUPER(GLFW_KEY_RIGHT_SUPER, Mod.SUPER),
    SUPER(GLFW_KEY_LEFT_SUPER, Mod.SUPER),
    CAPSLOCK(GLFW_MOD_CAPS_LOCK), CAPSLK(CAPSLOCK),
    NUMLOCK(GLFW_KEY_NUM_LOCK),

    ENTER(GLFW_KEY_ENTER), BACKSPACE(GLFW_KEY_BACKSPACE);

    public enum Action {
        PRESS(GLFW_PRESS), RELEASE(GLFW_RELEASE), REPEAT(GLFW_REPEAT);

        private final @Getter int glfwAction;

        Action(int glfwAction) {
            this.glfwAction = glfwAction;
        }

        public static Action fromGLFW(int glfwAction) {
            return switch (glfwAction) {
                case GLFW_PRESS -> PRESS;
                case GLFW_RELEASE -> RELEASE;
                case GLFW_REPEAT -> REPEAT;
                default -> null;
            };
        }
    }

    private final Mod modifierKey;

    private static final Map<Key, Map<Mod, Key>> keyCache = new HashMap<>(256);
    private final Key input;

    public enum Mod {
        SHIFT(GLFW_MOD_SHIFT, Keys.SHIFT),
        CONTROL(GLFW_MOD_CAPS_LOCK, Keys.CONTROL),
        ALT(GLFW_MOD_ALT, Keys.ALT),
        SUPER(GLFW_MOD_SUPER, Keys.SUPER),
        CAPSLOCK(GLFW_MOD_CAPS_LOCK, Keys.CAPSLOCK),
        NUMLOCK(GLFW_KEY_NUM_LOCK, Keys.NUMLOCK),
        NONE(0),

        //TODO add remaining combinations or think of a smarter solution for this
        CONTROL_SHIFT(GLFW_MOD_CONTROL | GLFW_MOD_SHIFT),
        CONTROL_ALT(GLFW_MOD_CONTROL | GLFW_MOD_ALT),
        ALT_SHIFT(GLFW_MOD_ALT | GLFW_MOD_SHIFT),
        CONTROL_ALT_SHIFT(GLFW_MOD_CONTROL | GLFW_MOD_ALT | GLFW_MOD_SHIFT);

        private final int glfwKey;
        private final Keys key;

        Mod(int glfwKey) {
            this(glfwKey, null);
        }

        Mod(int glfwKey, Keys key) {
            this.glfwKey = glfwKey;
            this.key = key;
        }

        public int getGlfwKey() {
            return glfwKey;
        }

        public Keys toKey() {
            return key;
        }

        public static @NotNull Mod fromGlfw(int glfwMods) {
            return switch (glfwMods) {
                case GLFW_MOD_SHIFT -> SHIFT;
                case GLFW_MOD_CONTROL -> CONTROL;
                case GLFW_MOD_ALT -> ALT;
                case GLFW_MOD_SUPER -> SUPER;
                case GLFW_MOD_CAPS_LOCK -> CAPSLOCK;
                case GLFW_MOD_NUM_LOCK -> NUMLOCK;
                case GLFW_MOD_CONTROL | GLFW_MOD_SHIFT -> CONTROL_SHIFT;
                case GLFW_MOD_CONTROL | GLFW_MOD_ALT -> CONTROL_ALT;
                case GLFW_MOD_ALT | GLFW_MOD_SHIFT -> ALT_SHIFT;
                case GLFW_MOD_CONTROL | GLFW_MOD_ALT | GLFW_MOD_SHIFT -> CONTROL_ALT_SHIFT;
                default -> NONE;
            };
        }
    }

    Keys(int glfwKey) {
        this(glfwKey, (Mod) null);
    }

    Keys(int glfwKey, Mod modifierKey) {
        this(glfwKey, modifierKey, Key.Type.KEYBOARD);
    }

    Keys(int glfwKey, Key.Type type) {
        this(glfwKey, null, type);
    }

    Keys(int glfwKey, Mod modifierKey, Key.Type type) {
        this.modifierKey = modifierKey;
        this.input = new Key(type, glfwKey);
    }

    Keys(Keys key) {
        this.modifierKey = key.modifierKey;
        this.input = key.input;
    }

    /**
     * Convenience overload to also allow for {@link Keys} to be used. </br>
     * Does nothing if {@code key} is not a modifier.
     */
    public Key withMods(Keys... keys) {
        Mod[] mods = Arrays.stream(keys).filter(key -> key != null && key.modifierKey != null).map(key -> key.modifierKey).toArray(Mod[]::new);
        return withMods(mods);
    }

    public Key withMods(Mod... mods) {
        Map<Mod, Key> input = keyCache.computeIfAbsent(this.input, k -> new HashMap<>(4));
        int glfwMods = Arrays.stream(mods).distinct().mapToInt(mod -> mod.glfwKey).sum(); //both adding and or-ing would work, since the modifiers are flags
        return input.computeIfAbsent(Mod.fromGlfw(glfwMods), k -> new Key(this.input.getType(), this.input.getValue(), glfwMods));
    }

    public static Keys fromGlfw(int glfwKey) {
        for (Keys key : Keys.values()) {
            if (key.getInput().getValue() == glfwKey)
                return key;
        }
        return null;
    }

    public static Keys fromKeyInput(Key key) {
        return fromGlfw(key.getValue());
    }

    public Key getInput() {
        return input;
    }
}