package org.etieskrill.engine.input;

import kotlin.Pair;
import org.etieskrill.engine.input.InputBinding.Trigger;
import org.etieskrill.engine.input.action.Action;
import org.etieskrill.engine.input.action.DeltaAction;
import org.etieskrill.engine.input.action.SimpleAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.etieskrill.engine.input.InputBinding.Trigger.*;
import static org.lwjgl.glfw.GLFW.*;

public class InputManager implements KeyInputHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(InputManager.class);
    
    //TODO add <name,action> map to allow for rebinding and structural streamlining
    //TODO allow for keys to overrule/bind each other with activation policies; latest, oldest, all, none
    private final Map<KeyInput, Pair<Trigger, Action>> bindings;
    private final Map<KeyInput, OverruleGroup> groups;
    private final Map<OverruleGroup, Integer> groupKeysActive;
    
    private final Set<KeyInput> pressed;
    private final Set<KeyInput> toggled;
    private final Queue<KeyInput> events;
    
    public InputManager() {
        this(new HashMap<>(), new HashMap<>());
    }
    
    public InputManager(Map<KeyInput, Pair<Trigger, Action>> bindings, Map<KeyInput, OverruleGroup> groups) {
        this.bindings = Objects.requireNonNull(bindings);
        this.groups = Objects.requireNonNull(groups);
        this.groupKeysActive = new HashMap<>();
        this.pressed = new HashSet<>();
        this.toggled = new HashSet<>();
        this.events = new ArrayDeque<>();
    }
    
    public InputManager addBindings(InputBinding... bindings) {
        for (InputBinding binding : bindings) {
            this.bindings.put(binding.getInput(), new Pair<>(binding.getTrigger(), binding.getAction()));
            if (binding.getGroup() != null) addGroups(binding.getGroup());
        }
        return this;
    }
    
    public InputManager addGroups(OverruleGroup... groups) {
        for (OverruleGroup group : groups) {
            for (KeyInput input : group.getGroup()) {
                if (this.groups.put(input, group) != null) {
                    logger.warn("The binding {} is present in multiple groups, only the last group registered will be respected.",
                            Keys.fromKeyInput(input).name());
                }
            } //ik ik, this makes me puke too
        }
        
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
        
        //TODO introduce a mixed polling system for continuous binds and callbacks for triggers
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
        if (action == GLFW_PRESS && !pressed.contains(keyInput)) {
            if (!toggled.contains(keyInput)) toggled.add(keyInput);
            else toggled.remove(keyInput);
        }
        
        //update press status
        if (action != GLFW_RELEASE) {
            OverruleGroup group = groups.get(keyInput);
            if (group == null) {
                pressed.add(keyInput);
            } else {
                handleOverrule(group, keyInput);
            }
        } else {
            pressed.remove(keyInput);
        }
        
        //TODO finish implementation of OverruleGroup.Mode#NONE
        OverruleGroup overruleGroup = groups.get(keyInput);
        if (overruleGroup != null) {
            groupKeysActive.compute(overruleGroup, (group, count) -> {
                if (count == null) count = 0;
                return action != GLFW_RELEASE ? count+1 : count-1;
            });
        }
        
        return handled;
    }
    
    private void handleOverrule(OverruleGroup group, KeyInput keyInput) {
        switch (group.getMode()) {
            case YOUNGEST -> {
                pressed.removeAll(group.getGroup());
                pressed.add(keyInput);
            }
            case OLDEST -> {
                AtomicBoolean contained = new AtomicBoolean(false);
                group.getGroup().forEach(g -> { if (pressed.contains(g)) contained.set(true); }); //groups will be tiny, so this should be okay
                if (!contained.get()) pressed.add(keyInput);
            }
            default -> pressed.add(keyInput);
        }
    }
    
    public boolean isPressed(KeyInput input) {
        //TODO is this efficient at all, or would something like a HashMap<KeyInput, Boolean> (even with value = null)
        // be faster? these methods need to be optimised since they will be called every frame for certain keybinds
        return pressed.contains(input);
    }
    
    public boolean isPressed(Keys input) {
        return isPressed(input.getInput());
    }
    
    public boolean isToggled(KeyInput input) {
        return toggled.contains(input);
    }
    
}
