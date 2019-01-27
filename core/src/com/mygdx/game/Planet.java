package com.mygdx.game;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.mygdx.game.util.Log;
import com.mygdx.game.util.Units;
import com.mygdx.game.util.VMath;

import java.util.*;

import static com.mygdx.game.util.Units.KM_TO_CM;

public class Planet {
    public int PLATE_COUNT = 72;
    public int plateCollisionTimeStepInMillionsOfYears = 50;
    public int subdivisions;
    private float radius;
    float max_collision_intensity = 0f;
    float max_elevation = 0f;
    Vector3 position;
    Vector3 NORTH = new Vector3(0, 1, 0);

    public Array<Vector3> points = new Array<Vector3>();
    public Array<Face> faces = new Array<Face>();
    public Array<Tile> tiles = new Array<Tile>();
    public Array<Tile> tiles_latitude = new Array<Tile>();  //tiles by latitude
    public Array<Tile> tiles_longitude = new Array<Tile>(); //tiles by longitude

    private Map<Long, Integer> midpointCache = new HashMap<Long, Integer>();
    private Map<Long, Face[]> faceNbrs = new HashMap<Long, Face[]>();
    private Map<Long, Tile[]> tileNbrs = new HashMap<Long, Tile[]>();
    public Map<Integer, Plate> plates = new HashMap<Integer, Plate>();
    public Map<Long, Float> tileCollisions = new HashMap<Long, Float>();
    public Map<Long, Integer> plateCollisions = new HashMap<Long, Integer>();

    public TileMap tileMap;

    Planet(Vector3 position, float radius, int subdivisions) {
        this.radius = radius;
        this.position = position;
        this.subdivisions = subdivisions;
        generateIcosphere(subdivisions);
    }

	private void generateIcosphere(int subdivisions){
        Log log = new Log();

        generateIcosahedron();
		
		log.start("Subdivision");
		    subdivide(subdivisions);

        log.start("Dual Conversion");
            convertToTruncatedIcosphere();

//            System.out.println(points.get(tiles.get(0).centroid).dst(points.get(tiles.get(0).nbrs.get(0).centroid)));

        log.start("Plate generation");
            generatePlates();
        log.end();

        scalePoints(points, radius);
    
        log.start("Assemble tileMap");
            tileMap = new TileMap(tiles);
        log.end();
    
//        System.out.println("Faces:  " + faces.size);
        System.out.println("Tiles:  " + tiles.size);
        System.out.println("Plates: " + plates.size());
	}

	private void generateIcosahedron() {
        float phi = (float)((1.0f + Math.sqrt(5.0f))/2.0f);
        float u = 1.0f/(float)Math.sqrt(phi*phi + 1.0f);
        float v = phi*u;

        addVertex(new Vector3(0.0f,   +v,   +u));
        addVertex(new Vector3(0.0f,   +v,   -u));
        addVertex(new Vector3(0.0f,   -v,   +u));
        addVertex(new Vector3(0.0f,   -v,   -u));
        addVertex(new Vector3(+u,   0.0f,   +v));
        addVertex(new Vector3(-u,   0.0f,   +v));
        addVertex(new Vector3(+u,   0.0f,   -v));
        addVertex(new Vector3(-u,   0.0f,   -v));
        addVertex(new Vector3(+v,     +u, 0.0f));
        addVertex(new Vector3(+v,     -u, 0.0f));
        addVertex(new Vector3(-v,     +u, 0.0f));
        addVertex(new Vector3(-v,     -u, 0.0f));

        faces.addAll(
            new Face(0,  8,  1, getCentroidFromIndices(0,  8,  1)),
            new Face(0,  5,  4, getCentroidFromIndices(0,  5,  4)),
            new Face(0, 10,  5, getCentroidFromIndices(0, 10,  5)),
            new Face(0,  4,  8, getCentroidFromIndices(0,  4,  8)),
            new Face(0,  1, 10, getCentroidFromIndices(0,  1, 10)),
            new Face(1,  8,  6, getCentroidFromIndices(1,  8,  6)),
            new Face(1,  6,  7, getCentroidFromIndices(1,  6,  7)),
            new Face(1,  7, 10, getCentroidFromIndices(1,  7, 10)),
            new Face(2, 11,  3, getCentroidFromIndices(2, 11,  3)),
            new Face(2,  9,  4, getCentroidFromIndices(2,  9,  4)),
            new Face(2,  4,  5, getCentroidFromIndices(2,  4,  5)),
            new Face(2,  3,  9, getCentroidFromIndices(2,  3,  9)),
            new Face(2,  5, 11, getCentroidFromIndices(2,  5, 11)),
            new Face(3,  7,  6, getCentroidFromIndices(3,  7,  6)),
            new Face(3, 11,  7, getCentroidFromIndices(3, 11,  7)),
            new Face(3,  6,  9, getCentroidFromIndices(3,  6,  9)),
            new Face(4,  9,  8, getCentroidFromIndices(4,  9,  8)),
            new Face(5, 10, 11, getCentroidFromIndices(5, 10, 11)),
            new Face(6,  8,  9, getCentroidFromIndices(6,  8,  9)),
            new Face(7, 11, 10, getCentroidFromIndices(7, 11, 10))
        );
    }

