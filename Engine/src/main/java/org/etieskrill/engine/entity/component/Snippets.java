package org.etieskrill.engine.entity.component;

import org.etieskrill.engine.entity.Script;

import java.util.ArrayList;
import java.util.List;

public class Snippets {

    private final List<Script> scripts;

    public Snippets() {
        this(new ArrayList<>());
    }

    public Snippets(List<Script> scripts) {
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
