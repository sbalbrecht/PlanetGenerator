package com.mygdx.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class TileInfoLayer extends Layer{
	
	private BitmapFont font;
	private SpriteBatch batch;
	private String information;
	private Face f;
	
	public TileInfoLayer(){
		font = new BitmapFont();
		batch = new SpriteBatch();
	}
	
	@Override
	public void resize(int screenWidth, int screenHeight){
	
	}
	
	@Override
	public void update(){
			information="";//TODO
	}
	
	@Override
	public void render(){
		batch.begin();
		font.draw(batch, information, 3, 3);
		batch.end();
	}
	
	public void setFace(Face f){
		this.f = f;
		return;
	}
	
	@Override
	public void dispose(){
	
	}
}
