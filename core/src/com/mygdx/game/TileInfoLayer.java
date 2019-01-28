package com.mygdx.game;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

public class TileInfoLayer extends Layer implements Disposable {
	
	private BitmapFont font;
	private Label label;
	private SpriteBatch batch;
	private StringBuilder stringBuilder;
	private String information;
	private Stage stage;
	private Tile tile;
	
	public TileInfoLayer() {
		stage = new Stage();
		stage.setViewport(new ScreenViewport());
		font = new BitmapFont();
        label = new Label(" ", new Label.LabelStyle(font, Color.WHITE));
        stage.addActor(label);
        stringBuilder = new StringBuilder();
//        batch = new SpriteBatch();
	}

	@Override
	public void resize(int screenWidth, int screenHeight) {
		stage.getViewport().update(screenWidth, screenHeight, true);
	}

	@Override
	public void update() {

	}

	@Override
	public void render(){
        label.setPosition(0,stage.getViewport().getScreenHeight()-20);
		stringBuilder.setLength(0);
		if (tile == null) {
		    stringBuilder.append("");
        } else {
		    stringBuilder.append(" Elevation: ").append(tile.getElevation());

        }
		label.setText(stringBuilder);
		stage.draw();
	}
	
	public void setTile(Tile t){
		tile = t;
	}
	
	@Override
	public void dispose(){
	    font.dispose();
	}
}
