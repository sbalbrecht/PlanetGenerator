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
	    cam.position.set(2f,2f,2f);
	    cam.lookAt(0f,0f,0f);
	    cam.near = 1f;
	    cam.far = 30f;
	    cam.update();

	    // Icosahedron test
        float phi = 1.0f + (float)Math.sqrt(5.0f)/2.0f;
        float u = 1.0f/(float)Math.sqrt(phi*phi + 1.0f);
        float v = phi*u;

        float[] verts = new float[] {
                0.0f,   +u,   +v,
                0.0f,   -u,   +v,
                0.0f,   +u,   -v,
                0.0f,   -u,   -v,
                +u,     +v, 0.0f,
                -u,     +v, 0.0f,
                +u,     -v, 0.0f,
                -u,     -v, 0.0f,
                +v,   0.0f,   +u,
                +v,   0.0f,   -u,
                -v,   0.0f,   +u,
                -v,   0.0f,   -u
        };

	    short[] indices = new short[] {
                0,  1,  8,
                0,  4,  5,
                0,  5, 10,
                0,  8,  4,
                0, 10,  1,
                1,  6,  8,
                1,  7,  6,
                1, 10,  7,
                2,  3, 11,
                2,  4,  9,
                2,  5,  4,
                2,  9,  3,
                2, 11,  5,
                3,  6,  7,
                3,  7, 11,
                3,  9,  6,
                4,  8,  9,
                5, 11, 10,
                6,  9,  8,
                7, 10, 11
        };

	    // Subdivided icosahedron test
        Planet planet = new Planet();
        planet.generateIcosphere(0);

        Random r = new Random();                // for colors
        modelBuilder = new ModelBuilder();      // Declare the ModelBuilder
        modelBuilder.begin();                   // LET THE GAMES BEGIN

        // Every face is added to the model as a meshPart
//        for(int i = 0; i < indices.length; i += 3) {
//            modelBuilder.part("face"+(i/3), GL20.GL_TRIANGLES, VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal,
//                    new Material(ColorAttribute.createDiffuse(r.nextFloat(), r.nextFloat(), r.nextFloat(), 1)))
//                    .triangle(
//                            new Vector3(verts[indices[i  ]*3], verts[indices[i  ]*3+1], verts[indices[i  ]*3+2]),
//                            new Vector3(verts[indices[i+1]*3], verts[indices[i+1]*3+1], verts[indices[i+1]*3+2]),
//                            new Vector3(verts[indices[i+2]*3], verts[indices[i+2]*3+1], verts[indices[i+2]*3+2]));
//        }

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
