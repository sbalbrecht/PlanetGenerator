package com.mygdx.game;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.mygdx.game.util.Log;
import com.mygdx.game.util.VMath;

import java.util.*;

public class Planet{
    public int PLATE_COUNT = 72;

    public Array<Vector3> points = new Array<Vector3>();
    public Array<Face> faces = new Array<Face>();
    public Array<Tile> tiles = new Array<Tile>();

    public Map<Long, Integer> midpointCache = new HashMap<Long, Integer>();
    public Map<Long, Face[]> faceNbrs = new HashMap<Long, Face[]>();
    public Map<Long, Tile[]> tileNbrs = new HashMap<Long, Tile[]>();
    public Map<Integer, Plate> plates = new HashMap<Integer, Plate>();
    public Map<Integer, Float> plateCollisions = new HashMap<Integer, Float>();
    
    public TileMap tileMap;
    
    public Array<Tile> tiles_latitude = new Array<Tile>();  //tiles by latitude
    public Array<Tile> tiles_longitude = new Array<Tile>(); //tiles by longitude
    
    private float scale;
    
    Vector3 position;
    Vector3 NORTH;
  
    // create Planet constructor

    Planet() {}

	void generateIcosphere(Vector3 position, float scale, int subdivisions){
		//hardcoded ico shit

		float phi = (float)((1.0f + Math.sqrt(5.0f))/2.0f);
		float u = 1.0f/(float)Math.sqrt(phi*phi + 1.0f);
		float v = phi*u;

		this.scale = scale;
		this.position = position;
		
		NORTH = new Vector3(0, 0, 1);

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
		
		//for (Vector3 p: points){ p.scl(scale);}
		
	    // 20 faces
		faces.addAll(
                new Face(0,  8,  1, getCentroid(0, 8, 1)),
                new Face(0,  5,  4, getCentroid(0, 5, 4)),
                new Face(0, 10,  5, getCentroid(0,10, 5)),
                new Face(0,  4,  8, getCentroid(0, 4, 8)),
                new Face(0,  1, 10, getCentroid(0, 1,10)),
                new Face(1,  8,  6, getCentroid(1, 8, 6)),
                new Face(1,  6,  7, getCentroid(1, 6, 7)),
                new Face(1,  7, 10, getCentroid(1, 7,10)),
                new Face(2, 11,  3, getCentroid(2,11, 3)),
                new Face(2,  9,  4, getCentroid(2, 9, 4)),
                new Face(2,  4,  5, getCentroid(2, 4, 5)),
                new Face(2,  3,  9, getCentroid(2, 3, 9)),
                new Face(2,  5, 11, getCentroid(2, 5,11)),
                new Face(3,  7,  6, getCentroid(3, 7, 6)),
                new Face(3, 11,  7, getCentroid(3,11, 7)),
                new Face(3,  6,  9, getCentroid(3, 6, 9)),
                new Face(4,  9,  8, getCentroid(4, 9, 8)),
                new Face(5, 10, 11, getCentroid(5,10,11)),
                new Face(6,  8,  9, getCentroid(6, 8, 9)),
                new Face(7, 11, 10, getCentroid(7,11,10))
		 );
		
		Log l = new Log();
		
		l.start("Subdivision time");
		subdivide(subdivisions);
        l.end();
        
        l.start("Dual Conversion");
            convertToDual();
        l.end();

        l.start("Plate generation");
            generatePlates();
        l.end();

        l.start("Assign Attributes");
            addBaseAttributes();
            randomizeElevations();
            randomizeTemperatures();
            generateSolPower(new Sun(new Vector3( 150000000.0f, 0,  0),  3.8679289e20f));
        l.end();
        
        Log.log("Tile 0 attributes:\n" + tiles.get(0).getAttributes());

        for (Vector3 p : points){
            p.nor().scl(scale);
        }
    
        l.start("Assemble tileMap");
            tileMap = new TileMap(tiles);
        l.end();
    
        System.out.println("Faces:  " + faces.size);
        System.out.println("Tiles:  " + tiles.size);
        System.out.println("Plates: " + plates.size());
	}
    
    public Tile getNearestLatLong(float latitude, float longitude){
        return this.tileMap.getNearest(latitude, longitude);
    }

