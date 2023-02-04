package dev.urth.planetgen;

import static org.jocl.CL.*;

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
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import dev.urth.planetgen.layer.FrameRateLayer;
import dev.urth.planetgen.layer.Layer;
import dev.urth.planetgen.layer.TileInfoLayer;
import dev.urth.planetgen.util.ColorUtils;
import dev.urth.planetgen.util.Log;
import dev.urth.planetgen.util.Units;
import dev.urth.planetgen.util.VMath;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.Random;
import org.jocl.*;

public class PlanetGenerator extends InputAdapter implements ApplicationListener {
    private static final Random rand = new Random();
    private static final int AIR_PARTICLES = 7500;
    private static final int TILE_LIMIT = 1000;
    private static final int SUBDIVISIONS = 5;
    private static final float PLANET_RADIUS = 10;

    private CameraInterface cameraInterface; // For custom movement controls
    private Environment environment; // For lighting
    private MeshPartBuilder partBuilder; // For building models
    private ModelBatch modelBatch; // For rendering models
    private ModelBuilder modelBuilder; // For building models
    private PerspectiveCamera cam;
    private Planet planet;
    private Viewport viewport; // For proper screen resizing

    private TileInfoLayer til; // For displaying tile / plate info

    private final Array<Layer> layers = new Array<>();
    private final Array<Model> models = new Array<>();
    private final Array<ModelInstance> modelInstances = new Array<>();
    private final Array<ModelInstance> airInstances = new Array<>();

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
    private cl_mem[] memObjects;

    private long[] globalWorkSize;
    private long[] localWorkSize;

    private float[] wx;
    private float[] wy;
    private float[] wz;
    private float[] wt;
    private float[] we;
    private int[] ua;
    private final int[] frame = {0};

