package dev.urth.planetgen;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import dev.urth.planetgen.util.Log;
import dev.urth.planetgen.util.Units;
import dev.urth.planetgen.util.VMath;
import java.util.*;

public class Planet {
    public static final int PLATE_COUNT = 72;
    public static final int PLATE_COLLISION_TIME_STEP_IN_MILLIONS_OF_YEARS = 50;
    public static final Random rand = new Random();

    public final int numSubdivisions;
    public final Vector3 position;

    private final float radius;
    private float maxCollisionIntensity = 0f;
    private float maxElevation = 0f;

    private final Array<Vector3> points = new Array<>();
    private final Array<Face> faces = new Array<>();
    private final Array<Tile> tiles = new Array<>();

    private final Map<Long, Integer> midpointCache = new HashMap<>();
    private final Map<Long, Face[]> faceNeighbors = new HashMap<>();
    private final Map<Long, Tile[]> tileNeighbors = new HashMap<>();

    private final Map<Integer, Plate> plates = new HashMap<>();
    private final Map<Long, Float> tileCollisions = new HashMap<>();
    private final Map<Long, Integer> plateCollisions = new HashMap<>();

    private AirParticle[] wind;
    //    public Array<AirParticle> wind = new Array<AirParticle>();
    //    public Set<Integer> lowerAtmosphere = new HashSet<Integer>();
    //    public Set<Integer> upperAtmosphere = new HashSet<Integer>();

    private TileMap tileMap;

    Planet(Vector3 position, float radius, int subdivisions) {
        this.radius = radius;
        this.position = position;
        this.numSubdivisions = subdivisions;
        generateIcosphere();
    }

    private void generateIcosphere() {
        Log log = new Log();

        generateIcosahedron();

        log.start("Subdivision");
        subdivideFaces(numSubdivisions);

        log.start("Dual Conversion");
        convertToTruncatedIcosphere();
        faces.clear(); // array no longer needed if rendering truncated icosphere
        // System.out.println(points.get(tiles.get(0).centroid).dst(points.get(tiles.get(0).nbrs.get(0).centroid)));

        log.start("Plate generation");
        generatePlates();
        log.end();

        scalePoints(points, radius);

        log.start("Log Tile Lat/Longs");
        tileMap = new TileMap(tiles, radius);

        log.start("Generate wind points");
        generateWind();
        log.end();

        float minLat = tiles.get(0).getLatitude();
        float maxLat = tiles.get(0).getLatitude();
        for (Tile t : tiles) {
            float lat = t.getLatitude();
            if (lat < minLat) minLat = lat;
            else if (lat > maxLat) maxLat = lat;
        }
        System.out.println("MinLat: " + minLat);
        System.out.println("MaxLat: " + maxLat);

        // System.out.println("Faces:  " + faces.size);
        System.out.println("Tiles:  " + tiles.size);
        System.out.println("Plates: " + plates.size());
    }

    private void generateIcosahedron() {
        float phi = (float) ((1.0f + Math.sqrt(5.0f)) / 2.0f);
        float u = 1.0f / (float) Math.sqrt(phi * phi + 1.0f);
        float v = phi * u;

        addVertex(new Vector3(0f, +v, +u));
        addVertex(new Vector3(0f, +v, -u));
        addVertex(new Vector3(0f, -v, +u));
        addVertex(new Vector3(0f, -v, -u));
        addVertex(new Vector3(+u, 0f, +v));
        addVertex(new Vector3(-u, 0f, +v));
        addVertex(new Vector3(+u, 0f, -v));
        addVertex(new Vector3(-u, 0f, -v));
        addVertex(new Vector3(+v, +u, 0f));
        addVertex(new Vector3(+v, -u, 0f));
        addVertex(new Vector3(-v, +u, 0f));
        addVertex(new Vector3(-v, -u, 0f));

        faces.addAll(
                new Face(0, 8, 1, getCentroidFromIndices(0, 8, 1)),
                new Face(0, 5, 4, getCentroidFromIndices(0, 5, 4)),
                new Face(0, 10, 5, getCentroidFromIndices(0, 10, 5)),
                new Face(0, 4, 8, getCentroidFromIndices(0, 4, 8)),
                new Face(0, 1, 10, getCentroidFromIndices(0, 1, 10)),
                new Face(1, 8, 6, getCentroidFromIndices(1, 8, 6)),
                new Face(1, 6, 7, getCentroidFromIndices(1, 6, 7)),
                new Face(1, 7, 10, getCentroidFromIndices(1, 7, 10)),
                new Face(2, 11, 3, getCentroidFromIndices(2, 11, 3)),
                new Face(2, 9, 4, getCentroidFromIndices(2, 9, 4)),
                new Face(2, 4, 5, getCentroidFromIndices(2, 4, 5)),
                new Face(2, 3, 9, getCentroidFromIndices(2, 3, 9)),
                new Face(2, 5, 11, getCentroidFromIndices(2, 5, 11)),
                new Face(3, 7, 6, getCentroidFromIndices(3, 7, 6)),
                new Face(3, 11, 7, getCentroidFromIndices(3, 11, 7)),
                new Face(3, 6, 9, getCentroidFromIndices(3, 6, 9)),
                new Face(4, 9, 8, getCentroidFromIndices(4, 9, 8)),
                new Face(5, 10, 11, getCentroidFromIndices(5, 10, 11)),
                new Face(6, 8, 9, getCentroidFromIndices(6, 8, 9)),
                new Face(7, 11, 10, getCentroidFromIndices(7, 11, 10)));
    }

