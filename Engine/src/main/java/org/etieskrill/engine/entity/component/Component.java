package org.etieskrill.engine.entity.component;

public class Component<T> {

    private final int id;
    private final T component;

    public Component(int id, T component) {
        this.id = id;
        this.component = component;
    }

    public int getId() {
        return id;
    }

    public T getComponent() {
        return component;
    }

}
