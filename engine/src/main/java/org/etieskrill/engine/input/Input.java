package org.etieskrill.engine.input;

import org.etieskrill.engine.input.action.DeltaAction;
import org.etieskrill.engine.input.action.SimpleAction;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class Input {
    
    //TODO i do not trust working with such annotations, since i haven't even been able to find a plugin for vscode which processes these like intellij does
    public static KeyInputManager of(@NotNull InputBinding... bindings) {
        if (Arrays.stream(bindings).anyMatch(Objects::isNull))
            throw new NullPointerException("Invalid list of inputs");
        KeyInputManager manager = new KeyInputManager();
        for (InputBinding binding : bindings) manager.addBindings(binding);
        return manager;
    }

    public static BindPassOn bind(Key binding) {
        return new BindPassOn(binding);
    }
    
    public static BindPassOn bind(Keys key) {
        return bind(key.getInput());
    }
    
    //TODO this is basically a builder, so label it as such
    public static final class BindPassOn {
        private final Key binding;
        
        private InputBinding.Trigger trigger = InputBinding.Trigger.ON_PRESS;
        private OverruleGroup group;

        private BindPassOn(Key binding) {
            this.binding = binding;
        }
        
        public BindPassOn on(InputBinding.Trigger trigger) {
            this.trigger = trigger;
            return this;
        }

        public BindPassOn group(OverruleGroup.Mode mode, Key... bindings) {
            Set<Key> binds = new HashSet<>(List.of(bindings));
            binds.add(binding); //if you know a prettier solution to this, then please, feel free
            this.group = new OverruleGroup(binds, mode);
            return this;
        }
        
        public BindPassOn group(OverruleGroup.Mode mode, Keys... bindings) {
            return group(mode, Arrays.stream(bindings).map(Keys::getInput).toArray(Key[]::new));
        }
        
        public InputBinding to(SimpleAction action) {
            return new InputBinding(binding, trigger, action, group);
        }
    
        public InputBinding to(DeltaAction action) {
            return new InputBinding(binding, trigger, action, group);
        }
    }
    
}