    /* subdivides faces n times */
    private void subdivideFaces(int numSubdivisions) {
        for (int i = 0; i < numSubdivisions; i++) {
            Array<Face> newFaces = new Array<>();
            for (Face face : faces) {
                Face[] subdividedFaces = subdivideFace(face);
                if (i == numSubdivisions - 1) {
                    setFaceNeighbors(subdividedFaces);
                }
                newFaces.addAll(subdividedFaces);
            }
            faces.clear();
            faces.ensureCapacity(newFaces.size);
            faces.addAll(newFaces);
        }
        midpointCache.clear();
    }

    private void generatePlates() {
        placePlateRoots();
        floodFillPlates();
        updatePlateBorders();
        removeLongestPlates(8);
        eliminateIsolatedPlates();
        updatePlateBorders();
        // TODO: Place minor and micro plates along the borders of the majors
        assignPlateAttributes();
        assignTileAttributes();
        calculatePlateCollisionIntensities();
        simulateCollisions();
    }

    private void assignTileAttributes() {
        addBaseTileAttributes();
        randomizeTileElevations();
        randomizeTileTemperatures();
        generateSolPower(new Sun(new Vector3(150000000.0f, 0, 0), 3.8679289e20f));
    }

    private Face[] subdivideFace(Face face) {
        int[] pts = face.getPts();
        int p0 = pts[0];
        int p1 = pts[1];
        int p2 = pts[2];

        int m0 = getMidpointFromIndicesAndStore(p0, p1);
        int m1 = getMidpointFromIndicesAndStore(p1, p2);
        int m2 = getMidpointFromIndicesAndStore(p2, p0);

        return new Face[] {
            new Face(p0, m0, m2, getCentroidFromIndices(p0, m0, m2)),
            new Face(p1, m1, m0, getCentroidFromIndices(p1, m1, m0)),
            new Face(p2, m2, m1, getCentroidFromIndices(p2, m2, m1)),
            new Face(m0, m1, m2, getCentroidFromIndices(m0, m1, m2))
        };
    }

    private void setFaceNeighbors(Face[] faces) {
        for (Face f : faces) {
            for (int i = 0; i < f.getPts().length; i++) {
                addFaceEdgeToNbrCache(f, f.getPts()[i], f.getPts()[(i + 1) % f.getPts().length]);
            }
        }
    }

    private void convertToTruncatedIcosphere() {
        Map<Integer, Boolean> ptsUsedAsTileCentroid = new HashMap<>();
        for (Face face : faces) {
            int tileCentroid = face.getPts()[0];
            if (ptsUsedAsTileCentroid.get(tileCentroid) != null) {
                tileCentroid = face.getPts()[1];
            }
            if (ptsUsedAsTileCentroid.get(tileCentroid) != null) {
                continue;
            }
            Tile t = getTileFromFace(face, tileCentroid);
            ptsUsedAsTileCentroid.put(tileCentroid, true);
            setTileNeighbors(t);
            tiles.add(t);
        }
    }

