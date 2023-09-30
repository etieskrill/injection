package org.etieskrill.engine.input;

import kotlin.Pair;
import org.etieskrill.engine.input.InputBinding.Trigger;

import java.util.*;

import static org.etieskrill.engine.input.InputBinding.Trigger.*;
import static org.lwjgl.glfw.GLFW.*;

public class InputManager implements InputHandler {
    
    private final Map<Input, Pair<Trigger, Action>> bindings;
    private final Set<Input> pressed;
    private final Set<Input> toggledOn;
    private final Queue<Input> events;
    
    public InputManager() {
        this(new HashMap<>());
    }
    
    public InputManager(Map<Input, Pair<Trigger, Action>> bindings) {
        this.bindings = bindings;
        this.pressed = new HashSet<>();
        this.toggledOn = new HashSet<>();
        this.events = new ArrayDeque<>();
    }
    
    public InputManager addBinding(Input input, Trigger trigger, Action action) {
        this.bindings.put(input, new Pair<>(trigger, action));
        return this;
    }
    
    public void update(double delta) {
        for (Input input : pressed) {
            Pair<Trigger, Action> triggerAction = bindings.get(input);
            if (triggerAction.getFirst() == PRESSED)
                handleAction(triggerAction.getSecond(), delta);
        }
        for (Input input : toggledOn) {
            Pair<Trigger, Action> triggerAction = bindings.get(input);
            if (triggerAction.getFirst() == TOGGLED)
                handleAction(triggerAction.getSecond(), delta);
        }
        while (events.peek() != null) {
            Pair<Trigger, Action> triggerAction = bindings.get(events.poll());
            handleAction(triggerAction.getSecond(), delta);
        }
    }
    
    private void handleAction(Action action, double delta) {
        if (action instanceof SimpleAction simpleAction) simpleAction.run();
        else if (action instanceof DeltaAction deltaAction) deltaAction.accept(delta);
    }
    
    @Override
    public boolean invoke(Input.Type type, int key, int action, int modifiers) {
        Input input = new Input(type, key);
        Pair<Trigger, Action> triggerAction = bindings.get(input);
        if (triggerAction == null) return false;
        boolean handled = false;
    
        if (triggerAction.getFirst() == ON_PRESS) {
            if (action == GLFW_PRESS && !pressed.contains(input)) {
                events.offer(input);
                handled = true;
            }
        }
        
        if (action != GLFW_RELEASE) pressed.add(input);
        else pressed.remove(input);
    
        { //TODO un-disgustify
            if (action != GLFW_PRESS || pressed.contains(input)) {
                if (!toggledOn.contains(input)) {
                    toggledOn.add(input);
                    if (triggerAction.getFirst() == ON_TOGGLE) {
                        events.offer(input);
                        handled = true;
                    }
                } else
                    toggledOn.remove(input);
            }
        }
        
        return handled;
    }
    
    public boolean isToggled(Input input) {
        return toggledOn.contains(input);
    }
    
}
