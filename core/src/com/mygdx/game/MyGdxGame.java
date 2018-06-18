package com.mygdx.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.utils.Array;

import java.util.Random;

public class MyGdxGame extends ApplicationAdapter {
    public PerspectiveCamera cam;
    public CameraInputController camController;
    public Environment environment;
    public ModelBuilder modelBuilder;
    MeshPartBuilder partBuilder;
    public ModelInstance instance;
    public ModelBatch modelBatch;
    public Model model;
    
    private Ray cursor;
    
    public TileInfoLayer til;
    
    public Array<Layer> layers;
    
    @Override
	public void create () {
        super.create();
        cursor = new Ray();
        layers = new Array<Layer>();
        modelBatch = new ModelBatch();
		
        // Lighting
        environment = new Environment();
       // environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 1f, 1f, 1f, 1f));
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.05f, 0.05f, 0.05f, 0.05f));
        environment.add(new DirectionalLight().set(0.95f, 0.95f, 0.95f, -1f, -0.8f, -0.2f));

        // Camera
	    cam = new PerspectiveCamera(50, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
	    cam.position.set(20f,20f,20f);
	    cam.lookAt(0f,0f,0f);
	    cam.near = 1f;
	    cam.far = 50f;
	    cam.update();

	    // Subdivided icosahedron test
        long startTime = System.currentTimeMillis();
        Planet planet = new Planet();
			    planet.generateIcosphere(10.0f, 1);
        	planet.randomizeTopography();
		
      
        long endTime = System.currentTimeMillis();
        System.out.println("Generation Time: " + (endTime - startTime) + " ms");


        Random r = new Random();                // for colors
        modelBuilder = new ModelBuilder();      // Declare the ModelBuilder
        modelBuilder.begin();                   // LET THE GAMES BEGIN


        partBuilder = modelBuilder.part("tile", GL20.GL_TRIANGLES, VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal, new Material());
        for(int i = 0; i < planet.tiles.size; i++) {
            float red = r.nextFloat();
            float grn = r.nextFloat();
            float blu = r.nextFloat();
            for(int j = 0; j < planet.tiles.get(i).pts.size; j++) {
                int k = j+1;
                if(k == planet.tiles.get(i).pts.size) k = 0;
                partBuilder.setColor(red, grn, blu,1);
                partBuilder.triangle(
                                planet.tiles.get(i).centroid,
                                planet.tiles.get(i).pts.get(j),
                                planet.tiles.get(i).pts.get(k));
            }
        }


        model = modelBuilder.end();         // The model is then assigned

        // Create a new instance of the model
        instance = new ModelInstance(model, 0, 0 ,0);

        camController = new CameraInputController(cam);
        Gdx.input.setInputProcessor(camController);
        
        //Set up our graphics layers

		layers.add(new FrameRateLayer());
		layers.add(til);
		
	}


	@Override
	public void render () {
    	
        camController.update();

        
        
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        
		for (int i = 0; i < layers.size; i++){
			if(layers.get(i).getOn()) layers.get(i).update();
		}
		for (int i = 0; i < layers.size; i++){
			if(layers.get(i).getOn()) layers.get(i).render();
		}
		
		modelBatch.begin(cam);
		modelBatch.render(instance, environment);
        modelBatch.end();
	}

	@Override
	public void dispose () {
		for (int i = 0; i < layers.size; i++){
			layers.get(i).dispose();
		}
        modelBatch.dispose();
        model.dispose();
	}

    @Override
    public void resume () {
    }

    @Override
    public void resize (int width, int height) {
//        float aspectRatio = (float) width / (float) height;
//        cam = new PerspectiveCamera(67, 2f * aspectRatio, 2f);
//        cam.far = 30f;
//        cam.near = 1f;
//        cam.lookAt(0, 0, 0);
    }

    @Override
    public void pause () {
    }
}