    private Tile getTileFromFace(Face initialFace, int tileCentroid) {
        Array<Integer> newTilePts = new Array<>();
        Face currentFace = initialFace;
        do {
            newTilePts.add(currentFace.getCentroid());
            int clockwisePt = currentFace.getClockwisePt(tileCentroid);
            currentFace = getFaceNbr(currentFace, tileCentroid, clockwisePt);
        } while (currentFace != initialFace);
        return new Tile(tileCentroid, newTilePts, points);
    }

    private void setTileNeighbors(Tile t) {
        for (int i = 0; i < t.getPoints().size; i++) {
            addTileEdgeToNbrCache(
                    t, t.getPoints().get(i), t.getPoints().get((i + 1) % t.getPoints().size));
        }
    }

    private void placePlateRoots() {
        while (plates.size() < PLATE_COUNT) {
            int newPlateId = rand.nextInt(0x7fff);
            int tileIndex = rand.nextInt(tiles.size);
            if (tiles.get(tileIndex).getPlateId() == -1 && plates.get(newPlateId) == null)
                plates.put(newPlateId, new Plate(tiles.get(tileIndex), newPlateId));
        }
    }

    private void floodFillPlates() {
        List<Integer> keysArray = new ArrayList<>(plates.keySet());
        for (int i = 0; i < tiles.size * 1.6; i++) {
            plates.get(keysArray.get(rand.nextInt(keysArray.size()))).grow(points);
        }
    }

    private void updatePlateBorders() {
        for (Plate plate : plates.values()) {
            plate.getBorder().clear();
            plate.createBorder();
        }
    }

    private void removeLongestPlates(int minPlates) {
        Map<Integer, Plate> availPlates = new HashMap<>(plates);
        while (plates.size() > minPlates) {
            if (availPlates.isEmpty()) {
                break;
            }
            Plate longest = getLongestPlate(availPlates);
            Plate biggestNbr = getBiggestNeighborPlate(longest);
            // absorb that neighbor if their combined area < 25% global area
            if (biggestNbr != null && isCombinedPlateAreaUnderThreshold(longest, biggestNbr)) {
                mergePlates(longest, biggestNbr);
                availPlates.remove(biggestNbr.getId());
            } else {
                availPlates.remove(longest.getId());
            }
        }
    }

    private Plate getLongestPlate(Map<Integer, Plate> availPlates) {
        ArrayList<Integer> keysArray = new ArrayList<>(availPlates.keySet());
        Plate longest = availPlates.get(keysArray.get(0));
        float longestRatio = (float) longest.getBorder().size / (float) longest.getMembers().size;
        for (Plate newPlate : availPlates.values()) {
            float newRatio = (float) newPlate.getBorder().size / (float) newPlate.getMembers().size;
            if (newRatio > longestRatio) {
                longest = newPlate;
                longestRatio = newRatio;
            }
        }
        return longest;
    }

    private Plate getBiggestNeighborPlate(Plate sourcePlate) {
        Map<Integer, Integer> numOccurrences = getPlateNeighborsLengthAlongBorder(sourcePlate);
        int plateId = sourcePlate.getId();
        int maxOccurrences = 0;
        for (Map.Entry<Integer, Integer> entry : numOccurrences.entrySet()) {
            Integer key = entry.getKey();
            Integer value = entry.getValue();
            if (value > maxOccurrences) {
                maxOccurrences = value;
                plateId = key;
            }
        }
        return plates.get(plateId);
    }

    private void mergePlates(Plate primary, Plate secondary) {
        for (Tile t : secondary.getMembers()) {
            t.setPlateId(primary.getId());
        }
        secondary.getRoot().setRoot(false);
        plates.remove(secondary.getId());
        primary.getMembers().ensureCapacity(secondary.getMembers().size);
        primary.getMembers().addAll(secondary.getMembers());
        primary.getBorder().ensureCapacity(secondary.getBorder().size);
        primary.getBorder().addAll(secondary.getBorder());
        primary.calibrateBorder();
    }

    private boolean isCombinedPlateAreaUnderThreshold(Plate a, Plate b) {
        if (a == b) {
            return false;
        }
        float percentArea = (float) (a.getMembers().size + b.getMembers().size) / tiles.size;
        return percentArea <= 0.20;
    }

    private Map<Integer, Integer> getPlateNeighborsLengthAlongBorder(Plate p) {
        Map<Integer, Integer> numOccurrences = new HashMap<>();
        for (Tile bdr : p.getBorder()) {
            for (Tile nbr : bdr.getNeighbors()) {
                if (nbr.getPlateId() != p.getId()) {
                    numOccurrences.merge(nbr.getPlateId(), 1, Integer::sum);
                }
            }
        }
        return numOccurrences;
    }

