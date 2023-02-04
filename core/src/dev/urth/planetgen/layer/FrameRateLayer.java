package dev.urth.planetgen.layer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

public class FrameRateLayer extends Layer implements Disposable {

    protected final Stage stage;
    protected final Label label;
    protected final BitmapFont font;

    public FrameRateLayer() {
        super("Frame Rate", true);
        stage = new Stage();
        stage.setViewport(new ScreenViewport());
        font = new BitmapFont();
        label = new Label(" ", new Label.LabelStyle(font, Color.WHITE));
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
        label.setText("FPS: " + Gdx.graphics.getFramesPerSecond());
        stage.draw();
    }

    @Override
    public void dispose() {
        font.dispose();
    }
}
