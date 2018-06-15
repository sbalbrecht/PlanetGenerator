package com.mygdx.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;

import java.util.Random;

public class MyGdxGame extends ApplicationAdapter {
    public PerspectiveCamera cam;
    public CameraInputController camController;
    public Environment environment;
    public ModelBuilder modelBuilder;
    public ModelInstance instance;
    public ModelBatch modelBatch;
    public Model model;
    public FrameRate fr;

    @Override
	public void create () {
        super.create();
        modelBatch = new ModelBatch();

        // Lighting
        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.8f, 0.8f, 0.8f, 1f));
        environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));

        // Camera
	    cam = new PerspectiveCamera(50, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
	    cam.position.set(20f,20f,20f);
	    cam.lookAt(0f,0f,0f);
	    cam.near = 1f;
	    cam.far = 30f;
	    cam.update();

	    // Subdivided icosahedron test
        Planet planet = new Planet();
        planet.generateIcosphere(5);
        System.out.println(planet.faces.size + " faces");

        Random r = new Random();                // for colors
        modelBuilder = new ModelBuilder();      // Declare the ModelBuilder
        modelBuilder.begin();                   // LET THE GAMES BEGIN

        for(int i = 0; i < planet.faces.size; i++) {
            modelBuilder.part("face"+i, GL20.GL_TRIANGLES, VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal,
                    new Material(ColorAttribute.createDiffuse(r.nextFloat(), r.nextFloat(), r.nextFloat(), 1)))
                    //new Material(ColorAttribute.createDiffuse(Color.GREEN)))
                    .triangle(
                            new Vector3(planet.faces.get(i).pts[0]),
                            new Vector3(planet.faces.get(i).pts[1]),
                            new Vector3(planet.faces.get(i).pts[2]));
        }

        model = modelBuilder.end();         // The model is then assigned

        // Create a new instance of the model
        instance = new ModelInstance(model, 0, 0 ,0);

        camController = new CameraInputController(cam);
        Gdx.input.setInputProcessor(camController);
	}


	@Override
	public void render () {
        camController.update();
        fr = new FrameRate();

        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        modelBatch.begin(cam);
        modelBatch.render(instance, environment);
        fr.update();
        fr.render();
        modelBatch.end();
	}

	@Override
	public void dispose () {
        fr.dispose();
        modelBatch.dispose();
        model.dispose();
	}

    @Override
    public void resume () {
    }

    @Override
    public void resize (int width, int height) {
    }

    @Override
    public void pause () {
    }
}