    private void eliminateIsolatedPlates() {
        Array<Plate[]> platesToBeMerged = new Array<>();
        for (Plate plate : plates.values()) {
            Map<Integer, Integer> numOccurrences = getPlateNeighborsLengthAlongBorder(plate);
            if (numOccurrences.size() == 1) {
                ArrayList<Integer> keysArray = new ArrayList<>(numOccurrences.keySet());
                Plate parent = plates.get(keysArray.get(0));
                platesToBeMerged.add(new Plate[] {plate, parent});
            }
        }
        for (Plate[] pair : platesToBeMerged) {
            mergePlates(pair[0], pair[1]);
        }
    }

    private Plate[] sortPlatesByArea() {
        Plate[] sortedPlates = new Plate[plates.size()];
        int i = 0;
        for (Plate plate : plates.values()) {
            sortedPlates[i] = plate;
            i++;
        }
        Arrays.sort(sortedPlates, Comparator.comparingInt((Plate a) -> a.getMembers().size));
        return sortedPlates;
    }

    // TODO: Refactor
    private void assignPlateAttributes() {
        // Assign class of plate (oceanic/continental)
        // Assign random axis of rotation and velocity$cm_year to each plate
        float massConversionConstant = (float) Math.pow(Units.KM_TO_CM, 3.0f) * Units.G_TO_MTONS;

        int continentalCount = (int) (plates.size() * .65);
        Plate[] sortedPlates = sortPlatesByArea();
        for (int i = 0; i < continentalCount; i++) {
            sortedPlates[i].setOceanic(false);
        }

        for (Plate plate : plates.values()) {
            plate.setCenterOfMass(new Vector3());
            for (Tile t : plate.getMembers()) {
                plate.getCenterOfMass().add(points.get(t.getCentroid()));
            }
            plate.getCenterOfMass().scl(1f / plate.getMembers().size);

            if (plate.isOceanic()) {
                plate.setDensityGmPerCm3(2.5f + rand.nextFloat() / 2.0f);
                plate.setThicknessKm(3.0f + rand.nextFloat() * 8.0f);
            } else {
                plate.setDensityGmPerCm3(2.0f + rand.nextFloat() / 2.0f);
                plate.setThicknessKm(30.0f + rand.nextFloat() * 5.0f);
            }
            for (Tile t : plate.getMembers()) {
                t.setThickness(plate.getThicknessKm());
            }

            plate.setAreaKm2(
                    plate.getMembers().size
                            * (MathUtils.PI * 4.0f * (radius * radius) / tiles.size));

            plate.setMassMg(
                    plate.getDensityGmPerCm3()
                            * plate.getThicknessKm()
                            * plate.getAreaKm2()
                            * massConversionConstant);

            plate.setSpeedMPerMa(
                    rand.nextFloat()
                            * 10f
                            * Units.CM_YR_TO_M_MA
                            * PLATE_COLLISION_TIME_STEP_IN_MILLIONS_OF_YEARS);
            plate.setAngularSpeedRadPerYr(
                    plate.getSpeedMPerMa() / (radius * Units.KM_TO_CM)); // TINY

            plate.setAxisOfRotation(new Vector3().setToRandomDirection());

            plate.setAngularVelocity(
                    new Vector3(plate.getAxisOfRotation()).scl(plate.getAngularSpeedRadPerYr()));
            plate.setTangentialVelocity(
                    new Vector3(plate.getAngularVelocity()).crs(plate.getCenterOfMass()));
            plate.setMomentumCmMgPerYr(plate.getSpeedMPerMa() * plate.getMassMg());

            for (Tile t : plate.getMembers()) {
                t.setTangentialVelocity(
                        new Vector3(plate.getAngularVelocity()).crs(points.get(t.getCentroid())));
            }
        }
    }

