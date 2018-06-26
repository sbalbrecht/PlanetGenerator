package com.mygdx.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.FloatAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.mygdx.game.util.Log;

import java.util.Random;

public class MyGdxGame extends ApplicationAdapter {
    private PerspectiveCamera cam;
    private CameraInputController camController;
    private Environment environment;
    private ModelBuilder modelBuilder;
    private MeshPartBuilder partBuilder;
    private ModelInstance instance;
    private ModelBatch modelBatch;
    private Model model;
    private Viewport viewport;
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
//        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 1f, 1f, 1f, 1f));
//        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.15f, 0.15f, 0.15f, 0.15f));
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.2f, 0.2f, 0.2f, 0.2f));
        environment.add(new DirectionalLight().set(0.95f, 0.95f, 0.95f, -1, 0, 0));

        // Camera
        float planetRadius = 10;

	    cam = new PerspectiveCamera(50, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        viewport = new ScreenViewport(cam);
	    cam.position.set(2*planetRadius,2*planetRadius,2*planetRadius);
	    cam.lookAt(0f,0f,0f);
	    cam.near = 0.1f;
	    cam.far = 50000000.0f;
	    cam.update();

	    // Subdivided icosahedron test
        Log l = new Log();

        l.start("Generation time");
        Planet planet = new Planet();
			  planet.generateIcosphere(new Vector3(0, 0, 0), planetRadius, 6);
        	//planet.randomizeTopography();
		l.end();

        
        modelBuilder = new ModelBuilder();
        modelBuilder.begin();

        /* Render tiles */
        l.start("Build time");
        partBuilder = modelBuilder.part("tile", GL20.GL_TRIANGLES, VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal | VertexAttributes.Usage.ColorPacked, new Material());
        int q = 0;
        int tileLimit = 1000;

		{Tile t;
        for(int i = 0; i < planet.tiles.size; i++) {

        	t = planet.tiles.get(i);

			if(i % tileLimit == 0){
				partBuilder = modelBuilder.part("tile" + i, GL20.GL_TRIANGLES, VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal | VertexAttributes.Usage.ColorPacked, new Material());
				q++;
			}

			int plateId = t.plateId;
            partBuilder.setColor(planet.plates.get(plateId).color);

            int numPts = t.pts.size;
            if (numPts == 6) {
                partBuilder.rect(
                        t.pts.get(0),
                        t.pts.get(1),
                        t.pts.get(3),
                        t.pts.get(4),
                        planet.points.get(t.centroid)
                );
                partBuilder.triangle(
                        t.pts.get(4),
                        t.pts.get(5),
                        t.pts.get(0)
                );
                partBuilder.triangle(
                        t.pts.get(1),
                        t.pts.get(2),
                        t.pts.get(3)
                );
            } else {
                for (int j = 0; j < numPts; j++) {
                    int k = j + 1;
                    if (k == numPts) k = 0;
                    partBuilder.triangle(
                            planet.points.get(t.centroid),
                            t.pts.get(j),
                            t.pts.get(k));
                }
            }
        }
		}


        /* Render sun rays */
//		Material lineColor = new Material(ColorAttribute.createDiffuse(Color.valueOf("ffffff")));
//
//		Vector3 p1;
//		partBuilder = modelBuilder.part("tile", GL20.GL_LINES, VertexAttributes.Usage.Position, lineColor);
//        for(int i = 0; i < planet.tiles.size; i++) {
//            if(i % tileLimit == 0){
//                partBuilder = modelBuilder.part("tile", GL20.GL_LINES, VertexAttributes.Usage.Position, lineColor);
//            }
//                p1 = planet.tiles.get(i).centroid;
//				partBuilder.line(p1.scl(1.0f), p1.cpy().scl(1.0f + 0.00000000000000125f*planet.tiles.get(i).power.getValue()));
//        }

        /* Render wireframe */
//        Material lineColor = new Material(ColorAttribute.createDiffuse(Color.valueOf("202020")));
//        partBuilder = modelBuilder.part("tile", GL20.GL_LINES, VertexAttributes.Usage.Position, lineColor);
//        Vector3 p1;
//        Vector3 p2;
//        for(int i = 0; i < planet.tiles.size; i++) {
//            if(i % tileLimit == 0){
//                partBuilder = modelBuilder.part("tile", GL20.GL_LINES, VertexAttributes.Usage.Position, lineColor);
//            }
//            int numPts = planet.tiles.get(i).pts.size;
//            for (int j = 0; j < numPts; j++) {
//                int k = j + 1;
//                if (k == numPts) k = 0;
//                p1 = planet.tiles.get(i).pts.get(j).cpy();
//                p2 = planet.tiles.get(i).pts.get(k).cpy();
//                if(planet.tiles.get(i).getNbr(p1, p2).drawn)
//                    continue;
//                else
//                    partBuilder.line(p1.scl(1.000008f), p2.scl(1.000008f));
//            }
//            planet.tiles.get(i).drawn = true;
//        }

        model = modelBuilder.end();
        instance = new ModelInstance(model, planet.position);
        l.end();

        camController = new CameraInputController(cam);
        Gdx.input.setInputProcessor(camController);
        
        //Set up our graphics layers

		layers.add(new FrameRateLayer());
		//layers.add(til);
		
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
        viewport.update(width, height);
        for (int i = 0; i < layers.size; i++){
            if(layers.get(i).getOn()) layers.get(i).resize(width, height);
        }
    }

    @Override
    public void pause () {
    }
}
