package org.etieskrill.engine.input;

import org.etieskrill.engine.common.EveryFrame;
import org.etieskrill.engine.input.InputBinding.Trigger;
import org.etieskrill.engine.input.action.Action;
import org.etieskrill.engine.input.action.DeltaAction;
import org.etieskrill.engine.input.action.SimpleAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Collections.synchronizedSet;
import static org.etieskrill.engine.input.InputBinding.Trigger.*;
import static org.lwjgl.glfw.GLFW.*;

public class KeyInputManager implements KeyInputHandler {

    private static final Logger logger = LoggerFactory.getLogger(KeyInputManager.class);

    //TODO add <name,action> map to allow for rebinding and structural streamlining
    //TODO allow for keys to overrule/bind each other with activation policies; latest, oldest, all, none
    private final Map<KeyEvent, TriggerAction> bindings;
    private final Map<KeyEvent, OverruleGroup> groups;
    private final Map<OverruleGroup, Integer> groupKeysActive;

    private final Set<KeyEvent> pressed;
    private final Set<KeyEvent> toggled;
    private final Queue<KeyEvent> events;

    public KeyInputManager() {
        this(new HashMap<>(), new HashMap<>());
    }

    public KeyInputManager(Map<KeyEvent, TriggerAction> bindings, Map<KeyEvent, OverruleGroup> groups) {
        this.bindings = Objects.requireNonNull(bindings);
        this.groups = Objects.requireNonNull(groups);
        this.groupKeysActive = new HashMap<>();
        this.pressed = synchronizedSet(new HashSet<>());
        this.toggled = synchronizedSet(new HashSet<>());
        this.events = new LinkedBlockingDeque<>();
    }

    public record TriggerAction(
            Trigger trigger,
            Action action
    ) {
    }

    public KeyInputManager addBindings(InputBinding... bindings) {
        for (InputBinding binding : bindings) {
            this.bindings.put(binding.getInput(), new TriggerAction(binding.getTrigger(), binding.getAction()));
            if (binding.getGroup() != null) addGroups(binding.getGroup());
        }
        return this;
    }

    public KeyInputManager removeBindings(KeyEvent... keys) {
        for (KeyEvent key : keys)
            this.bindings.remove(key);
        return this;
    }

    public KeyInputManager addGroups(OverruleGroup... groups) {
        for (OverruleGroup group : groups) {
            for (KeyEvent input : group.getGroup()) {
                if (this.groups.put(input, group) != null) {
                    logger.warn("The binding {} is present in multiple groups, only the last group registered will be respected.",
                            Key.fromKeyInput(input).name());
                }
            } //ik ik, this makes me puke too
        }

        return this;
    }

    @EveryFrame
    public void update(double delta) {
        for (KeyEvent key : pressed) {
            TriggerAction triggerAction = bindings.get(key);
            if (triggerAction != null && triggerAction.trigger() == PRESSED)
                handleAction(triggerAction.action(), delta);
        }
        for (KeyEvent key : toggled) {
            TriggerAction triggerAction = bindings.get(key);
            if (triggerAction != null && triggerAction.trigger() == TOGGLED)
                handleAction(triggerAction.action(), delta);
        }
        while (events.peek() != null) {
            TriggerAction triggerAction = bindings.get(events.poll());
            if (triggerAction != null) handleAction(triggerAction.action(), delta);
        }
    }

    private void handleAction(Action action, double delta) {
        if (action instanceof SimpleAction simpleAction) simpleAction.run();
        else if (action instanceof DeltaAction deltaAction) deltaAction.accept(delta);
    }

    @Override
    public boolean invoke(KeyEvent.Type type, int key, int action, int modifiers) {
        if (action == GLFW_REPEAT) return false; //omitted for simplicity - for now

        //TODO introduce a mixed polling system for continuous binds and callbacks for triggers
        KeyEvent keyInput = new KeyEvent(type, key, modifiers);
        TriggerAction triggerAction = bindings.get(keyInput); //try lookup with exact modifiers
        if (triggerAction == null) {
            triggerAction = bindings.get(new KeyEvent(type, key, 0)); //try lookup again without modifiers
            keyInput = new KeyEvent(type, key, 0);
        }
        boolean handled = false;

        //queue press trigger event
        if (triggerAction != null && triggerAction.trigger() == ON_PRESS) {
            if (action == GLFW_PRESS && !pressed.contains(keyInput)) {
                events.offer(keyInput);
                handled = true;
            }
        }

        //queue toggle trigger event
        if (triggerAction != null) {
            if (triggerAction.trigger() == ON_TOGGLE &&
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
                return action != GLFW_RELEASE ? count + 1 : count - 1;
            });
        }

        return handled;
    }

    private void handleOverrule(OverruleGroup group, KeyEvent key) {
        switch (group.getMode()) {
            case YOUNGEST -> {
                pressed.removeAll(group.getGroup());
                pressed.add(key);
            }
            case OLDEST -> {
                AtomicBoolean contained = new AtomicBoolean(false);
                group.getGroup().forEach(g -> {
                    if (pressed.contains(g)) contained.set(true);
                }); //groups will be tiny, so this should be okay
                if (!contained.get()) pressed.add(key);
            }
            //TODO add NONE
            default -> pressed.add(key);
        }
    }

    public boolean isPressed(KeyEvent input) {
        return pressed.contains(input);
    }

    public boolean isPressed(Key input) {
        return isPressed(input.getInput());
    }

    public boolean isToggled(KeyEvent input) {
        return toggled.contains(input);
    }

}
