package com.mygdx.game;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.EllipseShapeBuilder;
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.SphereShapeBuilder;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.mygdx.game.util.Log;
import com.mygdx.game.util.Units;
import com.mygdx.game.util.VMath;
import static com.mygdx.game.util.VMath.*;

import static org.jocl.CL.*;
import org.jocl.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.*;
import java.sql.SQLClientInfoException;
import java.util.Map;
import java.util.Random;

import static com.mygdx.game.util.ColorUtils.getComplementary;

public class PlanetGenerator extends InputAdapter implements ApplicationListener {
    private CameraInterface      cameraInterface;   // For custom movement controls
    private Environment          environment;       // For lighting
    private MeshPartBuilder      partBuilder;       // For building models
    private Model                model;             // For building models
    private ModelBatch           modelBatch;        // For rendering models
    private ModelBuilder         modelBuilder;      // For building models
    private ModelInstance        instance;          // For rendering models
    private PerspectiveCamera    cam;
    private Planet               planet;
    private Viewport             viewport;          // For proper screen resizing

    public  TileInfoLayer        til;               // For displaying tile / plate info
    
    public  Array<Layer>         layers;
    private Array<Model>         models         = new Array<Model>();
    private Array<ModelInstance> modelInstances = new Array<ModelInstance>();
    private Array<ModelInstance> airInstances   = new Array<ModelInstance>();


    private cl_program cpParticle;
    private cl_kernel kernel;
    private Pointer pwx;
    private Pointer pwy;
    private Pointer pwz;
    private Pointer pwt;
    private Pointer pwe;
    private Pointer pua;
    private Pointer pf;

    private cl_context context;
    private cl_command_queue commandQueue;
    private cl_mem memObjects[];

    private long global_work_size[];
    private long local_work_size[];

    private float[] wx;
    private float[] wy;
    private float[] wz;
    private float[] wt;
    private float[] we;
    private int[]   ua;
    private int[] frame = {0};

    private final int   AIR_PARTICLES = 7500;
    private final int   TILE_LIMIT = 1000;
    private final int   SUBDIVSIONS = 5;
    private final float PLANET_RADIUS = 10;

    @Override
	public void create () {
        layers = new Array<Layer>();
        modelBatch = new ModelBatch();
		
        /* Set lighting */
        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 1f, 1f, 1f, 1f));
