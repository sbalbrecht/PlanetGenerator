package dev.urth.planetgen.layer;

import com.badlogic.gdx.utils.Disposable;

public abstract class Layer implements Disposable {
    private boolean active;
    private String name;

    protected Layer() {
        active = true;
        name = "New Layer";
    }

    protected Layer(String name, boolean active) {
        this.active = active;
        this.name = name;
    }

    public abstract void resize(int screenWidth, int screenHeight);

    public abstract void update();

    public abstract void render();

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isActive() {
        return active;
    }

    public String getName() {
        return name;
    }
}