    private void calculatePlateCollisionIntensities() {
        for (Plate plate : plates.values()) {
            for (Tile borderTile : plate.getBorder()) {
                for (int i = 0; i < borderTile.getPoints().size; i++) {
                    int edgeP1 = borderTile.getPoints().get(i);
                    int edgeP2 = borderTile.getPoints().get((i + 1) % borderTile.getPoints().size);
                    Tile neighbor = getTileNbr(borderTile, edgeP1, edgeP2);
                    if (neighbor.getPlateId() != borderTile.getPlateId()) {
                        Plate nbrPlate = plates.get(neighbor.getPlateId());
                        Long edgeKey = getHashKeyFromIndices(edgeP1, edgeP2);
                        if (!tileCollisions.containsKey(edgeKey)) {
                            float intensity =
                                    getCollisionIntensity(borderTile, neighbor, edgeP1, edgeP2);
                            logMaxIntensity(intensity);
                            tileCollisions.put(edgeKey, intensity);
                            plateCollisions.put(
                                    edgeKey,
                                    getHashKeyFromPlateIDs(plate.getId(), nbrPlate.getId()));
                        }
                    }
                }
            }
        }
    }

    private void simulateCollisions() {
        int propagationLimit = (int) (Math.ceil(0.0625 * Math.exp(0.6931 * numSubdivisions)));
        for (Plate plate : plates.values()) {
            for (Tile bdr : plate.getBorder()) {
                float sumOfIntensitiesActingOnTile = sumIntensities(bdr);
                Vector3 epicenter = getCentroidOfCollision(bdr);
                adjustElevation(bdr, sumOfIntensitiesActingOnTile, epicenter, new Array<>());
            }
        }

        // for (Long key : plateCollisions.keySet()) {
        //    float intensity = tileCollisions.get(key);
        //    int[] neighborIDs = getPlateIDsFromHashKey(plateCollisions.get(key));
        //    Plate plateA = plates.get(neighborIDs[0]);
        //    Plate plateB = plates.get(neighborIDs[0]);
        //    CC+ createMountains();
        //    CC- createRiftValley();
        //    CO+ createContinentalSubductionZone();
        //    CO- createContinentalSlope();
        //    OO+ createOceanicSubductionZone();
        //    OO- createOceanRidge();
        // }

    }

    private float sumIntensities(Tile t) {
        float sum = 0;
        for (int i = 0; i < t.getPoints().size; i++) {
            Long key =
                    getHashKeyFromIndices(
                            t.getPoints().get(i), t.getPoints().get((i + 1) % t.getPoints().size));
            if (tileCollisions.get(key) != null) {
                sum += tileCollisions.get(key);
            }
        }
        return sum;
    }

    private Vector3 getCentroidOfCollision(Tile t) {
        Array<Vector3> edgePoints = new Array<>();
        for (int i = 0; i < t.getPoints().size; i++) {
            int edgeP1 = t.getPoints().get(i);
            int edgeP2 = t.getPoints().get((i + 1) % t.getPoints().size);
            Long edgeKey = getHashKeyFromIndices(edgeP1, edgeP2);
            if (tileCollisions.get(edgeKey) != null) {
                edgePoints.addAll(points.get(edgeP1), points.get(edgeP2));
            }
        }
        return VMath.centroid(edgePoints);
    }

    private Array<Tile> adjustElevation(
            Tile origin, float intensity, Vector3 epicenter, Array<Tile> alreadyAffected) {
        // TODO: need to change elevationChange formula to something more grounded in reality
        // TODO: propagation should be determined by combination of intensity and distance?
        float distanceFromEpicenter = points.get(origin.getCentroid()).dst(epicenter);
        float elevationChange = intensity * (1 / distanceFromEpicenter);
        // TODO: magic number; replace with value taking radius into account
        float propagationLimit = 0.068f;
        // System.out.printf("intensity: %.3f dst: %.3f new Elev: %.3f  tilesAffectedSize:
        // %d\n",
        // intensity, distanceFromEpicenter, elevationChange, tilesAlreadyAffected.size);
        alreadyAffected.add(origin);
        if (distanceFromEpicenter > propagationLimit || numSubdivisions < 4) {
            return alreadyAffected;
        }
        origin.setElevationMasl(origin.getElevationMasl() + elevationChange);
        // TODO: find better place to log elevation for this test...
        logMaxElevation(origin.getElevationMasl());
        for (int i = 0; i < origin.getNeighbors().size; i++) {
            Tile neighbor = origin.getNeighbors().get(i);
            if (neighbor.getPlateId() == origin.getPlateId()
                    && !alreadyAffected.contains(neighbor, false))
                alreadyAffected = adjustElevation(neighbor, intensity, epicenter, alreadyAffected);
        }
        return alreadyAffected;
    }