//        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.2f, 0.2f, 0.2f, 0.2f));
//        environment.add(new DirectionalLight().set(0.95f, 0.95f, 0.95f, -1, 0, 0));

        /* Set camera */
	    cam = new PerspectiveCamera(50, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        float camPosMultiplier = 1.6f;
        float camCoordinate = camPosMultiplier * PLANET_RADIUS;
	    cam.position.set(camCoordinate,camCoordinate,camCoordinate);
	    cam.lookAt(0f,0f,0f);
	    cam.near = 1f;
	    cam.far = 50000.0f;
	    cam.update();
	    viewport = new ScreenViewport(cam);

        Log log = new Log();

        /* Generate planet */
        log.start("Generation time");
        planet = new Planet(new Vector3(0, 0, 0), PLANET_RADIUS, SUBDIVSIONS);

        cameraInterface = new CameraInterface(cam);
        cameraInterface.center = planet.position;
	    Gdx.input.setInputProcessor(new InputMultiplexer(this, cameraInterface));

        /* Build models */
        log.start("Build time");

        modelBuilder = new ModelBuilder();

//        models.add(buildIcosahedron(planet));         // Triangles
		models.add(buildTruncatedIcosahedron(planet));  // Tiles
//        models.add(buildSunRays(planet));
//        models.add(buildWireframe(planet));
        models.add(buildAxes());
//        models.add(buildPlateDirectionArrows(planet));
        models.add(buildMajorLatLines(planet));
        models.add(buildPlateCollisions(planet));
//        models.add(buildLatLongSpikes(planet));

        final float partRadius = 0.05f;
        final int partDivs = 10;
        Model airParticle = modelBuilder.createSphere(partRadius, partRadius, partRadius, partDivs, partDivs,
                new Material(ColorAttribute.createDiffuse(Color.WHITE)),
                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);


//        for (AirParticle particle : planet.wind) {
//            Vector3 v = particle.getPosition().cpy().scl(1.001f);
//            ModelInstance airInstance = new ModelInstance(airParticle);
//            airInstance.transform.setToTranslation(v);
//            airInstances.add(airInstance);
//        }
        initAirParticles();

        for (int i = 0; i < AIR_PARTICLES; i++) {
//            Vector3 v = new Vector3(wx[i], wy[i], wz[i]).scl(1.001f);
            Vector3 v = new Vector3(wx[i], wy[i], wz[i]);
            ModelInstance airInstance = new ModelInstance(airParticle);
            airInstance.transform.setToTranslation(v);
            airInstances.add(airInstance);
        }

        initGPU();

        for (Model model : models) {
            modelInstances.add(new ModelInstance(model, planet.position));
        }
        log.end();

        /* Set up additional layers */
		til = new TileInfoLayer();

		layers.add(til);
		layers.add(new FrameRateLayer());
	}

	@Override
	public void render () {
        cameraInterface.update(Gdx.graphics.getDeltaTime());

        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

//        if (frame == 0) {
            updateAirParticles();
//        }

        frame[0]++;
        if (frame[0] == 3600) frame[0] = 0;

		modelBatch.begin(cam);
        for (ModelInstance instance : modelInstances) {
            modelBatch.render(instance, environment);
        }
        for (ModelInstance instance : airInstances) {
            modelBatch.render(instance, environment);
        }
        modelBatch.end();

        for (int i = 0; i < layers.size; i++){
            Layer layer = layers.get(i);
			if(layer.isActive()) {
                layer.update();
                layer.render();
            }
		}
	}

	@Override
	public void dispose () {
		for (int i = 0; i < layers.size; i++){
			layers.get(i).dispose();
		}
        modelBatch.dispose();
		modelInstances.clear();
        for(Model model : models) {
		    model.dispose();
        }

        // Release kernel, program, and memory objects
        for (cl_mem memObject : memObjects) {
            clReleaseMemObject(memObject);
        }
        clReleaseKernel(kernel);
        clReleaseProgram(cpParticle);
        clReleaseCommandQueue(commandQueue);
        clReleaseContext(context);
	}

    @Override
    public void resume () {
    }

    @Override
    public void resize (int width, int height) {
        viewport.update(width, height);
        for (int i = 0; i < layers.size; i++){
            if(layers.get(i).isActive()) layers.get(i).resize(width, height);
        }
    }

    @Override
    public void pause () {
    }

    public boolean mouseMoved (int screenX, int screenY) {
        updateTileInfoLayer(screenX, screenY);
        return false;
    }

    private void updateTileInfoLayer(int screenX, int screenY) {
        Vector3 intersection = getMouseIntersection(screenX, screenY);
        if (intersection != null) {
            float longitude = VMath.cartesianToLongitude(intersection);
            float latitude = VMath.cartesianToLatitude(intersection, PLANET_RADIUS);

            Tile t = planet.getNearestTile(latitude, longitude);

            til.setTile(t);
            til.setPlate(planet.plates.get(t.plateId));
        } else {
            til.setTile(null);
            til.setPlate(null);
        }
    }

    private Vector3 getMouseIntersection(int screenX, int screenY) {
        Vector3 position = new Vector3(0,0,0);
        Vector3 intersection = new Vector3();

        Ray ray = cam.getPickRay(screenX, screenY);

        return (Intersector.intersectRaySphere(ray, position, PLANET_RADIUS, intersection))
                ? intersection : null;
    }

    private Model buildIcosahedron(Planet planet) {
        modelBuilder.begin();
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
        return modelBuilder.end();
    }

    private Model buildTruncatedIcosahedron(Planet planet) {
        modelBuilder.begin();
        Tile t;
        for(int i = 0; i < planet.tiles.size; i++) {

            t = planet.tiles.get(i);

            if(i % TILE_LIMIT == 0){
                partBuilder = modelBuilder.part("tile" + i, GL20.GL_TRIANGLES,
                        VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal |
                                VertexAttributes.Usage.ColorPacked, new Material());
            }

            int plateId = t.plateId;
            partBuilder.setColor(planet.plates.get(plateId).color);                  // color by plate
//            partBuilder.setColor(getElevationColor(planet, t.getElevation_masl()));  // color by relative elevation
//            if (t.isRoot) partBuilder.setColor(Color.BLUE);                          // color plate roots blue

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
        return modelBuilder.end();
    }

    private Model buildSunRays(Planet planet) {
        modelBuilder.begin();
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
        return modelBuilder.end();
    }

    private Model buildWireframe(Planet planet) {
        modelBuilder.begin();
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
                if(!t.getNbr(t.pts.get(j), t.pts.get(k)).isDrawn)
                    partBuilder.line(p1.scl(1.000008f), p2.scl(1.000008f));
            }
            planet.tiles.get(i).isDrawn = true;
        }
        return modelBuilder.end();
    }

    private Model buildAxes() {
        modelBuilder.begin();
        partBuilder = modelBuilder.part("axes", GL20.GL_LINES, VertexAttributes.Usage.Position, new Material(ColorAttribute.createDiffuse(Color.RED)));
        partBuilder.line(0,0,0,15f,0,0);
        partBuilder = modelBuilder.part("axes", GL20.GL_LINES, VertexAttributes.Usage.Position, new Material(ColorAttribute.createDiffuse(Color.BLUE)));
        partBuilder.line(0,0,0,0,15f,0);
        partBuilder = modelBuilder.part("axes", GL20.GL_LINES, VertexAttributes.Usage.Position, new Material(ColorAttribute.createDiffuse(Color.GREEN)));
        partBuilder.line(0,0,0,0,0,15f);
        return modelBuilder.end();
    }
    
    private Model buildMajorLatLines(Planet planet) {
        modelBuilder.begin();
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

        lineColor = new Material(ColorAttribute.createDiffuse(Color.BLUE));
        partBuilder = modelBuilder.part("polarCircles", GL20.GL_LINES, VertexAttributes.Usage.Position, lineColor);
        EllipseShapeBuilder.build(partBuilder, arcticRadius*1.000010f, 480, new Vector3(0f, arcticHeight,0f),new Vector3(0f,1f,0f));
        EllipseShapeBuilder.build(partBuilder, arcticRadius*1.000010f, 480, new Vector3(0f, -arcticHeight, 0f),new Vector3(0f,1f,0f));
        return modelBuilder.end();
    }

    private Model buildLatLongSpikes(Planet planet) {
        modelBuilder.begin();
        Material lineColor = new Material(ColorAttribute.createDiffuse(Color.valueOf("ffffff")));
        partBuilder = modelBuilder.part("tile", GL20.GL_LINES, VertexAttributes.Usage.Position, lineColor);

        float pi = MathUtils.PI;
        Vector3 p1, p2;
        Log l2 = new Log();
        l2.start("Generate pickCircles");
        for (int i = 0; i < 32; i++){
            for (int j = 0; j < 4; j++){
                p1 = planet.points.get(planet.getNearestTile((i*(pi/16.0f))-pi, j*pi/4.0f).centroid);
                p2 = p1.cpy().scl(1.5f);
                partBuilder.line(p1, p2);
            }

        }
        l2.end();
        return modelBuilder.end();
    }

    private Model buildWindLines(Planet planet) {
        // TODO: Create 1 line model and create transformed instances

        Log l = new Log();
        l.start("Build wind lines");
        modelBuilder.begin();
        Material lineColor = new Material(ColorAttribute.createDiffuse(Color.VIOLET));
        partBuilder = modelBuilder.part("wind", GL20.GL_LINES, VertexAttributes.Usage.Position, lineColor);

        SphereShapeBuilder.build(partBuilder, 0.5f, 0.5f, 0.5f, 60, 60);
//
//        Vector3 p1 = new Vector3(0,0,0);
//        for (int i = 0; i < planet.wind.size; i++) {
//            partBuilder.line(p1, planet.wind.get(i).getPosition().cpy().scl(1.001f));
//        }

        l.end();

        return modelBuilder.end();
    }


