package org.etieskrill.engine.input;

import kotlin.Pair;
import org.etieskrill.engine.EveryFrame;
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

public class KeyInputManager implements KeyInputHandler {

    private static final Logger logger = LoggerFactory.getLogger(KeyInputManager.class);
    
    //TODO add <name,action> map to allow for rebinding and structural streamlining
    //TODO allow for keys to overrule/bind each other with activation policies; latest, oldest, all, none
    private final Map<Key, Pair<Trigger, Action>> bindings;
    private final Map<Key, OverruleGroup> groups;
    private final Map<OverruleGroup, Integer> groupKeysActive;

    private final Set<Key> pressed;
    private final Set<Key> toggled;
    private final Queue<Key> events;

    public KeyInputManager() {
        this(new HashMap<>(), new HashMap<>());
    }

    public KeyInputManager(Map<Key, Pair<Trigger, Action>> bindings, Map<Key, OverruleGroup> groups) {
        this.bindings = Objects.requireNonNull(bindings);
        this.groups = Objects.requireNonNull(groups);
        this.groupKeysActive = new HashMap<>();
        this.pressed = new HashSet<>();
        this.toggled = new HashSet<>();
        this.events = new ArrayDeque<>();
    }

    public KeyInputManager addBindings(InputBinding... bindings) {
        for (InputBinding binding : bindings) {
            this.bindings.put(binding.getInput(), new Pair<>(binding.getTrigger(), binding.getAction()));
            if (binding.getGroup() != null) addGroups(binding.getGroup());
        }
        return this;
    }

    public KeyInputManager addGroups(OverruleGroup... groups) {
        for (OverruleGroup group : groups) {
            for (Key input : group.getGroup()) {
                if (this.groups.put(input, group) != null) {
                    logger.warn("The binding {} is present in multiple groups, only the last group registered will be respected.",
                            Keys.fromKeyInput(input).name());
                }
            } //ik ik, this makes me puke too
        }
        
        return this;
    }
    
    @EveryFrame
    public void update(double delta) {
        for (Key key : pressed) {
            Pair<Trigger, Action> triggerAction = bindings.get(key);
            if (triggerAction != null && triggerAction.getFirst() == PRESSED)
                handleAction(triggerAction.getSecond(), delta);
        }
        for (Key key : toggled) {
            Pair<Trigger, Action> triggerAction = bindings.get(key);
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
    public boolean invoke(Key.Type type, int key, int action, int modifiers) {
        if (action == GLFW_REPEAT) return false; //omitted for simplicity - for now
        
        //TODO introduce a mixed polling system for continuous binds and callbacks for triggers
        Key keyInput = new Key(type, key, modifiers);
        Pair<Trigger, Action> triggerAction = bindings.get(keyInput); //try lookup with exact modifiers
        if (triggerAction == null) {
            triggerAction = bindings.get(new Key(type, key, 0)); //try lookup again without modifiers
            keyInput = new Key(type, key, 0);
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
            pressed.remove(keyInput.withoutModifiers());
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

    private void handleOverrule(OverruleGroup group, Key key) {
        switch (group.getMode()) {
            case YOUNGEST -> {
                pressed.removeAll(group.getGroup());
                pressed.add(key);
            }
            case OLDEST -> {
                AtomicBoolean contained = new AtomicBoolean(false);
                group.getGroup().forEach(g -> { if (pressed.contains(g)) contained.set(true); }); //groups will be tiny, so this should be okay
                if (!contained.get()) pressed.add(key);
            }
            //TODO add NONE
            default -> pressed.add(key);
        }
    }

    public boolean isPressed(Key input) {
        //TODO is this efficient at all, or would something like a HashMap<KeyInput, Boolean> (even with value = null)
        // be faster? these methods need to be optimised since they will be called every frame for certain keybinds
        return pressed.contains(input);
    }
    
    public boolean isPressed(Keys input) {
        return isPressed(input.getInput());
    }

    public boolean isToggled(Key input) {
        return toggled.contains(input);
    }

}
