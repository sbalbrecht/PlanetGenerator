package com.mygdx.game;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.mygdx.game.util.Log;
import com.mygdx.game.util.Units;

import java.util.*;

public class Planet {
    public int PLATE_COUNT = 72;
    private float radius;
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
    public Map<Long, Float> plateCollisions = new HashMap<Long, Float>();
    
    public TileMap tileMap;

    Planet(Vector3 position, float radius, int subdivisions) {
        this.radius = radius;
        this.position = position;
        generateIcosphere(subdivisions);
    }

	private void generateIcosphere(int subdivisions){
        Log log = new Log();

        generateIcosahedron();
		
		log.start("Subdivision");
		    subdivide(subdivisions);

        log.start("Dual Conversion");
            convertToTruncatedIcosphere();

        log.start("Plate generation");
            generatePlates();

        log.start("Assign Attributes");
            addBaseAttributes();
            randomizeElevations();
            randomizeTemperatures();
            generateSolPower(new Sun(new Vector3( 150000000.0f, 0,  0),  3.8679289e20f));
        
        Log.log("Tile 0 attributes:\n" + tiles.get(0).getAttributes());

        scalePoints(points, radius);
    
        log.start("Assemble tileMap");
            tileMap = new TileMap(tiles);
        log.end();
    
        System.out.println("Faces:  " + faces.size);
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
    
    public Tile getNearestLatLong(float latitude, float longitude){
        return this.tileMap.getNearest(latitude, longitude, points);
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

    private Face[] subdivideFace(Face face) {
        int p0 = face.pts[0];
        int p1 = face.pts[1];
        int p2 = face.pts[2];

        int m0 = getMidpointFromIndices(p0, p1);
        int m1 = getMidpointFromIndices(p1, p2);
        int m2 = getMidpointFromIndices(p2, p0);

        return new Face[] {
            new Face(p0, m0, m2, getCentroidFromIndices(p0, m0, m2)),
            new Face(p1, m1, m0, getCentroidFromIndices(p1, m1, m0)),
            new Face(p2, m2, m1, getCentroidFromIndices(p2, m2, m1)),
            new Face(m0, m1, m2, getCentroidFromIndices(m0, m1, m2))
        };
    }

    private void setFaceNeighbors(Face[] faces) {
        for(Face f : faces) {
            for(int j = 0; j < f.pts.length; j++) {
                if(j + 1 == f.pts.length)
                    addFaceEdgeToNbrCache(f, f.pts[j], f.pts[0]);
                else
                    addFaceEdgeToNbrCache(f, f.pts[j], f.pts[j + 1]);
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
            if(i + 1 == t.pts.size)
                addTileEdgeToNbrCache(t, t.pts.get(i), t.pts.get(0));
            else
                addTileEdgeToNbrCache(t, t.pts.get(i), t.pts.get(i + 1));
        }
    }

    private void generatePlates() {
        placePlateRoots();
        floodFillPlates();
        updatePlateBorders();
        removeLongestPlates(8);
        updatePlateBorders();
        // TODO: Place minor and micro plates along the borders of the majors
        assignPlateAttributes();
        calculatePlateCollisions();
    }

    private void placePlateRoots() {
        Random r = new Random();
        int newPlateId;
        int tileIndex;
        while(plates.size() < PLATE_COUNT) {
            newPlateId = r.nextInt(0xffffff);
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
            if(mergePlates(longest, biggestNbr))
                availPlates.remove(biggestNbr.id);
            else
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
        Map<Integer, Integer> numOccurrences = new HashMap<Integer, Integer>();
        for(Tile bdr : sourcePlate.border) {
            for(Tile nbr : bdr.nbrs) {
                if(nbr.plateId != sourcePlate.id) {
                    try {
                        numOccurrences.put(nbr.plateId, numOccurrences.get(nbr.plateId)+1);
                    } catch (NullPointerException e) {
                        numOccurrences.put(nbr.plateId, 1);
                    }
                }
            }
        }
        int plateId = sourcePlate.id;
        int bigNbrOccurrences = 0;
        for (Map.Entry<Integer, Integer> entry : numOccurrences.entrySet()) {
            Integer key = entry.getKey();
            Integer value = entry.getValue();
            if(value > bigNbrOccurrences) {
                bigNbrOccurrences = value;
                plateId = key;
            }
        }
        return plates.get(plateId);
    }

    private boolean mergePlates(Plate primary, Plate secondary) {
        if(primary == secondary) return false;
        float percentArea = (float)(primary.members.size + secondary.members.size)/(float)tiles.size;
        if(percentArea <= 0.25) {
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
            return true;
        } else {
            return false;
        }
    }

    private void assignPlateAttributes() {
        // Assign class of plate (oceanic/continental)
        // Assign random axis of rotation and velocity$cm_year to each plate
        
        Random r = new Random();
        Vector3 tmp1 = new Vector3();
        
        float massConversionConstant = (float)Math.pow(Units.KM_TO_CM, 3.0f) * Units.G_TO_MTONS;
        
        int continentalCount = plates.size()/2;
        for(Plate plate : plates.values()) {
            if(continentalCount > 0) {
                plate.oceanic = false;
                continentalCount--;
            }
    
            plate.centerOfMass = new Vector3();
            for (Tile t: plate.members){
                plate.centerOfMass.add(points.get(t.centroid));
            }
            plate.centerOfMass.scl(plate.members.size);
            
            plate.speed_cm_yr = r.nextFloat()*10f;
            plate.angularSpeed_rad_yr = plate.speed_cm_yr / radius; //TINY
            
            plate.axisOfRotation = new Vector3().setToRandomDirection().scl(radius);
            plate.angularVelocity = new Vector3(plate.axisOfRotation).scl(plate.angularSpeed_rad_yr);
            
            plate.tangentialVelocity = new Vector3();
            
            plate.angularVelocity = new Vector3();
            
            //plate.tangentialVelocity.add(plate.centerOfMass).scl(MathUtils.cos(plate.angularSpeed_rad_yr));
            //plate.tangentialVelocity.add(new Vector3(plate.axisOfRotation).crs(plate.centerOfMass).scl(MathUtils.sin(plate.angularSpeed_rad_yr)));
            //plate.tangentialVelocity.add(new Vector3(plate.centerOfMass).scl());
            
            
            if(plate.oceanic){
                plate.density_gm_cm3 = (float)2.5 + r.nextFloat()/2;
                plate.thickness_km = 3.0f + r.nextFloat()*8.0f;
            }
            else{
                plate.density_gm_cm3 = (float)2.0 + r.nextFloat()/2;
                plate.thickness_km = 5.0f + r.nextFloat()*70.0f;
            }
            
            plate.area_km2 =
                    plate.members.size
                    * (MathUtils.PI*4.0f*(radius*radius)/tiles.size);
            
            plate.mass_Mg =
                    plate.density_gm_cm3
                    * plate.thickness_km
                    * plate.area_km2
                    * massConversionConstant;
            
            
            
            //TODO
            //plate.momentum_cmMg_yr = plate.mass_Mg * plate.speed_cm_yr;
            
            
            
        }
    }

    private void calculatePlateCollisions() {
        Plate nbrPlate;
        Tile bdrNbr;
        Long edgeKey;
        int edgeP1, edgeP2;
        for (Plate plate : plates.values()) {
            for (Tile bdr : plate.border) {
                // for each edge, if it's a border edge, check existence
                // if null, store collision info
                for(int i = 0; i < bdr.pts.size; i++) {
                    edgeP1 = bdr.pts.get(i);
                    if(i + 1 < bdr.pts.size) {
                        edgeP2 = bdr.pts.get(i + 1);
                    } else {
                        edgeP2 = bdr.pts.get(0);
                    }
                    edgeKey = getHashKeyFromIndices(edgeP1, edgeP2);
                    bdrNbr = getTileNbr(bdr, edgeP1, edgeP2);
                    if(bdrNbr.plateId != bdr.plateId) {
                        nbrPlate = plates.get(bdrNbr.plateId);
                        try {
                            plateCollisions.get(edgeKey);
                        } catch (NullPointerException e) {
                            // TODO: calculate coll info and store
                        }
                    }
                }
            }
        }
    }
    
    private void addBaseAttributes(){
        for (Tile t : tiles){
            t.setArea((MathUtils.PI*4.0f*(radius*radius)/tiles.size));
            t.setElevation(0.0f);   //0m above sea level
            t.setTemperature(0.0f); //0K
        }
    }
    
    private void randomizeElevations(){
        for (Tile t : tiles){
            t.setElevation((0.5f - (float)Math.random())*100.0f);
        }
    }
    private void randomizeTemperatures(){
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

            area_fractional = t.area.getValue()*(r1.dot(r2));

            area_fractional = (area_fractional < 0.0f) ? 0.0f : area_fractional;
            p = (k/r3.len2())*area_fractional*km_m*km_m;
            t.power.setValue(p);
        }
    }

    private void scalePoints(Array<Vector3> points, float scale) {
        for (Vector3 p : points){
            p.nor().scl(scale);
        }
    }

    private int addVertex(Vector3 p) {
//        float length = (float)Math.sqrt(p.x * p.x + p.y * p.y + p.z * p.z);
//        points.add(new Vector3(p.x/length, p.y/length, p.z/length));
        points.add(new Vector3(p));
        return points.size - 1;
    }

    private int getMidpointFromIndices(int p1, int p2) {
        Long key = getHashKeyFromIndices(p1, p2);
        int i;
        try {
            i = midpointCache.get(key);
        } catch (NullPointerException e) {
            Vector3 u = points.get(p1);
            Vector3 v = points.get(p2);
            Vector3 w = u.cpy().add(v).scl(0.5f);

            i = addVertex(w);
            midpointCache.put(key, i);
        }
        return i;
    }

    private void addFaceEdgeToNbrCache(Face f, int p1, int p2) {
        Long key = getHashKeyFromIndices(p1, p2);
        Face[] nbrs;
        try {
            nbrs = faceNbrs.get(key);
            nbrs[1] = f;
        } catch (NullPointerException e) {
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
        try {
            nbrs = tileNbrs.get(key);
            nbrs[1] = t;
            tileNbrs.put(key, nbrs);
            nbrs[0].nbrs.add(t);
            nbrs[1].nbrs.add(nbrs[0]);
        } catch (NullPointerException e) {
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