//    int CPUtotal = 0;
//    int GPUtotal = 0;
    private void updateAirParticles() {
//        long CPUdt = System.currentTimeMillis();
//        for (int i = 0; i < AIR_PARTICLES; i++) {
//            planet.wind[i].update();
//        }
//        CPUtotal += System.currentTimeMillis() - CPUdt;
//
//        long GPUdt = System.currentTimeMillis();

        clSetKernelArg(kernel, 6, Sizeof.cl_int, Pointer.to(new int[]{ frame[0] }));

        // Execute the kernel
        clEnqueueNDRangeKernel(commandQueue, kernel, 1, null,
                global_work_size, local_work_size, 0, null, null);

        // Read back results
        clEnqueueReadBuffer(commandQueue, memObjects[0], CL_TRUE, 0,
                AIR_PARTICLES * Sizeof.cl_float, pwx, 0, null, null);
        clEnqueueReadBuffer(commandQueue, memObjects[1], CL_TRUE, 0,
                AIR_PARTICLES * Sizeof.cl_float, pwy, 0, null, null);
        clEnqueueReadBuffer(commandQueue, memObjects[2], CL_TRUE, 0,
                AIR_PARTICLES * Sizeof.cl_float, pwz, 0, null, null);
        clEnqueueReadBuffer(commandQueue, memObjects[3], CL_TRUE, 0,
                AIR_PARTICLES * Sizeof.cl_float, pwt, 0, null, null);
        clEnqueueReadBuffer(commandQueue, memObjects[4], CL_TRUE, 0,
                AIR_PARTICLES * Sizeof.cl_float, pwe, 0, null, null);
        clEnqueueReadBuffer(commandQueue, memObjects[5], CL_TRUE, 0,
                AIR_PARTICLES * Sizeof.cl_int, pua, 0, null, null);

        System.out.println("After:  " + frame[0]);

//        GPUtotal += System.currentTimeMillis() - GPUdt;
//
//        if (frame == 599) {
//            System.out.printf("Avg CPU time: %.2f\n", CPUtotal / 600f);
//            System.out.printf("Avg GPU time: %.2f\n", GPUtotal / 600f);
//            CPUtotal = 0;
//            GPUtotal = 0;
//        }

        for (int i = 0; i < AIR_PARTICLES; i++) {
            airInstances.get(i).transform.setToTranslation(wx[i], wy[i], wz[i]);
        }
    }

    private Model buildPlateDirectionArrows(Planet planet) {
        modelBuilder.begin();
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
        return modelBuilder.end();
    }

    private Vector3[] getArrowVertices(Planet planet, Tile t, Vector3 direction) {
        float arrowHeight = (float)
                ((764428 / (planet.plateCollisionTimeStepInMillionsOfYears * Units.CM_YR_TO_M_MA))
                        * Math.exp(-0.653 * planet.numSubdivisions));
        float baseWidthHalf;
        if(t.pts.size == 6) {
            baseWidthHalf = planet.points.get(t.pts.get(0)).dst(planet.points.get(t.pts.get(3)))*0.1f;
        } else {
            Vector3 u = planet.points.get(t.pts.get(2));
            Vector3 v = planet.points.get(t.pts.get(3));
            Vector3 w = u.cpy().add(v).scl(0.5f);
            baseWidthHalf = planet.points.get(t.pts.get(0)).dst(w)*0.1f;
        }
        Vector3 tileCentroid = planet.points.get(t.centroid);
        Vector3 base1 = tileCentroid.cpy().add(direction.cpy().crs(tileCentroid).nor().scl(-baseWidthHalf));
        Vector3 base2 = tileCentroid.cpy().add(direction.cpy().crs(tileCentroid).nor().scl( baseWidthHalf));
        Vector3 height = tileCentroid.cpy().add(direction.cpy().scl(arrowHeight));
        return new Vector3[] {base1, base2, height};
    }

    private Model buildPlateCollisions(Planet planet) {
        modelBuilder.begin();
        buildPlateCollisionGradient(planet);
        buildPlateCollisionLines(planet);
        return modelBuilder.end();
    }

    private void buildPlateCollisionGradient(Planet planet) {
        Long key;
        int[] edge;
        Vector3[] rectangleVertices = new Vector3[5];
        int i = 0;
        partBuilder = modelBuilder.part("edge" + i++, GL20.GL_TRIANGLES,
                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal |
                        VertexAttributes.Usage.ColorPacked, new Material());
        for(Map.Entry<Long, Float> entry : planet.tileCollisions.entrySet()) {
            key = entry.getKey();
            edge = planet.getIndicesFromHashkey(key);
            rectangleVertices = getCollisionRectangleVertices(planet, edge);
            if(i % TILE_LIMIT == 0){
                partBuilder = modelBuilder.part("edge" + i++, GL20.GL_TRIANGLES,
                        VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal |
                                VertexAttributes.Usage.ColorPacked, new Material());

            }
            partBuilder.setColor(getCollisionColor(planet, entry.getValue()));
            partBuilder.rect(
                    rectangleVertices[0],
                    rectangleVertices[1],
                    rectangleVertices[2],
                    rectangleVertices[3],
                    rectangleVertices[4]);
            i++;
        }
    }

    private void buildPlateCollisionLines(Planet planet) {
        Long key;
        int[] edge;
        Vector3[] rectangleVertices = new Vector3[5];
        int i = 0;
        Material lineColor = new Material(ColorAttribute.createDiffuse(Color.valueOf("101010")));
        partBuilder = modelBuilder.part("edgeLine" + i++, GL20.GL_LINES,
                VertexAttributes.Usage.Position, lineColor);
        for(Map.Entry<Long, Float> entry : planet.tileCollisions.entrySet()) {
            key = entry.getKey();
            edge = planet.getIndicesFromHashkey(key);
            rectangleVertices = getCollisionRectangleVertices(planet, edge);
            if(i % TILE_LIMIT == 0){
                partBuilder = modelBuilder.part("edgeLine" + i++, GL20.GL_LINES,
                        VertexAttributes.Usage.Position, lineColor);
            }
            partBuilder.line(rectangleVertices[1], rectangleVertices[2]);
            partBuilder.line(rectangleVertices[0], rectangleVertices[3]);
            i++;
        }
    }

    private Vector3[] getCollisionRectangleVertices(Planet planet, int[] edge) {
        Vector3[] vertices = new Vector3[5];
        Vector3 p1 = planet.points.get(edge[0]), p2 = planet.points.get(edge[1]);
        int subdivisions = planet.numSubdivisions;
        // polynomial eqn sets width of edge based on level of subdivision
        float baseWidthHalf = (float)(0.0027*Math.pow(subdivisions, 2.0)-0.0416*subdivisions+0.161);
        if(subdivisions > 6) baseWidthHalf *= 2;
        Vector3 offset = p1.cpy().crs(p2).nor().scl(baseWidthHalf);
        vertices[0] = p1.cpy().add(offset);
        vertices[1] = p1.cpy().add(offset.scl(-1));
        vertices[2] = p2.cpy().add(offset);
        vertices[3] = p2.cpy().add(offset.scl(-1));
        vertices[4] = p1.cpy().add(p2).scl(0.5f).nor(); // midpoint for normal
        return vertices;
    }

    private Color getCollisionColor(Planet planet, float intensity) {
        float relativeIntensity = (float)(Math.log(Math.abs(intensity)/planet.max_collision_intensity)+8)/8f;
        if(intensity > 0) {
            return new Color(1, 1-relativeIntensity, 1-relativeIntensity, 1);
        } else {
            return new Color(1-relativeIntensity, 1, 1-relativeIntensity, 1);
        }
    }

    private Color getElevationColor(Planet planet, float elevation) {
        float relativeElevation = (float)(Math.log(Math.abs(elevation)/planet.max_elevation)+8)/8f;
        if(elevation > 0) {
            return new Color(1, 1-relativeElevation, 1-relativeElevation, 1);
        } else {
            return new Color(1-relativeElevation, 1, 1-relativeElevation, 1);
        }
    }

    private void initAirParticles() {
        Random r = new Random();

        wx = new float[AIR_PARTICLES];
        wy = new float[AIR_PARTICLES];
        wz = new float[AIR_PARTICLES];
        wt = new float[AIR_PARTICLES];
        we = new float[AIR_PARTICLES];
        ua = new int[AIR_PARTICLES];

        for (int i = 0; i < AIR_PARTICLES; i++) {
            wx[i] = r.nextFloat()-0.5f;
            wy[i] = r.nextFloat()-0.5f;
            wz[i] = r.nextFloat()-0.5f;

            // normalize, scale coords to radius
            float mag = (float)Math.sqrt(wx[i]*wx[i]+wy[i]*wy[i]+wz[i]*wz[i]);
            if (mag > 0) {
                wx[i] /= mag;
                wy[i] /= mag;
                wz[i] /= mag;

                wx[i] *= PLANET_RADIUS;
                wy[i] *= PLANET_RADIUS;
                wz[i] *= PLANET_RADIUS;
            }

            we[i] = r.nextFloat() * 14000;
            ua[i] = (we[i] >= 7000) ? 1 : 0;
            wt[i] = calculateAirTemp(wx[i], wy[i], wz[i], ua[i]);
        }
    }

    private void initGPU() {
        pwx = Pointer.to(wx);
        pwy = Pointer.to(wy);
        pwz = Pointer.to(wz);
        pwt = Pointer.to(wt);
        pwe = Pointer.to(we);
        pua = Pointer.to(ua);

        final int platformIndex = 0;
        final long deviceType = CL_DEVICE_TYPE_ALL;
        final int deviceIndex = 0;

        // Enable exceptions and subsequently omit error checks in this sample
        CL.setExceptionsEnabled(true);

        // Obtain the number of platforms
        int numPlatformsArray[] = new int[1];
        clGetPlatformIDs(0, null, numPlatformsArray);
        int numPlatforms = numPlatformsArray[0];

        // Obtain a platform ID
        cl_platform_id platforms[] = new cl_platform_id[numPlatforms];
        clGetPlatformIDs(platforms.length, platforms, null);
        cl_platform_id platform = platforms[platformIndex];

        // Initialize the context properties
        cl_context_properties contextProperties = new cl_context_properties();
        contextProperties.addProperty(CL_CONTEXT_PLATFORM, platform);

        // Obtain the number of devices for the platform
        int numDevicesArray[] = new int[1];
        clGetDeviceIDs(platform, deviceType, 0, null, numDevicesArray);
        int numDevices = numDevicesArray[0];

        // Obtain a device ID
        cl_device_id devices[] = new cl_device_id[numDevices];
        clGetDeviceIDs(platform, deviceType, numDevices, devices, null);
        cl_device_id device = devices[deviceIndex];

        // Create a context for the selected device
        context = clCreateContext(
                contextProperties, 1, new cl_device_id[]{device},
                null, null, null);

        // Create a command-queue for the selected device
        commandQueue = clCreateCommandQueueWithProperties(context, device, null, null);

        // Allocate the memory objects for the data
        memObjects = new cl_mem[6];
        int size = Sizeof.cl_float * AIR_PARTICLES;
        long flags = CL_MEM_READ_WRITE | CL_MEM_COPY_HOST_PTR;
        memObjects[0] = clCreateBuffer(context, flags, size, pwx, null);
        memObjects[1] = clCreateBuffer(context, flags, size, pwy, null);
        memObjects[2] = clCreateBuffer(context, flags, size, pwz, null);
        memObjects[3] = clCreateBuffer(context, flags, size, pwt, null);
        memObjects[4] = clCreateBuffer(context, flags, size, pwe, null);
        memObjects[5] = clCreateBuffer(context, flags,
                Sizeof.cl_int * AIR_PARTICLES, pua, null);

        // Read in the program
        System.out.println("...loading Particle.cl");
        String cParticle = readFile("kernels/Particle.cl");

        // Create the program from the source code
        System.out.println("...creating particle program");
        cpParticle = clCreateProgramWithSource(context, 1, new String[]{ cParticle },
                new long[]{cParticle.length()}, null);

        // Build the program
        clBuildProgram(cpParticle, 0, null, null, null, null);

        // Create the kernel
        kernel = clCreateKernel(cpParticle, "update", null);

        // Set the arguments for the kernel
        for (int i = 0; i < memObjects.length; i++) {
            clSetKernelArg(kernel, i, Sizeof.cl_mem, Pointer.to(memObjects[i]));
        }

        // Set the work-item dimensions
        global_work_size = new long[]{AIR_PARTICLES};
        local_work_size = new long[]{1};

    }

    private float calculateAirTemp(float x, float y, float z, int ua) {
        Random r = new Random();
        float lat = VMath.cartesianToLatitude(x, y, z, PLANET_RADIUS) + (float)(Math.PI/2);
        if (lat > Math.PI / 2) {
            // reducing range to 0 - PI/2
            lat = (float)(Math.PI - lat);
        }

        float percentToBoundary;
        float temperature;

        if (lat < PI_6)
        {
            percentToBoundary = lat / PI_6;
            temperature = (ua == 1)
                    ? -20 - percentToBoundary * 35  // -55 to -20
                    : -20 + percentToBoundary * 20; // -20 to 0
        }
        else if (lat < PI_3)
        {
            percentToBoundary = (lat - PI_6) / (PI_3 - PI_6);
            temperature = (ua == 1)
                    ? -10 - percentToBoundary * 20  // -10 to -30
                    : 15 - percentToBoundary * 25;  // 15 to -10
        }
        else
        {
            percentToBoundary = (lat - PI_3) / (PI_2 - PI_3);
            temperature = (ua == 1)
                    ? -20 - percentToBoundary * 20  // -40 to -20
                    : 10 + percentToBoundary * 20;  // 10 to 30
        }

        float randomOffset = ((r.nextFloat() - 0.5f)*4);
        return temperature + randomOffset;
    }

    private static String readFile(String fileName)
    {
        try
        {
            BufferedReader br = new BufferedReader(new FileReader(fileName));
            StringBuilder sb = new StringBuilder();
            String line = null;
            while (true)
            {
                line = br.readLine();
                if (line == null)
                {
                    break;
                }
                sb.append(line+"\n");
            }
            return sb.toString();
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return "";
        }
    }
}
