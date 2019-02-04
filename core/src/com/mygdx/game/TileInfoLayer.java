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
	public void render() {
        label.setPosition(0,stage.getViewport().getScreenHeight() - 122); // TODO: magic number
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
                .append("\n Lat: ").append(tile.getLatitude())
                .append("\n Long: ").append(tile.getLongitude())
                .append("\n Elevation: ").append(tile.getElevation_masl()).append(" masl")
                .append("\n\n PlateID: ").append(tile.plateId)
                .append("\n Oceanic: ").append(plate.oceanic)
                .append("\n Angular speed: ").append(plate.angularSpeed_rad_yr).append(" rad/yr")
                .append("\n Density: ").append(plate.density_gm_cm3).append(" gm/cm^3")
                .append("\n Thickness: ").append(plate.thickness_km).append(" km")
                .append("\n Plate members: ").append(plate.members.size)
                .append("\n Plate border: ").append(plate.border.size);
    }
}