    @Override
    public void create() {
        modelBatch = new ModelBatch();

        /* Set lighting */
        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 1f, 1f, 1f, 1f));
        // environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.2f, 0.2f, 0.2f, 0.2f));
        // environment.add(new DirectionalLight().set(0.95f, 0.95f, 0.95f, -1, 0, 0));

        /* Set camera */
        cam = new PerspectiveCamera(50, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        float camPosMultiplier = 1.6f;
        float camCoordinate = camPosMultiplier * PLANET_RADIUS;
        cam.position.set(camCoordinate, camCoordinate, camCoordinate);
        cam.lookAt(0f, 0f, 0f);
        cam.near = 1f;
        cam.far = 50000.0f;
        cam.update();
        viewport = new ScreenViewport(cam);

        Log log = new Log();

        /* Generate planet */
        log.start("Generation time");
        planet = new Planet(new Vector3(0, 0, 0), PLANET_RADIUS, SUBDIVISIONS);

        cameraInterface = new CameraInterface(cam);
        cameraInterface.setCenter(planet.position);
        Gdx.input.setInputProcessor(new InputMultiplexer(this, cameraInterface));

        /* Build models */
        log.start("Build time");

        modelBuilder = new ModelBuilder();

        // models.add(buildIcosahedron(planet));          // Triangles
        models.add(buildTruncatedIcosahedron(planet)); // Tiles
        // models.add(buildSunRays(planet));
        // models.add(buildWireframe(planet));
        models.add(buildAxes());
        models.add(buildPlateDirectionArrows(planet));
        models.add(buildMajorLatLines());
        models.add(buildPlateCollisions(planet));
        models.add(buildLatLongSpikes(planet));

        final float partRadius = 0.05f;
        final int partDivs = 10;
        Model airParticle =
                modelBuilder.createSphere(
                        partRadius,
                        partRadius,
                        partRadius,
                        partDivs,
                        partDivs,
                        new Material(ColorAttribute.createDiffuse(Color.WHITE)),
                        VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);

        // for (AirParticle particle : planet.wind) {
        //    Vector3 v = particle.getPosition().cpy().scl(1.001f);
        //    ModelInstance airInstance = new ModelInstance(airParticle);
        //    airInstance.transform.setToTranslation(v);
        //    airInstances.add(airInstance);
        // }
        initAirParticles();

        for (int i = 0; i < AIR_PARTICLES; i++) {
            // Vector3 v = new Vector3(wx[i], wy[i], wz[i]).scl(1.001f);
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
        this.til = new TileInfoLayer();
        layers.add(til);
        layers.add(new FrameRateLayer());
    }

    @Override
    public void render() {
        cameraInterface.update(Gdx.graphics.getDeltaTime());

        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        updateAirParticles();

        frame[0]++;
        if (frame[0] == 3600) {
            frame[0] = 0;
        }

        modelBatch.begin(cam);
        for (ModelInstance instance : modelInstances) {
            modelBatch.render(instance, environment);
        }
        for (ModelInstance instance : airInstances) {
            modelBatch.render(instance, environment);
        }
        modelBatch.end();

        for (int i = 0; i < layers.size; i++) {
            Layer layer = layers.get(i);
            if (layer.isActive()) {
                layer.render();
            }
        }
    }

    @Override
    public void dispose() {
        for (int i = 0; i < layers.size; i++) {
            layers.get(i).dispose();
        }
        modelBatch.dispose();
        modelInstances.clear();
        for (Model model : models) {

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
    public void resume() {
        // Resume not yet implemented
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
        for (Layer layer : layers) {
            if (layer.isActive()) {
                layer.resize(width, height);
            }
        }
    }

    @Override
    public void pause() {
        // Pause not yet implemented
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
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
            til.setPlate(planet.getPlates().get(t.getPlateId()));
        } else {
            til.setTile(null);
            til.setPlate(null);
        }
    }

    private Vector3 getMouseIntersection(int screenX, int screenY) {
        Vector3 position = new Vector3(0, 0, 0);
        Vector3 intersection = new Vector3();

        Ray ray = cam.getPickRay(screenX, screenY);

        return (Intersector.intersectRaySphere(ray, position, PLANET_RADIUS, intersection))
                ? intersection
                : null;
    }

    private Model buildIcosahedron(Planet planet) {
        modelBuilder.begin();
        for (int i = 0; i < planet.getFaces().size; i++) {
            Face f = planet.getFaces().get(i);
            if (i % TILE_LIMIT == 0) {
                partBuilder = getTriangleMeshPart("tile" + i, new Material());
            }
            partBuilder.setColor(rand.nextFloat(), rand.nextFloat(), rand.nextFloat(), 1.0f);
            partBuilder.triangle(
                    planet.getPoints().get(f.getPts()[0]),
                    planet.getPoints().get(f.getPts()[1]),
                    planet.getPoints().get(f.getPts()[2]));
        }
        return modelBuilder.end();
    }

    private Model buildTruncatedIcosahedron(Planet planet) {
        modelBuilder.begin();

        for (int i = 0; i < planet.getTiles().size; i++) {
            Tile t = planet.getTiles().get(i);

            if (i % TILE_LIMIT == 0) {
                partBuilder = getTriangleMeshPart("tile" + i, new Material());
            }

            // color by plate
            // partBuilder.setColor(planet.getPlates().get(t.getPlateId()).getColor());
            // color by relative elevation
            partBuilder.setColor(getElevationColor(planet, t.getElevationMasl()));

            int numPts = t.getPoints().size;
            Array<Vector3> planetPoints = planet.getPoints();
            Array<Integer> tilePoints = t.getPoints();
            if (numPts == 6) {
                // partBuilder.rect(
                //         planetPoints.get(tilePoints.get(0)),
                //         planetPoints.get(tilePoints.get(1)),
                //         planetPoints.get(tilePoints.get(3)),
                //         planetPoints.get(tilePoints.get(4)),
                //         planetPoints.get(t.getCentroid())
                // );
                partBuilder.triangle(
                        planetPoints.get(tilePoints.get(0)),
                        planetPoints.get(tilePoints.get(1)),
                        planetPoints.get(tilePoints.get(3)));
                partBuilder.triangle(
                        planetPoints.get(tilePoints.get(3)),
                        planetPoints.get(tilePoints.get(4)),
                        planetPoints.get(tilePoints.get(0)));
                partBuilder.triangle(
                        planetPoints.get(tilePoints.get(4)),
                        planetPoints.get(tilePoints.get(5)),
                        planetPoints.get(tilePoints.get(0)));
                partBuilder.triangle(
                        planetPoints.get(tilePoints.get(1)),
                        planetPoints.get(tilePoints.get(2)),
                        planetPoints.get(tilePoints.get(3)));
            } else {
                for (int j = 0; j < numPts; j++) {
                    partBuilder.triangle(
                            planetPoints.get(t.getCentroid()),
                            planetPoints.get(tilePoints.get(j)),
                            planetPoints.get(tilePoints.get((j + 1) % numPts)));
                }
            }
        }
        return modelBuilder.end();
    }

    private Model buildSunRays(Planet planet) {
        modelBuilder.begin();
        Material lineColor = new Material(ColorAttribute.createDiffuse(Color.valueOf("ffffff")));
        Vector3 p1;
        partBuilder = getLineMeshPart("tile", lineColor);
        for (int i = 0; i < planet.getTiles().size; i++) {
            if (i % TILE_LIMIT == 0) {
                partBuilder = getLineMeshPart("tile", lineColor);
            }
            p1 = planet.getPoints().get(planet.getTiles().get(i).getCentroid());
            partBuilder.line(
                    p1.scl(1.0f),
                    p1.cpy()
                            .scl(
                                    1.0f
                                            + 0.00000000000000125f
                                                    * planet.getTiles().get(i).getPower()));
        }
        return modelBuilder.end();
    }

    private Model buildWireframe(Planet planet) {
        modelBuilder.begin();
        Material lineColor = new Material(ColorAttribute.createDiffuse(Color.valueOf("101010")));
        partBuilder = getLineMeshPart("tile", lineColor);

        for (int i = 0; i < planet.getTiles().size; i++) {
            if (i % TILE_LIMIT == 0) {
                partBuilder = getLineMeshPart("tile", lineColor);
            }
            Tile t = planet.getTiles().get(i);
            int numPts = t.getPoints().size;
            for (int j = 0; j < numPts; j++) {
                int k = (j + 1) % numPts;
                Vector3 p1 = planet.getPoints().get(t.getPoints().get(j)).cpy();
                Vector3 p2 = planet.getPoints().get(t.getPoints().get(k)).cpy();
                if (!t.getNeighbor(t.getPoints().get(j), t.getPoints().get(k)).isDrawn())
                    partBuilder.line(p1.scl(1.000008f), p2.scl(1.000008f));
            }
            planet.getTiles().get(i).setDrawn(true);
        }
        return modelBuilder.end();
    }

    private Model buildAxes() {
        final float length = 15f;
        final String partName = "axes";
        final Material xMaterial = new Material(ColorAttribute.createDiffuse(Color.RED));
        final Material yMaterial = new Material(ColorAttribute.createDiffuse(Color.BLUE));
        final Material zMaterial = new Material(ColorAttribute.createDiffuse(Color.GREEN));
        modelBuilder.begin();
        partBuilder = getLineMeshPart(partName, xMaterial);
        partBuilder.line(0, 0, 0, length, 0, 0);
        partBuilder = getLineMeshPart(partName, yMaterial);
        partBuilder.line(0, 0, 0, 0, length, 0);
        partBuilder = getLineMeshPart(partName, zMaterial);
        partBuilder.line(0, 0, 0, 0, 0, length);
        return modelBuilder.end();
    }

    private Model buildMajorLatLines() {
        modelBuilder.begin();
        double tropicLatitudeRad = 23.5 * Math.PI / 180;
        float tropicHeight = (float) Math.sin(tropicLatitudeRad) * PLANET_RADIUS;
        float tropicRadius = (float) Math.cos(tropicLatitudeRad) * PLANET_RADIUS;
        double arcticLatitudeRad = 66.5 * Math.PI / 180;
        float arcticHeight = (float) Math.sin(arcticLatitudeRad) * PLANET_RADIUS;
        float arcticRadius = (float) Math.cos(arcticLatitudeRad) * PLANET_RADIUS;

        Material equatorMaterial = new Material(ColorAttribute.createDiffuse(Color.RED));
        partBuilder = getLineMeshPart("equator", equatorMaterial);
        buildEllipseModel(partBuilder, PLANET_RADIUS, 0f);

        Material tropicMaterial = new Material(ColorAttribute.createDiffuse(Color.ORANGE));
        partBuilder = getLineMeshPart("tropics", tropicMaterial);
        buildEllipseModel(partBuilder, tropicRadius, tropicHeight);
        buildEllipseModel(partBuilder, tropicRadius, -tropicHeight);

        Material arcticMaterial = new Material(ColorAttribute.createDiffuse(Color.BLUE));
        partBuilder = getLineMeshPart("polarCircles", arcticMaterial);
        buildEllipseModel(partBuilder, arcticRadius, arcticHeight);
        buildEllipseModel(partBuilder, arcticRadius, -arcticHeight);
        return modelBuilder.end();
    }

    private void buildEllipseModel(MeshPartBuilder pb, float radius, float height) {
        Vector3 normal = new Vector3(0f, 1f, 0f);
        Vector3 center = new Vector3(0f, height, 0f);
        EllipseShapeBuilder.build(pb, radius * 1.000010f, 480, center, normal);
    }

    private Model buildLatLongSpikes(Planet planet) {
        modelBuilder.begin();
        Material lineColor = new Material(ColorAttribute.createDiffuse(Color.valueOf("ffffff")));
        partBuilder = getLineMeshPart("tile", lineColor);

        float pi = MathUtils.PI;
        Vector3 p1, p2;
        Log l2 = new Log();
        l2.start("Generate pickCircles");
        for (int i = 0; i < 32; i++) {
            for (int j = 0; j < 4; j++) {
                float latitude = (i * (pi / 16.0f)) - pi;
                float longitude = j * pi / 4.0f;
                p1 =
                        planet.getPoints()
                                .get(planet.getNearestTile(latitude, longitude).getCentroid());
                p2 = p1.cpy().scl(1.5f);
                partBuilder.line(p1, p2);
            }
        }
        l2.end();
        return modelBuilder.end();
    }

    private MeshPartBuilder getLineMeshPart(String partName, Material material) {
        return modelBuilder.part(
                partName, GL20.GL_LINES, VertexAttributes.Usage.Position, material);
    }

    private MeshPartBuilder getTriangleMeshPart(String name, Material material) {
        int attributes =
                VertexAttributes.Usage.Position
                        | VertexAttributes.Usage.Normal
                        | VertexAttributes.Usage.ColorPacked;
        return modelBuilder.part(name, GL20.GL_TRIANGLES, attributes, material);
    }

    private Model buildWindLines(Planet planet) {
        // TODO: Create 1 line model and create transformed instances

        Log l = new Log();
        l.start("Build wind lines");
        modelBuilder.begin();
        Material lineColor = new Material(ColorAttribute.createDiffuse(Color.VIOLET));
        partBuilder =
                modelBuilder.part(
                        "wind", GL20.GL_LINES, VertexAttributes.Usage.Position, lineColor);

        SphereShapeBuilder.build(partBuilder, 0.5f, 0.5f, 0.5f, 60, 60);
        //
        // Vector3 p1 = new Vector3(0,0,0);
        // for (int i = 0; i < planet.wind.size; i++) {
        //     partBuilder.line(p1, planet.wind.get(i).getPosition().cpy().scl(1.001f));
        // }

        l.end();

        return modelBuilder.end();
    }

    private void updateAirParticles() {
        //     for (int i = 0; i < AIR_PARTICLES; i++) {
        //       planet.getWind()[i].update();
        //     }

        clSetKernelArg(kernel, 6, Sizeof.cl_int, Pointer.to(new int[] {frame[0]}));

        // Execute the kernel
        clEnqueueNDRangeKernel(
                commandQueue, kernel, 1, null, globalWorkSize, localWorkSize, 0, null, null);

        // Read back results
        readBackAirParticleData(memObjects[0], Sizeof.cl_float, pwx);
        readBackAirParticleData(memObjects[1], Sizeof.cl_float, pwy);
        readBackAirParticleData(memObjects[2], Sizeof.cl_float, pwz);
        readBackAirParticleData(memObjects[3], Sizeof.cl_float, pwt);
        readBackAirParticleData(memObjects[4], Sizeof.cl_float, pwe);
        readBackAirParticleData(memObjects[5], Sizeof.cl_int, pua);

        // System.out.println("After:  " + frame[0]);

        for (int i = 0; i < AIR_PARTICLES; i++) {
            airInstances.get(i).transform.setToTranslation(wx[i], wy[i], wz[i]);
        }
    }

    private void readBackAirParticleData(cl_mem memObj, int dataSize, Pointer ptr) {
        long size = (long) AIR_PARTICLES * dataSize;
        clEnqueueReadBuffer(commandQueue, memObj, CL_TRUE, 0, size, ptr, 0, null, null);
    }

    private Model buildPlateDirectionArrows(Planet planet) {
        modelBuilder.begin();
        final Material material = new Material();
        int i = 0;
        for (Plate plate : planet.getPlates().values()) {
            partBuilder = getTriangleMeshPart("arrow" + i++, material);
            partBuilder.setColor(ColorUtils.getComplementary(plate.getColor()));
            for (Tile tile : plate.getMembers()) {
                Vector3[] arrowVertices =
                        getArrowVertices(planet, tile, tile.getTangentialVelocity());
                if (i % TILE_LIMIT == 0) {
                    partBuilder = getTriangleMeshPart("arrow" + i++, material);
                    partBuilder.setColor(ColorUtils.getComplementary(plate.getColor()));
                }
                partBuilder.triangle(arrowVertices[0], arrowVertices[1], arrowVertices[2]);
                i++;
            }
        }
        return modelBuilder.end();
    }

    private Vector3[] getArrowVertices(Planet planet, Tile t, Vector3 direction) {
        Array<Integer> pts = t.getPoints();
        float arrowHeight =
                (float)
                        ((764428
                                        / (Planet.PLATE_COLLISION_TIME_STEP_IN_MILLIONS_OF_YEARS
                                                * Units.CM_YR_TO_M_MA))
                                * Math.exp(-0.653 * planet.numSubdivisions));
        float baseWidthHalf;
        if (pts.size == 6) {
            baseWidthHalf =
                    planet.getPoints().get(pts.get(0)).dst(planet.getPoints().get(pts.get(3)))
                            * 0.1f;
        } else {
            Vector3 u = planet.getPoints().get(pts.get(2));
            Vector3 v = planet.getPoints().get(pts.get(3));
            Vector3 w = u.cpy().add(v).scl(0.5f);
            baseWidthHalf = planet.getPoints().get(pts.get(0)).dst(w) * 0.1f;
        }
        Vector3 tileCentroid = planet.getPoints().get(t.getCentroid());
        Vector3 base1 =
                tileCentroid.cpy().add(direction.cpy().crs(tileCentroid).nor().scl(-baseWidthHalf));
        Vector3 base2 =
                tileCentroid.cpy().add(direction.cpy().crs(tileCentroid).nor().scl(baseWidthHalf));
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
        final Material material = new Material();
        int i = 0;
        partBuilder = getTriangleMeshPart("edge" + i++, material);
        for (Map.Entry<Long, Float> entry : planet.getTileCollisions().entrySet()) {
            Long key = entry.getKey();
            int[] edge = planet.getIndicesFromHashkey(key);
            Vector3[] rectangleVertices = getCollisionRectangleVertices(planet, edge);
            if (i % TILE_LIMIT == 0) {
                partBuilder = getTriangleMeshPart("edge" + i++, material);
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
        int i = 0;
        Material lineColor = new Material(ColorAttribute.createDiffuse(Color.valueOf("101010")));
        partBuilder = getLineMeshPart("edgeLine" + i++, lineColor);

        for (Map.Entry<Long, Float> entry : planet.getTileCollisions().entrySet()) {
            Long key = entry.getKey();
            int[] edge = planet.getIndicesFromHashkey(key);
            Vector3[] rectangleVertices = getCollisionRectangleVertices(planet, edge);
            if (i % TILE_LIMIT == 0) {
                partBuilder = getLineMeshPart("edgeLine" + i++, lineColor);
            }
            partBuilder.line(rectangleVertices[1], rectangleVertices[2]);
            partBuilder.line(rectangleVertices[0], rectangleVertices[3]);
            i++;
        }
    }

    private Vector3[] getCollisionRectangleVertices(Planet planet, int[] edge) {
        Vector3[] vertices = new Vector3[5];
        Vector3 p1 = planet.getPoints().get(edge[0]);
        Vector3 p2 = planet.getPoints().get(edge[1]);
        int subdivisions = planet.numSubdivisions;
        // polynomial eqn sets width of edge based on level of subdivision
        float baseWidthHalf =
                (float) (0.0027 * Math.pow(subdivisions, 2.0) - 0.0416 * subdivisions + 0.161);
        if (subdivisions > 6) baseWidthHalf *= 2;
        Vector3 offset = p1.cpy().crs(p2).nor().scl(baseWidthHalf);
        vertices[0] = p1.cpy().add(offset);
        vertices[1] = p1.cpy().add(offset.scl(-1));
        vertices[2] = p2.cpy().add(offset);
        vertices[3] = p2.cpy().add(offset.scl(-1));
        vertices[4] = p1.cpy().add(p2).scl(0.5f).nor(); // midpoint for normal
        return vertices;
    }

    private Color getCollisionColor(Planet planet, float intensity) {
        float relativeIntensity =
                (float) (Math.log(Math.abs(intensity) / planet.getMaxCollisionIntensity()) + 8)
                        / 8f;
        if (intensity > 0) {
            return new Color(1, 1 - relativeIntensity, 1 - relativeIntensity, 1);
        } else {
            return new Color(1 - relativeIntensity, 1, 1 - relativeIntensity, 1);
        }
    }

    private Color getElevationColor(Planet planet, float elevation) {
        float relativeElevation =
                (float) (Math.log(Math.abs(elevation) / planet.getMaxElevation()) + 8) / 8f;
        if (elevation > 0) {
            return new Color(1, 1 - relativeElevation, 1 - relativeElevation, 1);
        } else {
            return new Color(1 - relativeElevation, 1, 1 - relativeElevation, 1);
        }
    }

    private void initAirParticles() {
        wx = new float[AIR_PARTICLES];
        wy = new float[AIR_PARTICLES];
        wz = new float[AIR_PARTICLES];
        wt = new float[AIR_PARTICLES];
        we = new float[AIR_PARTICLES];
        ua = new int[AIR_PARTICLES];

        for (int i = 0; i < AIR_PARTICLES; i++) {
            wx[i] = rand.nextFloat() - 0.5f;
            wy[i] = rand.nextFloat() - 0.5f;
            wz[i] = rand.nextFloat() - 0.5f;

            // normalize, scale coords to radius
            float mag = (float) Math.sqrt(wx[i] * wx[i] + wy[i] * wy[i] + wz[i] * wz[i]);
            if (mag > 0) {
                wx[i] /= mag;
                wy[i] /= mag;
                wz[i] /= mag;

                wx[i] *= PLANET_RADIUS;
                wy[i] *= PLANET_RADIUS;
                wz[i] *= PLANET_RADIUS;
            }

            we[i] = rand.nextFloat() * 14000;
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
        int[] numPlatformsArray = new int[1];
        clGetPlatformIDs(0, null, numPlatformsArray);
        int numPlatforms = numPlatformsArray[0];

        // Obtain a platform ID
        cl_platform_id[] platforms = new cl_platform_id[numPlatforms];
        clGetPlatformIDs(platforms.length, platforms, null);
        cl_platform_id platform = platforms[platformIndex];

        // Initialize the context properties
        cl_context_properties contextProperties = new cl_context_properties();
        contextProperties.addProperty(CL_CONTEXT_PLATFORM, platform);

        // Obtain the number of devices for the platform
        int[] numDevicesArray = new int[1];
        clGetDeviceIDs(platform, deviceType, 0, null, numDevicesArray);
        int numDevices = numDevicesArray[0];

        // Obtain a device ID
        cl_device_id[] devices = new cl_device_id[numDevices];
        clGetDeviceIDs(platform, deviceType, numDevices, devices, null);
        cl_device_id device = devices[deviceIndex];

        // Create a context for the selected device
        context =
                clCreateContext(
                        contextProperties, 1, new cl_device_id[] {device}, null, null, null);

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
        memObjects[5] = clCreateBuffer(context, flags, Sizeof.cl_int * AIR_PARTICLES, pua, null);

        // Read in the program
        System.out.println("...loading Particle.cl");
        String cParticle = readFile("kernels/Particle.cl");

        // Create the program from the source code
        System.out.println("...creating particle program");
        cpParticle =
                clCreateProgramWithSource(
                        context,
                        1,
                        new String[] {cParticle},
                        new long[] {cParticle.length()},
                        null);

        // Build the program
        clBuildProgram(cpParticle, 0, null, null, null, null);

        // Create the kernel
        kernel = clCreateKernel(cpParticle, "update", null);

        // Set the arguments for the kernel
        for (int i = 0; i < memObjects.length; i++) {
            clSetKernelArg(kernel, i, Sizeof.cl_mem, Pointer.to(memObjects[i]));
        }

        // Set the work-item dimensions
        globalWorkSize = new long[] {AIR_PARTICLES};
        localWorkSize = new long[] {1};
    }

    private float calculateAirTemp(float x, float y, float z, int ua) {
        float lat = VMath.cartesianToLatitude(x, y, z, PLANET_RADIUS) + (float) (Math.PI / 2);
        if (lat > Math.PI / 2) {
            // reducing range to 0 - PI/2
            lat = (float) (Math.PI - lat);
        }

        float percentToBoundary;
        float temperature;

        if (lat < VMath.PI_6) {
            percentToBoundary = lat / VMath.PI_6;
            temperature =
                    (ua == 1)
                            ? -20 - percentToBoundary * 35 // -55 to -20
                            : -20 + percentToBoundary * 20; // -20 to 0
        } else if (lat < VMath.PI_3) {
            percentToBoundary = (lat - VMath.PI_6) / (VMath.PI_3 - VMath.PI_6);
            temperature =
                    (ua == 1)
                            ? -10 - percentToBoundary * 20 // -10 to -30
                            : 15 - percentToBoundary * 25; // 15 to -10
        } else {
            percentToBoundary = (lat - VMath.PI_3) / (VMath.PI_2 - VMath.PI_3);
            temperature =
                    (ua == 1)
                            ? -20 - percentToBoundary * 20 // -40 to -20
                            : 10 + percentToBoundary * 20; // 10 to 30
        }

        float randomOffset = ((rand.nextFloat() - 0.5f) * 4);
        return temperature + randomOffset;
    }

    private static String readFile(String fileName) {
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }
}