    private float getCollisionIntensity(Tile a, Tile b, int edgeP1, int edgeP2) {
        Vector3 edge = getVectorFromIndices(edgeP1, edgeP2);
        Vector3 edgeMid = getMidpointFromIndices(edgeP1, edgeP2);
        Vector3 aVel = new Vector3(a.getTangentialVelocity());
        Vector3 bVel = new Vector3(b.getTangentialVelocity());
        Vector3 projAOntoE = edge.cpy().scl(aVel.dot(edge) / edge.dot(edge));
        Vector3 projBOntoE = edge.cpy().scl(bVel.dot(edge) / edge.dot(edge));
        Vector3 rejA = aVel.sub(projAOntoE);
        Vector3 rejB = bVel.sub(projBOntoE);

        float cosBetweenRejections = (rejA.dot(rejB)) / (rejA.len() * rejB.len());
        float rejADotE = rejA.dot(edgeMid.sub(points.get(a.getCentroid())));

        if (cosBetweenRejections > 0) {
            if (rejADotE < 0) {
                return (rejA.len() > rejB.len())
                        ? -(rejA.len() - rejB.len())
                        : (rejB.len() - rejA.len());
            } else {
                return (rejB.len() > rejA.len())
                        ? -(rejB.len() - rejA.len())
                        : (rejA.len() - rejB.len());
            }
        } else {
            return (rejADotE < 0) ? -(rejA.len() + rejB.len()) : (rejA.len() + rejB.len());
        }

        // intensity *= a.getThickness()*a.getArea()*a.getDensity()
        //        + b.getThickness()*b.getArea()*b.getDensity();
        // return intensity;
    }

    private void logMaxIntensity(float intensity) {
        if (Math.abs(intensity) > maxCollisionIntensity) {
            maxCollisionIntensity = intensity;
        }
    }

    private void logMaxElevation(float elevation) {
        if (Math.abs(elevation) > maxElevation) {
            maxElevation = elevation;
        }
    }

    private void addBaseTileAttributes() {
        for (Tile t : tiles) {
            Plate parent = plates.get(t.getPlateId());
            t.setArea((MathUtils.PI * 4.0f * (radius * radius) / tiles.size));
            t.setElevationMasl(0.0f); // 0m above sea level
            t.setTemperature(0.0f); // 0K
            t.setDensity(parent.getDensityGmPerCm3());
            t.setThickness(parent.getThicknessKm());
        }
    }

    private void randomizeTileElevations() {
        // TODO: Add actual randomization / Refactor to add this into plates
        for (Tile t : tiles) {
            if (plates.get(t.getPlateId()).isOceanic()) {
                t.setElevationMasl(-t.getThickness() / 2);
            } else {
                t.setElevationMasl(t.getThickness() / 2);
            }
        }
    }

    private void randomizeTileTemperatures() {
        for (Tile t : tiles) {
            t.setTemperature((float) Math.random() * 300.0f);
        }
    }

    private void generateWind() {
        final int numPoints = 200000;
        wind = new AirParticle[numPoints];
        for (int i = 0; i < numPoints; i++) {
            wind[i] = new AirParticle(radius);
        }
    }

    @SuppressWarnings("java:S1191")
    private void generateSolPower(Sun sun) {
        float k = sun.getTotalPower() / (4.0f * (float) Math.PI); // solar power square law
        float kmToM = 1.0f / 1000.0f;

        for (Tile t : tiles) {
            Vector3 r1 = points.get(t.getCentroid()).sub(this.position).nor();
            Vector3 r2 = new Vector3(sun.getPosition()).sub(this.position).nor();
            Vector3 r3 = new Vector3(sun.getPosition()).sub(t.getCentroid()).nor();

            float areaFractional = Math.max(t.getArea() * (r1.dot(r2)), 0.0f);
            float p = (k / r3.len2()) * areaFractional * kmToM * kmToM;
            t.setPower(p);
        }
    }

    public Tile getNearestTile(float latitude, float longitude) {
        return tileMap.getNearest(latitude, longitude);
    }

    private void scalePoints(Array<Vector3> points, float scale) {
        for (Vector3 p : points) {
            p.nor().scl(scale);
        }
    }

    private int addVertex(Vector3 p) {
        points.add(new Vector3(p));
        return points.size - 1;
    }