    /* subdivides faces n times */
    private void subdivide(int degree) {
        for(int i = 0; i < degree; i++) {
            Array<Face> newFaces = new Array<Face>();
            for(Face face : faces) {
                Face[] subdividedFaces = subdivideFace(face);
                if(i == degree - 1) {
                    setFaceNeighbors(subdividedFaces);
                }
 				newFaces.addAll(subdividedFaces);
            }
			faces.clear();
            faces.ensureCapacity(newFaces.size);
            faces.addAll(newFaces);
        }
        midpointCache = null;
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
        generateSolPower(new Sun(new Vector3( 150000000.0f, 0,  0),  3.8679289e20f));
    }

    private Face[] subdivideFace(Face face) {
        int p0 = face.pts[0];
        int p1 = face.pts[1];
        int p2 = face.pts[2];

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
        for(Face f : faces) {
            for(int i = 0; i < f.pts.length; i++) {
                addFaceEdgeToNbrCache(f, f.pts[i], f.pts[(i + 1) % f.pts.length]);
            }
        }
    }

    private void convertToTruncatedIcosphere() {
        Map<Integer, Boolean> ptsUsedAsTileCentroid = new HashMap<Integer, Boolean>();
        for(Face face : faces) {
            int tileCentroid = face.pts[0];
            if(ptsUsedAsTileCentroid.get(tileCentroid) != null)
                tileCentroid = face.pts[1];
            if(ptsUsedAsTileCentroid.get(tileCentroid) != null)
                continue;

            Tile t = getTileFromFace(face, tileCentroid);
            ptsUsedAsTileCentroid.put(tileCentroid, true);
            setTileNeighbors(t);
            tiles.add(t);
        }
//        faces = null;
    }

    private Tile getTileFromFace(Face initialFace, int tileCentroid) {
        Array<Integer> newTilePts = new Array<Integer>();
        Face currentFace = initialFace;
        do {
            newTilePts.add(currentFace.centroid);
            int clockwisePt = currentFace.getClockwisePt(tileCentroid);
            currentFace = getFaceNbr(currentFace, tileCentroid, clockwisePt);
        } while(currentFace != initialFace);
        return new Tile(tileCentroid, newTilePts, points);
    }

    private void setTileNeighbors(Tile t) {
        for(int i = 0; i < t.pts.size; i++) {
            addTileEdgeToNbrCache(t, t.pts.get(i), t.pts.get((i + 1) % t.pts.size));
        }
    }