    /* subdivides faces n times */
    private void subdivide(int degree) {
        for(int i = 0; i < degree; i++) {
            Array<Face> newFaces = new Array<Face>();

            for(Face face : faces) {
                int p0 = face.pts[0];
                int p1 = face.pts[1];
                int p2 = face.pts[2];
                
                int q0 = mid(p0, p1);
                int q1 = mid(p1, p2);
                int q2 = mid(p2, p0);

				Face f0 = new Face(p0, q0, q2, getCentroid(p0,q0,q2));
				Face f1 = new Face(p1, q1, q0, getCentroid(p1,q1,q0));
				Face f2 = new Face(p2, q2, q1, getCentroid(p2,q2,q1));
				Face f3 = new Face(q0, q1, q2, getCentroid(q0,q1,q2));

				if(i == degree-1) {
                    // for each face's edge, look up existence in map
                    // if it doesn't exist, create it
                    // if it does, add face to the array
                    faceNbrCache(f0, f0.pts[0], f0.pts[1]);
                    faceNbrCache(f0, f0.pts[1], f0.pts[2]);
                    faceNbrCache(f0, f0.pts[2], f0.pts[0]);
                    faceNbrCache(f1, f1.pts[0], f1.pts[1]);
                    faceNbrCache(f1, f1.pts[1], f1.pts[2]);
                    faceNbrCache(f1, f1.pts[2], f1.pts[0]);
                    faceNbrCache(f2, f2.pts[0], f2.pts[1]);
                    faceNbrCache(f2, f2.pts[1], f2.pts[2]);
                    faceNbrCache(f2, f2.pts[2], f2.pts[0]);
                    faceNbrCache(f3, f3.pts[0], f3.pts[1]);
                    faceNbrCache(f3, f3.pts[1], f3.pts[2]);
                    faceNbrCache(f3, f3.pts[2], f3.pts[0]);
                }
				newFaces.addAll(f0, f1, f2, f3);

            }
            // set faces = newFaces
			faces.clear();
            faces.ensureCapacity(newFaces.size);
            faces.addAll(newFaces);
        }
    }

    private void convertToDual() {
        Array<Integer> pts = new Array<Integer>();  // Array for Tile points
        Face curr;
        for(Face face : faces) {
            curr = face;
            int p1 = curr.pts[0];         // Tile centroid
            if(face.ptsUsedAsTileCentroid.contains(p1, false))
                p1 = curr.pts[1];
            if(face.ptsUsedAsTileCentroid.contains(p1, false))
                continue;

            do {
                pts.add(curr.centroid);                     // add current centroid
                int p2 = curr.pts[getCwPt(curr, p1)];       // CW point
                for (Face nbr : curr.nbrs) {                // find CCW neighbor
                    int count = 0;
                    for(int i = 0; i < nbr.pts.length; i++) {
                        if(nbr.pts[i] == p1 || nbr.pts[i] == p2) {
                            count++;
                        }
                    }
                    if(count == 2) {
                        curr = nbr;
                        curr.addPtUsedInTileCentroid(p1);
                        break;
                    }
                }
            } while(curr != face);
            Tile t = new Tile(p1, pts);
            int j;
            for(int i = 0; i < t.pts.size; i++) {
                if(i+1 == t.pts.size) j = 0;
                else j = i+1;
                tileNbrCache(t, pts.get(i), pts.get(j));
            }
            tiles.add(t);
            pts.clear();                                 // clear points for next tile
        }
    }

    private int getCwPt(Face face, int TileCentroid) {
        // Find the index being used for the centroid
        int index = 0;
        for(int i = 0; i < face.pts.length; i++) {
            if(face.pts[i] == TileCentroid) {
                index = i;
                break;
            }
        }
        // Find the index of the next point CW from the centroid index
        if(index + 2 >= face.pts.length) {
            return index - 1;
        } else
            return index + 2;
    }

