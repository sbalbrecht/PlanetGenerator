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
	private Plate plate;
	
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
	public void update() {}

	@Override
	public void render(){
        label.setPosition(0,stage.getViewport().getScreenHeight()-90);
		stringBuilder.setLength(0);
		if (tile != null) {
		    displayTileInfo();
            label.setText(stringBuilder);
            stage.draw();
        }
	}
	
	public void setTile(Tile t){
		tile = t;
	}

	public void setPlate(Plate p) { plate = p; }

	@Override
	public void dispose(){
	    font.dispose();
	}

	private void displayTileInfo() {
	    stringBuilder.append(" Index: ").append(tile.centroid)
                .append("\n Lat:   ").append(tile.getLatitude())
                .append("\n Long: ").append(tile.getLongitude())
                .append("\n Elevation: ").append(tile.getElevation())
                .append("\n PlateID: ").append(tile.plateId)
                .append("\n Oceanic: ").append(plate.oceanic);
    }
}
