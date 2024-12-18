package org.etieskrill.game.horde.component;

import org.etieskrill.engine.entity.Entity;

public class EnemyState {

    private Entity target;

    public enum State {
        NONE,
        IDLE,
        ALERTED,
        ATTACKING,
        FLEEING,
    }

}