    private void placePlateRoots() {
        Random r = new Random();
        int newPlateId;
        int tileIndex;
        while(plates.size() < PLATE_COUNT) {
            newPlateId = (int)r.nextInt(0x7fff);
            tileIndex = r.nextInt(tiles.size);
            if (tiles.get(tileIndex).plateId == -1 && plates.get(newPlateId) == null)
                plates.put(newPlateId, new Plate(tiles.get(tileIndex), newPlateId));
        }
    }

    private void floodFillPlates() {
        Random r = new Random();
        List<Integer> keysArray = new ArrayList<Integer>(plates.keySet());
        for(int i = 0; i < tiles.size*1.6; i++) {
            plates.get(keysArray.get(r.nextInt(keysArray.size()))).grow(points);
        }
    }

    private void updatePlateBorders() {
        for (Plate plate : plates.values()) {
            plate.border.clear();
            plate.createBorder();
        }
    }

    private void removeLongestPlates(int minPlates) {
        Map<Integer, Plate> availPlates = new HashMap<Integer, Plate>(plates);
        while(plates.size() > minPlates) {
            if(availPlates.size() == 0) break;
            Plate longest = getLongestPlate(availPlates);
            Plate biggestNbr = getBiggestNbrPlate(longest);
            // absorb that neighbor if their combined area < 25% global area
            if(biggestNbr != null && isCombinedPlateAreaUnderThreshold(longest, biggestNbr)) {
                mergePlates(longest, biggestNbr);
                availPlates.remove(biggestNbr.id);
            } else
                availPlates.remove(longest.id);
        }
    }

    private Plate getLongestPlate(Map<Integer, Plate> availPlates) {
        ArrayList<Integer> keysArray = new ArrayList<Integer>(availPlates.keySet());
        Plate longest = availPlates.get(keysArray.get(0));
        float longestRatio = (float)longest.border.size / (float)longest.members.size;
        float newRatio;
        Plate newPlate;
        for (Integer key : availPlates.keySet()) {
            newPlate = availPlates.get(key);
            newRatio = (float)newPlate.border.size / (float)newPlate.members.size;
            if (newRatio > longestRatio) {
                longest = newPlate;
                longestRatio = newRatio;
            }
        }
        return longest;
    }

    private Plate getBiggestNbrPlate(Plate sourcePlate) {
        Map<Integer, Integer> numOccurrences = getPlateNeighborsLengthAlongBorder(sourcePlate);
        int plateId = sourcePlate.id;
        int maxOccurrences = 0;
        Integer key;
        Integer value;
        for (Map.Entry<Integer, Integer> entry : numOccurrences.entrySet()) {
            key = entry.getKey();
            value = entry.getValue();
            if(value > maxOccurrences) {
                maxOccurrences = value;
                plateId = key;
            }
        }
        return plates.get(plateId);
    }

    private void mergePlates(Plate primary, Plate secondary) {
        for (Tile t : secondary.members) {
            t.plateId = primary.id;
        }
        secondary.root.root = false;
        plates.remove(secondary.id);
        primary.members.ensureCapacity(secondary.members.size);
        primary.members.addAll(secondary.members);
        primary.border.ensureCapacity(secondary.border.size);
        primary.border.addAll(secondary.border);
        primary.calibrateBorder();
    }

    private boolean isCombinedPlateAreaUnderThreshold(Plate a, Plate b) {
        if(a == b) return false;
        float percentArea = (float)(a.members.size + b.members.size)/(float)tiles.size;
        return (percentArea <= 0.20);
    }

    private Map<Integer, Integer> getPlateNeighborsLengthAlongBorder(Plate p) {
        Map<Integer, Integer> numOccurrences = new HashMap<Integer, Integer>();
        for(Tile bdr : p.border) {
            for(Tile nbr : bdr.nbrs) {
                if(nbr.plateId != p.id) {
                    if(numOccurrences.get(nbr.plateId) != null) {
                        numOccurrences.put(nbr.plateId, numOccurrences.get(nbr.plateId)+1);
                    } else {
                        numOccurrences.put(nbr.plateId, 1);
                    }
                }
            }
        }
        return numOccurrences;
    }

