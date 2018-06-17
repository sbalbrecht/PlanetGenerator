package com.mygdx.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.TimeUtils;

/**
 * A nicer class for showing framerate that doesn't spam the console
 * like Logger.log()
 * 
 * @author William Hartman
 * https://gist.github.com/williamahartman/5584f037ed2748f57432
 */
public abstract class Layer implements Disposable{
    private boolean on;
    private String name;
    
    public Layer(){
        on =true;
        name = "New Layer";
    }
    public Layer(String name, boolean on){
    
    }
    
    public abstract void resize(int screenWidth, int screenHeight);
    public abstract void update();
    public abstract void render();
    public abstract void dispose();
    public void setOn(boolean on){
        this.on = on;
    }
    public void setName(String name){
        this.name = name;
    }
    public boolean getOn(){return on;}
    public String getName(){return name;}
    
}