    private void generatePlates() {
        Random r = new Random();
        int id;
        int tileInd;
        Map<Integer, Integer> numOccurrences = new HashMap<Integer, Integer>();
        Map<Integer, Plate> newPlates = new HashMap<Integer, Plate>();

        // generate 72 random plates
        while(plates.size() < PLATE_COUNT) {
            id = r.nextInt(0xffffff);
            tileInd = r.nextInt(tiles.size);
            if(tiles.get(tileInd).plateId != -1 || plates.get(id) != null) continue;
            else plates.put(id, new Plate(tiles.get(tileInd), id));
        }
        // flood fill randoms
        List<Integer> keysArray = new ArrayList<Integer>(plates.keySet());
        for(int i = 0; i < tiles.size*1.6; i++) {
            plates.get(keysArray.get(r.nextInt(keysArray.size()))).grow(points);
        }
        // establish borders
        for (Plate plate : plates.values()) {
            plate.createBorder();
        }

        // Eliminate longest plates until 8 remain
        Map<Integer, Plate> availPlates = new HashMap<Integer, Plate>(plates);
        while(plates.size() > 8) {
            if(availPlates.size() == 0) break;
            // find plate with greatest border / area ratio
            keysArray = new ArrayList<Integer>(availPlates.keySet());
            Plate longest = availPlates.get(keysArray.get(0));
            float longestRatio = (float)longest.border.size / (float)longest.members.size;
            float newRatio;
            Plate p;
            for (Integer key : availPlates.keySet()) {
                p = availPlates.get(key);
                newRatio = (float)p.border.size / (float)p.members.size;
                if (newRatio > longestRatio) {
                    longest = p;
                    longestRatio = newRatio;
                }
            }
            // find the neighbor which takes up most of its border
            for(Tile t : longest.border) {
                for(Tile nbr : t.nbrs) {
                    if(nbr.plateId != longest.id) {
                        try {
                            numOccurrences.put(nbr.plateId, numOccurrences.get(nbr.plateId)+1);
                        } catch (NullPointerException e) {
                            numOccurrences.put(nbr.plateId, 1);
                        }
                    }
                }
            }
            int plateId = longest.id;
            int bigNbrOccurrences = 0;
            for (Map.Entry<Integer, Integer> entry : numOccurrences.entrySet()) {
                Integer key = entry.getKey();
                Integer value = entry.getValue();
                if(value > bigNbrOccurrences) {
                    bigNbrOccurrences = value;
                    plateId = key;
                }
            }
            Plate biggestNbr = plates.get(plateId);

            // absorb that neighbor if their combined area < 25% global area
            if((float)(longest.members.size + biggestNbr.members.size)/(float)tiles.size > 0.25) {
                availPlates.remove(longest.id);
                numOccurrences.clear();
            } else {
                for (Tile t : biggestNbr.members) {
                    t.plateId = longest.id;
                }
                biggestNbr.root.root = false;
                longest.members.ensureCapacity(biggestNbr.members.size);
                longest.members.addAll(biggestNbr.members);
                longest.border.ensureCapacity(biggestNbr.border.size);
                longest.border.addAll(biggestNbr.border);
                availPlates.remove(plateId);
                plates.remove(plateId);
                longest.calibrateBorder();
                numOccurrences.clear();
            }
        }
        // recreate borders
        for (Plate plate : plates.values()) {
            plate.border.clear();
            plate.createBorder();
        }
//        // Place minor and micro plates along the borders of the majors
//        while(newPlates.size() < PLATE_COUNT - plates.size()) {
//            Tile t;
//            id = r.nextInt(0xffffff);
//            if(plates.get(id) != null || newPlates.get(id) != null) continue;
//            // need random border tile from random major plate
//            keysArray = new ArrayList<Integer>(plates.keySet());
//            int rId = keysArray.get(r.nextInt(keysArray.size()));
//            Plate rPlate = plates.get(rId);
//            // random border tile
//            t = rPlate.border.get(r.nextInt(rPlate.border.size));
//            if(r.nextFloat() < 0.2) { // border tile with two different neighbor plates
//                Array<Integer> nbrPlates = new Array<Integer>();
//                for(Tile bdr : rPlate.border) {
//                    int count = 0;
//                    for(Tile nbr : bdr.nbrs) {
//                        if(nbr.plateId != bdr.plateId && !nbrPlates.contains(nbr.plateId, false)
//                                && newPlates.containsKey(nbr.plateId)) {
//                            nbrPlates.add(nbr.plateId);
//                            count++;
//                        }
//                    }
//                    if(count == 2) {
//                        t = bdr;
//                        break;
//                    }
//                }
//            }
//            rPlate.members.removeValue(t, false);
//            rPlate.border.removeValue(t, false);
//            newPlates.put(id, new Plate(t, id, newPlates));
//
//        }
//        // flood fill new plates
//        keysArray = new ArrayList<Integer>(newPlates.keySet());
//        float roll;
//        for(int i = 0; i < tiles.size*.1; i++) {
//            roll = r.nextInt(100);
//            if(roll < 75) {
//                newPlates.get(keysArray.get(r.nextInt(10))).grow(points, newPlates);
//            }
//            else {
//                newPlates.get(keysArray.get(r.nextInt(keysArray.size()-10)+10)).grow(points, newPlates);
//            }
//        }
//        // add newPlates to plates
//        plates.putAll(newPlates);
//        newPlates.clear();
//        // recalibrate all plate borders
//        for (Plate plate : plates.values()) {
//            for(Tile mem : plate.members) {
//                if(mem.plateId != plate.id) {
//                    plate.members.removeValue(mem, false);
//                }
//            }
//            plate.border.clear();
//            plate.createBorder();
//        }

        // Assign class of plate (oceanic/continental)
        // Assign random axis of rotation and velocity to each plate
        int continentalCount = plates.size()/2;
        for(Plate plate : plates.values()) {
            if(continentalCount > 0) {
                plate.oceanic = false;
                continentalCount--;
            }
            plate.velocity = r.nextFloat()*10;
            plate.rotation = new Vector3(r.nextFloat(), r.nextFloat(), r.nextFloat()).nor().scl(scale);
            if(plate.oceanic)
                plate.density = (float)2.5 + r.nextFloat()/2;
            else
                plate.density = (float)2.0 + r.nextFloat()/2;
        }

        // Calculate collision data for every border tile, store in map
        {
        Tile t;
        Long key;
        int j;
        Tile[] pair;
        for (Plate plate : plates.values()) {
            for (Tile bdr : plate.border) {
                // for each edge, if it's a border edge, check existence
                // if null, store collision info
                for(int i = 0; i < bdr.pts.size; i++) {
                    if(i + 1 < bdr.pts.size)
                        j = i + 1;
                    else
                        j = 0;
                    key = getKey(bdr.pts.get(i), bdr.pts.get(j));
                    pair = tileNbrs.get(key);
                    if(pair[0].equals(bdr))
                        t = pair[1];
                    else
                        t = pair[0];
                    if(t.plateId != bdr.plateId) {
                        try {
                            plateCollisions.get(key);
                        } catch (NullPointerException e) {
                            // TODO: calculate coll info and store
                        }
                    }
                }
            }
        }
        }
    }
    
