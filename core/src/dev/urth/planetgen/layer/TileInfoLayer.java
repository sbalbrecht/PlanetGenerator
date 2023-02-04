package dev.urth.planetgen.layer;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import dev.urth.planetgen.Plate;
import dev.urth.planetgen.Tile;

public class TileInfoLayer extends Layer implements Disposable {

    private final BitmapFont font;
    private final Label label;
    private final Stage stage;
    private Tile tile;
    private Plate plate;

    public TileInfoLayer() {
        super("Tile info", true);
        font = new BitmapFont();
        label = new Label(" ", new Label.LabelStyle(font, Color.WHITE));
        stage = new Stage();
        stage.setViewport(new ScreenViewport());
        stage.addActor(label);
    }

    @Override
    public void resize(int screenWidth, int screenHeight) {
        stage.getViewport().update(screenWidth, screenHeight, true);
    }

    @Override
    public void update() {}

    @Override
    public void render() {
        if (tile != null) {
            int numLines = 11;
            float offset = numLines * font.getCapHeight();
            label.setPosition(0, stage.getViewport().getScreenHeight() - offset);
            label.setText(getTileInfoOutput());
            stage.draw();
        }
    }

    public void setTile(Tile t) {
        tile = t;
    }

    public void setPlate(Plate p) {
        plate = p;
    }

    @Override
    public void dispose() {
        font.dispose();
    }

    private String getTileInfoOutput() {
        return " Index: "
                + tile.getCentroid()
                + "\n Lat: "
                + tile.getLatitude()
                + "\n Long: "
                + tile.getLongitude()
                + "\n Elevation: "
                + tile.getElevationMasl()
                + " masl"
                + "\n\n PlateID: "
                + tile.getPlateId()
                + "\n Oceanic: "
                + plate.isOceanic()
                + "\n Angular speed: "
                + plate.getAngularSpeedRadPerYr()
                + " rad/yr"
                + "\n Density: "
                + plate.getDensityGmPerCm3()
                + " gm/cm^3"
                + "\n Thickness: "
                + plate.getThicknessKm()
                + " km"
                + "\n Plate members: "
                + plate.getMembers().size
                + "\n Plate border: "
                + plate.getBorder().size;
    }
}