    private int getMidpointFromIndicesAndStore(int p1, int p2) {
        Long key = getHashKeyFromIndices(p1, p2);

        if (midpointCache.get(key) != null) {
            return midpointCache.get(key);
        }

        Vector3 u = points.get(p1);
        Vector3 v = points.get(p2);
        Vector3 w = u.cpy().add(v).scl(0.5f);
        int i = addVertex(w);
        midpointCache.put(key, i);
        return i;
    }

    private Vector3 getMidpointFromIndices(int p1, int p2) {
        Vector3 u = points.get(p1);
        Vector3 v = points.get(p2);
        return u.cpy().add(v).scl(0.5f);
    }

    private void addFaceEdgeToNbrCache(Face f, int p1, int p2) {
        Long key = getHashKeyFromIndices(p1, p2);
        Face[] neighbors;
        if (faceNeighbors.get(key) != null) {
            neighbors = faceNeighbors.get(key);
            neighbors[1] = f;
        } else {
            neighbors = new Face[2];
            neighbors[0] = f;
        }
        faceNeighbors.put(key, neighbors);
    }

    private Face getFaceNbr(Face f, int p1, int p2) {
        Face[] neighbors = faceNeighbors.get(getHashKeyFromIndices(p1, p2));
        return f == neighbors[0] ? neighbors[1] : neighbors[0];
    }

    private Tile getTileNbr(Tile t, int p1, int p2) {
        Tile[] pair = tileNeighbors.get(getHashKeyFromIndices(p1, p2));
        return (pair[0].equals(t)) ? pair[1] : pair[0];
    }

    private void addTileEdgeToNbrCache(Tile t, int p1, int p2) {
        Long key = getHashKeyFromIndices(p1, p2);
        Tile[] neighbors;
        if (tileNeighbors.get(key) != null) {
            neighbors = tileNeighbors.get(key);
            neighbors[1] = t;
            tileNeighbors.put(key, neighbors);
            neighbors[0].getNeighbors().add(t);
            neighbors[1].getNeighbors().add(neighbors[0]);
        } else {
            neighbors = new Tile[2];
            neighbors[0] = t;
            tileNeighbors.put(key, neighbors);
        }
    }

    private long getHashKeyFromIndices(int p1, int p2) {
        boolean firstIsSmaller = p1 < p2;
        long smallerIndex = firstIsSmaller ? p1 : p2;
        long greaterIndex = firstIsSmaller ? p2 : p1;
        return (smallerIndex << 32) + greaterIndex;
    }

    public int[] getIndicesFromHashkey(Long key) {
        int[] edge = new int[2];
        edge[1] = (int) (key & 0x00000000FFFFFFFF);
        edge[0] = (int) (key >> 32);
        return edge;
    }

    private Vector3 getVectorFromIndices(int p1, int p2) {
        boolean firstIsSmaller = p1 < p2;
        int smallerIndex = firstIsSmaller ? p1 : p2;
        int greaterIndex = firstIsSmaller ? p2 : p1;
        return new Vector3(points.get(smallerIndex)).sub(points.get(greaterIndex));
    }

    private int getHashKeyFromPlateIDs(int id1, int id2) {
        boolean firstIsSmaller = id1 < id2;
        int smallerIndex = firstIsSmaller ? id1 : id2;
        int greaterIndex = firstIsSmaller ? id2 : id1;
        return (smallerIndex << 16) + greaterIndex;
    }

    public int[] getPlateIDsFromHashKey(Integer key) {
        return new int[] {key >> 16, key & 0x0000FFF};
    }

    private int getCentroidFromIndices(int p0, int p1, int p2) {
        Vector3 u = points.get(p0);
        Vector3 v = points.get(p1);
        Vector3 w = points.get(p2);
        float x = (u.x + v.x + w.x) / 3;
        float y = (u.y + v.y + w.y) / 3;
        float z = (u.z + v.z + w.z) / 3;
        Vector3 c = new Vector3(x, y, z).nor();
        return addVertex(c);
    }

    public Map<Integer, Plate> getPlates() {
        return plates;
    }

    public Map<Long, Float> getTileCollisions() {
        return tileCollisions;
    }

    public Array<Vector3> getPoints() {
        return points;
    }

    public Array<Face> getFaces() {
        return faces;
    }

    public Array<Tile> getTiles() {
        return tiles;
    }

    public float getMaxCollisionIntensity() {
        return maxCollisionIntensity;
    }

    public float getMaxElevation() {
        return maxElevation;
    }

    public AirParticle[] getWind() {
        return wind;
    }
}