    private void addBaseAttributes(){
        for (Tile t : tiles){
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
            t.area.setValue((float)Math.PI*4.0f*scale/tiles.size);

            r1 = points.get(t.centroid).sub(this.position).nor();
            r2 = new Vector3(S.position).sub(this.position).nor();
            r3 = new Vector3(S.position).sub(t.centroid).nor();

            area_fractional = t.area.getValue()*(r1.dot(r2));

            area_fractional = (area_fractional < 0.0f) ? 0.0f : area_fractional;
            p = (k/r3.len2())*area_fractional*km_m*km_m;
            t.power.setValue(p);
        }
    }

    private int addVertex(Vector3 p) {
//        float length = (float)Math.sqrt(p.x * p.x + p.y * p.y + p.z * p.z);
//        points.add(new Vector3(p.x/length, p.y/length, p.z/length));

        points.add(new Vector3(p));

        return points.size - 1;
    }

    private int mid(int p1, int p2) {
        Long key = getKey(p1, p2);
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

    private void faceNbrCache(Face f, int p1, int p2) {
        Long key = getKey(p1, p2);
        Face[] fArr;
        try {
            fArr = faceNbrs.get(key);
            fArr[1] = f;
            faceNbrs.put(key, fArr);
            fArr[0].nbrs.add(f);
            fArr[1].nbrs.add(fArr[0]);
        } catch (NullPointerException e) {
            fArr = new Face[2];
            fArr[0] = f;
            faceNbrs.put(key, fArr);
        }
    }

    private void tileNbrCache(Tile t, int p1, int p2) {
        Long key = getKey(p1, p2);
        Tile[] tArr;
        try {
            tArr = tileNbrs.get(key);
            tArr[1] = t;
            tileNbrs.put(key, tArr);
            tArr[0].nbrs.add(t);
            tArr[1].nbrs.add(tArr[0]);
        } catch (NullPointerException e) {
            tArr = new Tile[2];
            tArr[0] = t;
            tileNbrs.put(key, tArr);
        }
    }

    private void plateColCache(Tile t, int p1, int p2) {

    }

    private long getKey(int p1, int p2) {
        boolean firstIsSmaller = p1 < p2;
        Long smallerIndex = (long)(firstIsSmaller ? p1 : p2);
        Long greaterIndex = (long)(firstIsSmaller ? p2 : p1);
        return (smallerIndex << 32) + greaterIndex;
    }

    private int getCentroid(int p0, int p1, int p2) {
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
