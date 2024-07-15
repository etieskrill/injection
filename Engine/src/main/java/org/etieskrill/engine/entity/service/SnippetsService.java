package org.etieskrill.engine.entity.service;

import org.etieskrill.engine.entity.Entity;
import org.etieskrill.engine.entity.component.Snippets;

import java.util.List;

public class SnippetsService implements Service {
    @Override
    public boolean canProcess(Entity entity) {
        return entity.hasComponents(Snippets.class);
    }

    @Override
    public void process(Entity targetEntity, List<Entity> entities, double delta) {
        targetEntity.getComponent(Snippets.class).update(delta);
    }
}
