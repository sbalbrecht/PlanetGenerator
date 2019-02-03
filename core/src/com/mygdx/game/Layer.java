package com.mygdx.game;

import com.badlogic.gdx.utils.Disposable;

public abstract class Layer implements Disposable {
    private boolean active;
    private String name;
    
    public Layer() {
        active = true;
        name = "New Layer";
    }

    public Layer(String name, boolean active) {
        this.active = active;
        this.name = name;
    }
    
    public abstract void resize(int screenWidth, int screenHeight);
    public abstract void update();
    public abstract void render();
    public abstract void dispose();
    public void setActive(boolean active){
        this.active = active;
    }
    public void setName(String name){
        this.name = name;
    }
    public boolean isActive() { return active; }
    public String getName() { return name; }
    
}
