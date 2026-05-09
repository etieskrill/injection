package org.etieskrill.engine.entity.component;

import org.etieskrill.engine.entity.Script;

import java.util.ArrayList;
import java.util.List;

public class Scripts {

    private final List<Script> scripts;

    public Scripts() {
        this(new ArrayList<>());
    }

    public Scripts(List<Script> scripts) {
        this.scripts = scripts;
    }

    public List<Script> getSnippets() {
        return scripts;
    }

    public void addSnippet(Script script) {
        scripts.add(script);
    }

    public void update(double delta) {
        for (Script script : scripts) {
            script.accept(delta);
        }
    }

}
