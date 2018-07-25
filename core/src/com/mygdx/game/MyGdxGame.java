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
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.EllipseShapeBuilder;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.mygdx.game.util.Log;

import java.util.Random;

import static com.mygdx.game.util.ColorUtils.getComplementary;

public class MyGdxGame extends ApplicationAdapter {
    private PerspectiveCamera cam;
    private CameraInterface cameraInterface;
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

    private int TILE_LIMIT = 1000;
    private float PLANET_RADIUS = 10;

    @Override
	public void create () {
        super.create();
        cursor = new Ray();
        layers = new Array<Layer>();
        modelBatch = new ModelBatch();
		
        // Lighting
        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 1f, 1f, 1f, 1f));
//        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.2f, 0.2f, 0.2f, 0.2f));
//        environment.add(new DirectionalLight().set(0.95f, 0.95f, 0.95f, -1, 0, 0));

        // Camera
	    cam = new PerspectiveCamera(50, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        viewport = new ScreenViewport(cam);
	    cam.position.set(2*PLANET_RADIUS,2*PLANET_RADIUS,2*PLANET_RADIUS);
	    cam.lookAt(0f,0f,0f);
	    cam.near = 0.1f;
	    cam.far = 50000000.0f;
	    cam.update();

	    
	    
	    // Subdivided icosahedron test
        Log log = new Log();

        log.start("Generation time");
        Planet planet = new Planet(new Vector3(0, 0, 0), PLANET_RADIUS, 6);
        
        modelBuilder = new ModelBuilder();
        modelBuilder.begin();
    
        cameraInterface = new CameraInterface(cam);
        cameraInterface.center = planet.position;

        /* Build model */
        log.start("Build time");
        partBuilder = modelBuilder.part("tile", GL20.GL_TRIANGLES,
                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal |
                         VertexAttributes.Usage.ColorPacked, new Material());


//        buildIcosahedron(planet);         // Triangles
		buildTruncatedIcosahedron(planet);  // Tiles
//        buildSunRays(planet);
//        buildWireframe(planet);
//        buildAxes();
        buildPlateDirectionArrows(planet);
        buildMajorLatLines(planet);
//        buildLatLongSpikes(planet);
    
        model = modelBuilder.end();
        instance = new ModelInstance(model, planet.position);
        log.end();

		layers.add(new FrameRateLayer());
		//layers.add(til);

	}

	@Override
	public void render () {
    	
        cameraInterface.update(Gdx.graphics.getDeltaTime());

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


    private void buildIcosahedron(Planet planet) {
        Random r = new Random();
        Face f;
        for(int i = 0; i < planet.faces.size; i++) {
            float red = r.nextFloat();
            float grn = r.nextFloat();
            float blu = r.nextFloat();
            f = planet.faces.get(i);

            if(i % TILE_LIMIT == 0){
                partBuilder = modelBuilder.part("tile" + i, GL20.GL_TRIANGLES,
                        VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal |
                                 VertexAttributes.Usage.ColorPacked, new Material());
            }
            partBuilder.setColor(red, grn, blu, 1.0f);
            partBuilder.triangle(
                    planet.points.get(f.pts[0]),
                    planet.points.get(f.pts[1]),
                    planet.points.get(f.pts[2]));
        }
    }

    private void buildTruncatedIcosahedron(Planet planet) {
        Tile t;
        for(int i = 0; i < planet.tiles.size; i++) {

            t = planet.tiles.get(i);

            if(i % TILE_LIMIT == 0){
                partBuilder = modelBuilder.part("tile" + i, GL20.GL_TRIANGLES,
                        VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal |
                                VertexAttributes.Usage.ColorPacked, new Material());
            }

            int plateId = t.plateId;
            partBuilder.setColor(planet.plates.get(plateId).color);

            int numPts = t.pts.size;
            if (numPts == 6) {
//                partBuilder.rect(
//                        planet.points.get(t.pts.get(0)),
//                        planet.points.get(t.pts.get(1)),
//                        planet.points.get(t.pts.get(3)),
//                        planet.points.get(t.pts.get(4)),
//                        planet.points.get(t.centroid)
//                );
                partBuilder.triangle(
                        planet.points.get(t.pts.get(0)),
                        planet.points.get(t.pts.get(1)),
                        planet.points.get(t.pts.get(3))
                );
                partBuilder.triangle(
                        planet.points.get(t.pts.get(3)),
                        planet.points.get(t.pts.get(4)),
                        planet.points.get(t.pts.get(0))
                );
                partBuilder.triangle(
                        planet.points.get(t.pts.get(4)),
                        planet.points.get(t.pts.get(5)),
                        planet.points.get(t.pts.get(0))
                );
                partBuilder.triangle(
                        planet.points.get(t.pts.get(1)),
                        planet.points.get(t.pts.get(2)),
                        planet.points.get(t.pts.get(3))
                );
            } else {
                for (int j = 0; j < numPts; j++) {
                    partBuilder.triangle(
                            planet.points.get(t.centroid),
                            planet.points.get(t.pts.get(j)),
                            planet.points.get(t.pts.get((j + 1) % numPts)));
                }
            }
        }
    }

    private void buildSunRays(Planet planet) {
        Material lineColor = new Material(ColorAttribute.createDiffuse(Color.valueOf("ffffff")));
		Vector3 p1;
		partBuilder = modelBuilder.part("tile", GL20.GL_LINES, VertexAttributes.Usage.Position, lineColor);
        for(int i = 0; i < planet.tiles.size; i++) {
            if(i % TILE_LIMIT == 0){
                partBuilder = modelBuilder.part("tile", GL20.GL_LINES, VertexAttributes.Usage.Position, lineColor);
            }
                p1 = planet.points.get(planet.tiles.get(i).centroid);
				partBuilder.line(p1.scl(1.0f), p1.cpy().scl(1.0f + 0.00000000000000125f*planet.tiles.get(i).getPower()));
        }
    }

    private void buildWireframe(Planet planet) {
        Material lineColor = new Material(ColorAttribute.createDiffuse(Color.valueOf("101010")));
        partBuilder = modelBuilder.part("tile", GL20.GL_LINES, VertexAttributes.Usage.Position, lineColor);
        Vector3 p1;
        Vector3 p2;
        Tile t;
        for(int i = 0; i < planet.tiles.size; i++) {
            if(i % TILE_LIMIT == 0){
                partBuilder = modelBuilder.part("tile", GL20.GL_LINES, VertexAttributes.Usage.Position, lineColor);
            }
            t = planet.tiles.get(i);
            int numPts = t.pts.size;
            for (int j = 0; j < numPts; j++) {
                int k = (j + 1) % numPts;
                p1 = planet.points.get(t.pts.get(j)).cpy();
                p2 = planet.points.get(t.pts.get(k)).cpy();
                if(!t.getNbr(t.pts.get(j), t.pts.get(k)).drawn)
                    partBuilder.line(p1.scl(1.000008f), p2.scl(1.000008f));
            }
            planet.tiles.get(i).drawn = true;
        }
    }

    private void buildAxes() {
        partBuilder = modelBuilder.part("axes", GL20.GL_LINES, VertexAttributes.Usage.Position, new Material());
        partBuilder.line(0,0,0,15f,0,0);
        partBuilder.line(0,0,0, 0,20f,0);
        partBuilder.line(0,0,0,0,0,25f);
    }
    
    private void buildMajorLatLines(Planet planet) {
        Material lineColor;
        double tropicLatitude_rad = 23.5*Math.PI/180;
        float tropicHeight = (float)Math.sin(tropicLatitude_rad)*PLANET_RADIUS;
        float tropicRadius = (float)Math.cos(tropicLatitude_rad)*PLANET_RADIUS;
        double arcticLatitude_rad = 66.5*Math.PI/180;
        float arcticHeight = (float)Math.sin(arcticLatitude_rad)*PLANET_RADIUS;
        float arcticRadius = (float)Math.cos(arcticLatitude_rad)*PLANET_RADIUS;

        lineColor = new Material(ColorAttribute.createDiffuse(Color.RED));
        partBuilder = modelBuilder.part("equator", GL20.GL_LINES, VertexAttributes.Usage.Position, lineColor);
        EllipseShapeBuilder.build(partBuilder, PLANET_RADIUS*1.000010f, 480, new Vector3(0f,0f,0f),new Vector3(0f,1f,0f));

        lineColor = new Material(ColorAttribute.createDiffuse(Color.ORANGE));
        partBuilder = modelBuilder.part("tropics", GL20.GL_LINES, VertexAttributes.Usage.Position, lineColor);
        EllipseShapeBuilder.build(partBuilder, tropicRadius*1.000010f, 480, new Vector3(0f,tropicHeight, 0f),new Vector3(0f,1f,0f));
        EllipseShapeBuilder.build(partBuilder, tropicRadius*1.000010f, 480, new Vector3(0f,-tropicHeight, 0f),new Vector3(0f,1f,0f));

        lineColor = new Material(ColorAttribute.createDiffuse(Color.CYAN));
        partBuilder = modelBuilder.part("polarCircles", GL20.GL_LINES, VertexAttributes.Usage.Position, lineColor);
        EllipseShapeBuilder.build(partBuilder, arcticRadius*1.000010f, 480, new Vector3(0f, arcticHeight,0f),new Vector3(0f,1f,0f));
        EllipseShapeBuilder.build(partBuilder, arcticRadius*1.000010f, 480, new Vector3(0f, -arcticHeight, 0f),new Vector3(0f,1f,0f));
    }

    private void buildLatLongSpikes(Planet planet) {
        Material lineColor = new Material(ColorAttribute.createDiffuse(Color.valueOf("ffffff")));
        partBuilder = modelBuilder.part("tile", GL20.GL_LINES, VertexAttributes.Usage.Position, lineColor);

        float pi = MathUtils.PI;
        Vector3 p1, p2;
        Log l2 = new Log();
        l2.start("Generate pickCircles");
        for (int i = 0; i < 32; i++){
            for (int j = 0; j < 4; j++){
                p1 = planet.points.get(planet.getNearestLatLong((i*(pi/16.0f))-pi, j*pi/4.0f).centroid);
                p2 = p1.cpy().scl(1.5f);
                partBuilder.line(p1, p2);
            }

        }l2.end();
    }
    private void buildPlateDirectionArrows(Planet planet) {
        int i = 0;
        for(Plate plate : planet.plates.values()) {
            partBuilder = modelBuilder.part("arrow" + i, GL20.GL_TRIANGLES,
                    VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal |
                            VertexAttributes.Usage.ColorPacked, new Material());
            partBuilder.setColor(getComplementary(plate.color));
            for(Tile tile : plate.members) {
                Vector3[] arrowVertices = getArrowVertices(planet, tile, tile.tangentialVelocity);
                if(i % TILE_LIMIT == 0){
                    partBuilder = modelBuilder.part("arrow" + i, GL20.GL_TRIANGLES,
                            VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal |
                                    VertexAttributes.Usage.ColorPacked, new Material());
                    partBuilder.setColor(getComplementary(plate.color));
                }
                partBuilder.triangle(
                        arrowVertices[0],
                        arrowVertices[1],
                        arrowVertices[2]);
                i++;
            }
        }
    }

    private Vector3[] getArrowVertices(Planet planet, Tile t, Vector3 direction) {
        final float HEIGHT_SCALE = (float)(764428*Math.exp(-0.653*planet.subdivisions));
        float baseWidthHalf;
        if(t.pts.size == 6) {
            baseWidthHalf = planet.points.get(t.pts.get(0)).dst(planet.points.get(t.pts.get(3)))*0.1f;
        } else {
            Vector3 u = planet.points.get(t.pts.get(2));
            Vector3 v = planet.points.get(t.pts.get(3));
            Vector3 w = u.cpy().add(v).scl(0.5f);
            baseWidthHalf = planet.points.get(t.pts.get(0)).dst(w)*0.1f;
        }
        Vector3 base1 = planet.points.get(t.centroid).cpy().add(direction.cpy().crs(planet.points.get(t.centroid)).nor().scl(-baseWidthHalf));
        Vector3 base2 = planet.points.get(t.centroid).cpy().add(direction.cpy().crs(planet.points.get(t.centroid)).nor().scl( baseWidthHalf));
        Vector3 heigt = planet.points.get(t.centroid).cpy().add(direction.cpy().scl(HEIGHT_SCALE));
        return new Vector3[] {base1, base2, heigt};
    }
}
