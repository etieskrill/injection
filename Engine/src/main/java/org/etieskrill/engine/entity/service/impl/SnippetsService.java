package org.etieskrill.engine.entity.service.impl;

import org.etieskrill.engine.entity.Entity;
import org.etieskrill.engine.entity.component.Scripts;
import org.etieskrill.engine.entity.service.Service;

import java.util.List;

public class SnippetsService implements Service {
    @Override
    public boolean canProcess(Entity entity) {
        return entity.hasComponents(Scripts.class);
    }

    @Override
    public void process(Entity targetEntity, List<Entity> entities, double delta) {
        targetEntity.getComponent(Scripts.class).update(delta);
    }
}
