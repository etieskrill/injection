package org.etieskrill.engine.entity.system;

import org.etieskrill.engine.entity.Entity;
import org.etieskrill.engine.entity.data.Component;

import java.util.List;
import java.util.Map;

public interface EntitySystem {

    void update(float delta, Map<Entity, List<Component>> entities);

}
