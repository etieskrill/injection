package org.etieskrill.engine.input;

import kotlin.Pair;
import org.etieskrill.engine.input.InputBinding.Trigger;
import org.etieskrill.engine.input.action.Action;
import org.etieskrill.engine.input.action.DeltaAction;
import org.etieskrill.engine.input.action.SimpleAction;

import java.util.*;

import static org.etieskrill.engine.input.InputBinding.Trigger.*;
import static org.lwjgl.glfw.GLFW.*;

public class InputManager implements KeyInputHandler {
    
    private final Map<KeyInput, Pair<Trigger, Action>> bindings;
    private final Set<KeyInput> pressed;
    private final Set<KeyInput> toggled;
    private final Queue<KeyInput> events;
    
    public InputManager() {
        this(new HashMap<>());
    }
    
    public InputManager(Map<KeyInput, Pair<Trigger, Action>> bindings) {
        this.bindings = bindings;
        this.pressed = new HashSet<>();
        this.toggled = new HashSet<>();
        this.events = new ArrayDeque<>();
    }
    
    public InputManager addBinding(KeyInput keyInput, Trigger trigger, Action action) {
        this.bindings.put(keyInput, new Pair<>(trigger, action));
        return this;
    }
    
    public void update(double delta) {
        for (KeyInput keyInput : pressed) {
            Pair<Trigger, Action> triggerAction = bindings.get(keyInput);
            if (triggerAction != null && triggerAction.getFirst() == PRESSED)
                handleAction(triggerAction.getSecond(), delta);
        }
        for (KeyInput keyInput : toggled) {
            Pair<Trigger, Action> triggerAction = bindings.get(keyInput);
            if (triggerAction != null && triggerAction.getFirst() == TOGGLED)
                handleAction(triggerAction.getSecond(), delta);
        }
        while (events.peek() != null) {
            Pair<Trigger, Action> triggerAction = bindings.get(events.poll());
            if (triggerAction != null) handleAction(triggerAction.getSecond(), delta);
        }
    }
    
    private void handleAction(Action action, double delta) {
        if (action instanceof SimpleAction simpleAction) simpleAction.run();
        else if (action instanceof DeltaAction deltaAction) deltaAction.accept(delta);
    }
    
    @Override
    public boolean invoke(KeyInput.Type type, int key, int action, int modifiers) {
        if (action == GLFW_REPEAT) return false; //omitted for simplicity - for now
        
        //TODO this is waaaay to complicated, rework if any time, probs by introducing a mixed polling system for continuous binds and callbacks for triggers
        KeyInput keyInput = new KeyInput(type, key, modifiers);
        Pair<Trigger, Action> triggerAction = bindings.get(keyInput); //try lookup with exact modifiers
        if (triggerAction == null) {
            triggerAction = bindings.get(new KeyInput(type, key, 0)); //try lookup again without modifiers
            keyInput = new KeyInput(type, key, 0);
        }
        boolean handled = false;
    
        //queue press trigger event
        if (triggerAction != null && triggerAction.getFirst() == ON_PRESS) {
            if (action == GLFW_PRESS && !pressed.contains(keyInput)) {
                events.offer(keyInput);
                handled = true;
            }
        }
        
        //queue toggle trigger event
        if (triggerAction != null) {
            if (triggerAction.getFirst() == ON_TOGGLE &&
                    action == GLFW_PRESS && !toggled.contains(keyInput)) {
                events.offer(keyInput);
                handled = true;
            }
        }
    
        //update toggle status
        if ( action == GLFW_PRESS && !pressed.contains(keyInput)) {
            if (!toggled.contains(keyInput)) toggled.add(keyInput);
            else toggled.remove(keyInput);
        }
        
        //update press status
        if (action != GLFW_RELEASE) pressed.add(keyInput);
        else pressed.remove(keyInput);
        
        return handled;
    }
    
    public boolean isPressed(KeyInput input) {
        return pressed.contains(input);
    }
    
    public boolean isPressed(KeyInput.Keys input) {
        return pressed.contains(input.getInput());
    }
    
    public boolean isToggled(KeyInput input) {
        return toggled.contains(input);
    }
    
}
