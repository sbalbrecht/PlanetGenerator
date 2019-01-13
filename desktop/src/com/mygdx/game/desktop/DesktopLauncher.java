package com.mygdx.game.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.mygdx.game.PlanetGenerator;

public class DesktopLauncher {
	public static void main (String[] arg) {
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		config.title = "PlanetGenerator";
		config.height = 720;
		config.width = 960;
		config.samples = 16;
		new LwjglApplication(new PlanetGenerator(), config);
	}
}
