package org.etieskrill.engine.input;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.glfw.GLFW.*;

public class KeyInput {
    
    private final Type type;
    private final int value;
    private final int modifiers;
    private final boolean modifier;
    
    public static final int[] modifierKeys = {
            GLFW_KEY_LEFT_SHIFT,
            GLFW_KEY_RIGHT_SHIFT,
            GLFW_KEY_LEFT_ALT,
            GLFW_KEY_RIGHT_ALT,
            GLFW_KEY_CAPS_LOCK,
            GLFW_KEY_LEFT_CONTROL,
            GLFW_KEY_RIGHT_CONTROL,
            GLFW_KEY_NUM_LOCK
    };
    
    public enum Type {
        KEY,
        MOUSE
    }
    
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
        
        LEFT_SHIFT(GLFW_KEY_LEFT_SHIFT, Mod.SHIFT), RIGHT_SHIFT(GLFW_KEY_RIGHT_SHIFT, Mod.SHIFT),
        SHIFT(GLFW_KEY_LEFT_SHIFT, Mod.SHIFT),
        LEFT_CONTROL(GLFW_KEY_LEFT_CONTROL, Mod.CONTROL), RIGHT_CONTROL(GLFW_KEY_RIGHT_CONTROL, Mod.CONTROL),
        CONTROL(GLFW_KEY_LEFT_CONTROL, Mod.CONTROL), CTRL(CONTROL),
        LEFT_ALT(GLFW_KEY_LEFT_ALT, Mod.ALT), RIGHT_ALT(GLFW_KEY_RIGHT_ALT),
        ALT(GLFW_KEY_LEFT_ALT),
        LEFT_SUPER(GLFW_KEY_LEFT_SUPER, Mod.SUPER), RIGHT_SUPER(GLFW_KEY_RIGHT_SUPER, Mod.SUPER),
        SUPER(GLFW_KEY_LEFT_SUPER, Mod.SUPER),
        CAPSLOCK(GLFW_MOD_CAPS_LOCK), CAPSLK(CAPSLOCK),
        NUMLOCK(GLFW_KEY_NUM_LOCK);
    
        private final Mod modifierKey;
    
        private static final Map<KeyInput, Map<Keys.Mod, KeyInput>> keyCache = new HashMap<>(256);
        private final KeyInput input;
        
        public enum Mod {
            SHIFT(GLFW_MOD_SHIFT, Keys.SHIFT),
            CONTROL(GLFW_MOD_CAPS_LOCK, Keys.CONTROL),
            ALT(GLFW_MOD_ALT, Keys.ALT),
            SUPER(GLFW_MOD_SUPER, Keys.SUPER),
            CAPSLOCK(GLFW_MOD_CAPS_LOCK, Keys.CAPSLOCK),
            NUMLOCK(GLFW_KEY_NUM_LOCK, Keys.NUMLOCK),
            
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
            
            public static Mod fromGlfw(int glfwMods) {
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
                    default -> null;
                };
            }
        }
        
        Keys(int glfwKey) {
            this(glfwKey, null);
        }
        
        Keys(int glfwKey, Mod modifierKey) {
            this.modifierKey = modifierKey;
            this.input = new KeyInput(Type.KEY, glfwKey);
        }
        
        Keys(Keys key) {
            this.modifierKey = key.modifierKey;
            this.input = key.input;
        }
    
        /**
         * Convenience overload to also allow for {@link Keys} to be used. </br>
         * Does nothing if {@code key} is not a modifier.
         */
        //TODO modifier key combining & chaining
        public KeyInput withMods(Keys key) {
            if (key.modifierKey == null) return key.getInput();
            return withMods(key.modifierKey);
        }
        
        public KeyInput withMods(Mod... mods) {
            Map<Mod, KeyInput> input = keyCache.computeIfAbsent(this.input, k -> new HashMap<>(4));
            int glfwMods = 0;
            for (Mod mod : mods)
                glfwMods |= mod.glfwKey; //both adding and or-ing would work, since the modifiers are flags
            return input.computeIfAbsent(Mod.fromGlfw(glfwMods), k -> new KeyInput(Type.KEY, this.input.getValue(), 2));
        }
    
        public KeyInput getInput() {
            return input;
        }
    }
    
    public KeyInput(Type type, int value) {
        this(type, value, 0);
    }
    
    public KeyInput(Type type, int value, int modifiers) {
        this.type = type;
        this.value = value;
        this.modifiers = isModifier() ? 0 : modifiers;
        this.modifier = Arrays.stream(modifierKeys).anyMatch(key -> key == this.value);
    }
    
    public boolean isModifier() {
        return modifier;
    }
    
    public Type getType() {
        return type;
    }
    
    public int getValue() {
        return value;
    }
    
    public int getModifiers() {
        return modifiers;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        KeyInput that = (KeyInput) o;
        
        if (value != that.value) return false;
        if (modifiers != that.modifiers && !isModifier()) return false;
        return type == that.type;
    }
    
    @Override
    public int hashCode() {
        int result = type != null ? type.hashCode() : 0;
        result = 31 * result + value;
        if (!isModifier()) result = 31 * result + modifiers;
        return result;
    }
    
}
