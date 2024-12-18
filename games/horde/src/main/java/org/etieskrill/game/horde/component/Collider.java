package org.etieskrill.game.horde.component;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@AllArgsConstructor
@RequiredArgsConstructor
public class Collider {
    private final float radius;
    private boolean immobile = false;
    private boolean solid = true;
}