    private void eliminateIsolatedPlates() {
        Array<Plate[]> platesToBeMerged = new Array<Plate[]>();
        for(Plate plate : plates.values()) {
            Map<Integer, Integer> numOccurrences = getPlateNeighborsLengthAlongBorder(plate);
            if(numOccurrences.size() == 1) {
                ArrayList<Integer> keysArray = new ArrayList<Integer>(numOccurrences.keySet());
                Plate parent = plates.get(keysArray.get(0));
                platesToBeMerged.add(new Plate[] {plate, parent});
            }
        }
        for(Plate[] pair : platesToBeMerged) {
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
        Arrays.sort(sortedPlates, new Comparator<Plate>(){
            @Override public int compare(Plate a, Plate b){
                if(a.members.size < b.members.size) return -1;
                if(a.members.size > b.members.size) return 1;
                else return 0;
            }
        });
        return sortedPlates;
    }

    // TODO: Refactor
    private void assignPlateAttributes() {
        // Assign class of plate (oceanic/continental)
        // Assign random axis of rotation and velocity$cm_year to each plate
        
        Random r = new Random();
        Vector3 tmp1 = new Vector3();
        
        float massConversionConstant = (float)Math.pow(KM_TO_CM, 3.0f) * Units.G_TO_MTONS;
        
        int continentalCount = (int)(plates.size()*.65);
        Plate[] sortedPlates = sortPlatesByArea();
        for(int i = 0; i < continentalCount; i++) {
            sortedPlates[i].oceanic = false;
        }

        for(Plate plate : plates.values()) {
    
            plate.centerOfMass = new Vector3();
            for (Tile t: plate.members){
                plate.centerOfMass.add(points.get(t.centroid));
            }
            plate.centerOfMass.scl(1f/plate.members.size);
            
    
            if(plate.oceanic){
                plate.density_gm_cm3 = 2.5f + r.nextFloat()/2.0f;
                plate.thickness_km = 3.0f + r.nextFloat()*8.0f;
            }
            else{
                plate.density_gm_cm3 = 2.0f + r.nextFloat()/2.0f;
                plate.thickness_km = 30.0f + r.nextFloat()*5.0f;
            }
            for (Tile t: plate.members){
                t.setThickness(plate.thickness_km);
            }
    
            plate.area_km2 =
                    plate.members.size
                            * (MathUtils.PI*4.0f*(radius*radius)/tiles.size);

            plate.mass_Mg =
                    plate.density_gm_cm3
                            * plate.thickness_km
                            * plate.area_km2
                            * massConversionConstant;

            plate.speed_m_Ma = r.nextFloat()*10f*Units.CM_YR_TO_M_MA* plateCollisionTimeStepInMillionsOfYears;
            plate.angularSpeed_rad_yr = plate.speed_m_Ma / (radius*KM_TO_CM); //TINY
            
            plate.axisOfRotation = new Vector3().setToRandomDirection();
            
            plate.angularVelocity = new Vector3(plate.axisOfRotation).scl(plate.angularSpeed_rad_yr);
            plate.tangentialVelocity = new Vector3(plate.angularVelocity).crs(plate.centerOfMass);
            plate.momentum_cmMg_yr = plate.speed_m_Ma * plate.mass_Mg;
    
            for (Tile t: plate.members){
                t.tangentialVelocity = new Vector3(plate.angularVelocity).crs(points.get(t.centroid));
            }
            
        }
    }

    private void calculatePlateCollisionIntensities() {
        Plate nbrPlate;
        Tile neighbor;
        Long edgeKey;
        int edgeP1, edgeP2;
        float intensity;
//        max_collision_intensity = 10f * 2.0f *(75.0f * 3.0f * 4.0f * (MathUtils.PI * radius * radius)/(float)tiles.size);
        for (Plate plate : plates.values()) {
            for (Tile borderTile : plate.border) {
                for(int i = 0; i < borderTile.pts.size; i++) {
                    edgeP1 = borderTile.pts.get(i);
                    edgeP2 = borderTile.pts.get((i + 1) % borderTile.pts.size);
                    neighbor = getTileNbr(borderTile, edgeP1, edgeP2);
                    if(neighbor.plateId != borderTile.plateId) {
                        nbrPlate = plates.get(neighbor.plateId);
                        edgeKey = getHashKeyFromIndices(edgeP1, edgeP2);
                        if(tileCollisions.get(edgeKey) != null) {
                            intensity = tileCollisions.get(edgeKey);
                        } else {
                            intensity = getCollisionIntensity(borderTile, neighbor, edgeP1, edgeP2);
                            logMaxIntensity(intensity);
                            tileCollisions.put(edgeKey, intensity);
                            // TODO: determine type of collision with plateCollision map
//                            plateCollisions.put(edgeKey, getHashKeyFromPlateIDs(plate.id, nbrPlate.id));
                        }
                    }
                }
            }
        }
    }

    private void simulateCollisions() {
        int propagationLimit = (int)(Math.ceil(0.0625*Math.exp(0.6931*subdivisions)));
        for (Plate plate : plates.values()) {
            for (Tile bdr : plate.border) {
                float sumOfIntensitiesActingOnTile = sumIntensities(bdr);
                Vector3 epicenter = getCentroidOfCollision(bdr);
                adjustElevation(bdr, sumOfIntensitiesActingOnTile, epicenter, new Array<Tile>());
            }
        }
    }

    private float sumIntensities(Tile t) {
        float sum = 0;
        for(int i = 0; i < t.pts.size; i++) {
            Long key = getHashKeyFromIndices(t.pts.get(i), t.pts.get((i + 1) % t.pts.size));
            if(tileCollisions.get(key) != null) {
                sum += tileCollisions.get(key);
            }
        }
        return sum;
    }

    private Vector3 getCentroidOfCollision(Tile t) {
        Array<Vector3> edgePoints = new Array<Vector3>();
        for(int i = 0; i < t.pts.size; i++) {
            int edgeP1 = t.pts.get(i);
            int edgeP2 = t.pts.get((i + 1) % t.pts.size);
            Long edgeKey = getHashKeyFromIndices(edgeP1, edgeP2);
            if(tileCollisions.get(edgeKey) != null) {
                edgePoints.addAll(points.get(edgeP1), points.get(edgeP2));
            }
        }
        return VMath.centroid(edgePoints);
    }

    private Array<Tile> adjustElevation(Tile origin, float intensity, Vector3 epicenter,
                                        Array<Tile> tilesAlreadyAffected){
        // TODO: need to change elevationChange formula to something more grounded in reality
        // TODO: propagation should be determined by combination of intensity and distance?
        float distanceFromEpicenter = points.get(origin.centroid).dst(epicenter);
        float elevationChange = intensity * (1 / distanceFromEpicenter);
        float propagationLimit = 0.068f;
//        System.out.printf("intensity: %.3f dst: %.3f new Elev: %.3f  tilesAffectedSize: %d\n", intensity, distanceFromEpicenter, elevationChange, tilesAlreadyAffected.size);
        tilesAlreadyAffected.add(origin);
        if(distanceFromEpicenter > propagationLimit || subdivisions < 4) {
            return tilesAlreadyAffected;
        }
        origin.setElevation(origin.getElevation() + elevationChange);
        logMaxElevation(origin.getElevation()); // TODO: find better place to log elevation for this test...
        for(int i = 0; i < origin.nbrs.size; i++) {
            Tile neighbor = origin.nbrs.get(i);
            if(neighbor.plateId == origin.plateId && !tilesAlreadyAffected.contains(neighbor, false))
                tilesAlreadyAffected = adjustElevation(neighbor, intensity, epicenter, tilesAlreadyAffected);
        }
        return tilesAlreadyAffected;
    }

    private float getCollisionIntensity(Tile a, Tile b, int edgeP1, int edgeP2) {
        Vector3 edge = getVectorFromIndices(edgeP1, edgeP2);
        Vector3 edge_mid = getMidpointFromIndices(edgeP1, edgeP2);
        Vector3 a_vel = new Vector3(a.tangentialVelocity);
        Vector3 b_vel = new Vector3(b.tangentialVelocity);
        Vector3 proj_a_onto_e = edge.cpy().scl(a_vel.dot(edge)/edge.dot(edge));
        Vector3 proj_b_onto_e = edge.cpy().scl(b_vel.dot(edge)/edge.dot(edge));
        Vector3 rej_a = a_vel.sub(proj_a_onto_e);
        Vector3 rej_b = b_vel.sub(proj_b_onto_e);

        float cos_between_rejections = (rej_a.dot(rej_b))/(rej_a.len()*rej_b.len());
        float rej_a_dot_e = rej_a.dot(edge_mid.sub(points.get(a.centroid)));

        if (cos_between_rejections > 0) {
            if (rej_a_dot_e < 0) {
                return (rej_a.len() > rej_b.len())
                        ? -(rej_a.len() - rej_b.len())
                        :  (rej_b.len() - rej_a.len());
            } else {
                return (rej_b.len() > rej_a.len())
                        ? -(rej_b.len() - rej_a.len())
                        :  (rej_a.len() - rej_b.len());
            }
        } else {
            return (rej_a_dot_e < 0)
                    ? -(rej_a.len() + rej_b.len())
                    :  (rej_a.len() + rej_b.len());
        }

//        intensity *= a.getThickness()*a.getArea()*a.getDensity()
//                + b.getThickness()*b.getArea()*b.getDensity();
//        return intensity;
    }

    private void logMaxIntensity(float intensity) {
        if(Math.abs(intensity) > max_collision_intensity) {
            max_collision_intensity = intensity;
        }
    }

    private void logMaxElevation(float elevation) {
        if(Math.abs(elevation) > max_elevation) {
            max_elevation = elevation;
        }
    }
    
    private void addBaseTileAttributes() {
        Plate parent;
        for (Tile t : tiles){
            parent = plates.get(t.plateId);
            t.setArea((MathUtils.PI*4.0f*(radius*radius)/tiles.size));
            t.setElevation(0.0f);   //0m above sea level
            t.setTemperature(0.0f); //0K
            t.setDensity(parent.density_gm_cm3);
            t.setThickness(parent.thickness_km);
        }
    }
    
    private void randomizeTileElevations() {
        for (Tile t : tiles){
            t.setElevation((0.5f - (float)Math.random())*100.0f);
        }
    }

    private void randomizeTileTemperatures(){
        for (Tile t : tiles){
            t.setTemperature((float)Math.random()*300.0f);
        }
    }
    
    private void generateSolPower(Sun S){

        float k = S.totalPower/(4.0f*(float)Math.PI); //solar power square law
        float area_fractional;
        Vector3 r1;
        Vector3 r2;
        Vector3 r3;
        float p;

        float km_m = 1.0f/1000.0f;

        for (Tile t : tiles){
            r1 = points.get(t.centroid).sub(this.position).nor();
            r2 = new Vector3(S.position).sub(this.position).nor();
            r3 = new Vector3(S.position).sub(t.centroid).nor();

            area_fractional = t.getArea()*(r1.dot(r2));

            area_fractional = (area_fractional < 0.0f) ? 0.0f : area_fractional;
            p = (k/r3.len2())*area_fractional*km_m*km_m;
            t.setPower(p);
        }
    }

    public Tile getNearestLatLong(float latitude, float longitude){
        return this.tileMap.getNearest(latitude, longitude, points);
    }

    private void scalePoints(Array<Vector3> points, float scale) {
        for (Vector3 p : points){
            p.nor().scl(scale);
        }
    }

    private int addVertex(Vector3 p) {
        points.add(new Vector3(p));
        return points.size - 1;
    }

    private int getMidpointFromIndicesAndStore(int p1, int p2) {
        Long key = getHashKeyFromIndices(p1, p2);
        int i;
        if(midpointCache.get(key) != null) {
            i = midpointCache.get(key);
        } else {
            Vector3 u = points.get(p1);
            Vector3 v = points.get(p2);
            Vector3 w = u.cpy().add(v).scl(0.5f);

            i = addVertex(w);
            midpointCache.put(key, i);
        }
        return i;
    }

    private Vector3 getMidpointFromIndices(int p1, int p2) {
        Vector3 u = points.get(p1);
        Vector3 v = points.get(p2);
        return u.cpy().add(v).scl(0.5f);
    }

    private void addFaceEdgeToNbrCache(Face f, int p1, int p2) {
        Long key = getHashKeyFromIndices(p1, p2);
        Face[] nbrs;
        if(faceNbrs.get(key) != null) {
            nbrs = faceNbrs.get(key);
            nbrs[1] = f;
        } else {
            nbrs = new Face[2];
            nbrs[0] = f;
        }
        faceNbrs.put(key, nbrs);
    }

    private Face getFaceNbr(Face f, int p1, int p2) {
        Face[] nbrs = faceNbrs.get(getHashKeyFromIndices(p1, p2));
        if(f == nbrs[0])
            return nbrs[1];
        else
            return nbrs[0];
    }

    private Tile getTileNbr(Tile t, int p1, int p2) {
        Tile[] pair = tileNbrs.get(getHashKeyFromIndices(p1, p2));
        if(pair[0].equals(t))
            return pair[1];
        else
            return pair[0];
    }

    private void addTileEdgeToNbrCache(Tile t, int p1, int p2) {
        Long key = getHashKeyFromIndices(p1, p2);
        Tile[] nbrs;
        if(tileNbrs.get(key) != null) {
            nbrs = tileNbrs.get(key);
            nbrs[1] = t;
            tileNbrs.put(key, nbrs);
            nbrs[0].nbrs.add(t);
            nbrs[1].nbrs.add(nbrs[0]);
        } else {
            nbrs = new Tile[2];
            nbrs[0] = t;
            tileNbrs.put(key, nbrs);
        }
    }

    private long getHashKeyFromIndices(int p1, int p2) {
        boolean firstIsSmaller = p1 < p2;
        Long smallerIndex = (long)(firstIsSmaller ? p1 : p2);
        Long greaterIndex = (long)(firstIsSmaller ? p2 : p1);
        return (smallerIndex << 32) + greaterIndex;
    }

    public int[] getIndicesFromHashkey(Long key) {
        int[] edge = new int[2];
        edge[1] = (int)(key & 0x00000000FFFFFFFF);
        edge[0] = (int)(key >> 32);
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
        Integer smallerIndex = (int)(firstIsSmaller ? id1 : id2);
        Integer greaterIndex = (int)(firstIsSmaller ? id2 : id1);
        return (smallerIndex << 16) + greaterIndex;
    }

    public int[] getPlateIDsFromHashKey(Integer key) {
        int[] edge = new int[2];
        edge[1] = (int)(key & 0x0000FFFF);
        edge[0] = (int)(key >> 16);
        return edge;
    }

    private int getCentroidFromIndices(int p0, int p1, int p2) {
        Vector3 u = points.get(p0);
        Vector3 v = points.get(p1);
        Vector3 w = points.get(p2);
        Vector3 c = new Vector3(
                (u.x + v.x + w.x)/3,
                (u.y + v.y + w.y)/3,
                (u.z + v.z + w.z)/3).nor();
        
        return addVertex(c);
    }
}
