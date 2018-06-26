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
//    public Array<Plate> plates = new Array<Plate>();

    public Map<Long, Integer> midpointCache = new HashMap<Long, Integer>();
    public Map<Integer, Plate> plates = new HashMap<Integer, Plate>();
    
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
                new Face(0,  8,  1, points),
                new Face(0,  5,  4, points),
                new Face(0, 10,  5, points),
                new Face(0,  4,  8, points),
                new Face(0,  1, 10, points),
                new Face(1,  8,  6, points),
                new Face(1,  6,  7, points),
                new Face(1,  7, 10, points),
                new Face(2, 11,  3, points),
                new Face(2,  9,  4, points),
                new Face(2,  4,  5, points),
                new Face(2,  3,  9, points),
                new Face(2,  5, 11, points),
                new Face(3,  7,  6, points),
                new Face(3, 11,  7, points),
                new Face(3,  6,  9, points),
                new Face(4,  9,  8, points),
                new Face(5, 10, 11, points),
                new Face(6,  8,  9, points),
                new Face(7, 11, 10, points)
		 );
		
		Log l = new Log();
		
		l.start("Subdivision time");
		subdivide(subdivisions);
        l.end();
        
        l.start("Set Face Neighbors");
        setFaceNeighbors();
        l.end();
        
        l.start("Dual Conversion");
            convertToDual();
        l.end();

        l.start("Set Tile Neighbors");
            setTileNeighbors();
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
        for(Tile tile : tiles) {
            for(Vector3 p : tile.pts) {
                p.nor().scl(scale);
            }
        }
        System.out.println("Faces:  " + faces.size);
        System.out.println("Tiles:  " + tiles.size);
        System.out.println("Plates: " + plates.size());
	}

    //BROKEN:
	/*void randomizeTopography(){
    	Face tempFc;
    	for (int i = 0; i < faces.size; i++){
			tempFc = faces.get(i);
			for (int j = 0; j < tempFc.pts.length; j++){
				tempFc.pts[j].scl(1.0f + 0.1f*(float)Math.random());
			}
		}
	}*/

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

				Face f0 = new Face(p0, q0, q2, points);
				Face f1 = new Face(p1, q1, q0, points);
				Face f2 = new Face(p2, q2, q1, points);
				Face f3 = new Face(q0, q1, q2, points);
				if(i == degree-1) {
                    f3.addNbr(f0);
                    f3.addNbr(f1);
                    f3.addNbr(f2);
                    f0.addNbr(f3);
                    f1.addNbr(f3);
                    f2.addNbr(f3);
                }
				newFaces.addAll(f0, f1, f2, f3);

            }
            // set faces = newFaces
			faces.clear();
            faces.ensureCapacity(newFaces.size);
            faces.addAll(newFaces);
        }
    }

    private void setFaceNeighbors() {
        Face a;
        Face b;
        for(int i = 0; i < faces.size; i++) {
            a = faces.get(i);
            for(int j = i+1; j < faces.size; j++) {
                b = faces.get(j);
                if(a.nbrs.size == 3) break;
                if(a.testNeighbor(b)) {
                    a.addNbr(b);
                    b.addNbr(a);
                }
            }
        }
    }

    private void setTileNeighbors() {
        Tile a;
        Tile b;
        for(int i = 0; i < tiles.size; i++) {
            a = tiles.get(i);
            for(int j = i+1; j < tiles.size; j++) {
                b = tiles.get(j);
                if(a.nbrs.size == 6) break;
                if(a.testNeighbor(b)) {
                    a.addNbr(b);
                    b.addNbr(a);
                }
            }
        }
    }

    private void convertToDual() {
        Array<Vector3> pts = new Array<Vector3>();  // Array for Tile points
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
            tiles.add(new Tile(p1, pts));
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

            // absorb that neighbor
            // TODO: if their combined area < 25% of globe; keep track of 2nd longest for this
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
//                System.out.println("BLOOD FOR THE BLOOD GOD: " + longest.members.size + " " + biggestNbr.members.size );

            }
        }
        // recreate borders
        for (Plate plate : plates.values()) {
            plate.border.clear();
            plate.createBorder();
        }
        // Place 64 minor and micro plates along the borders of the majors
        for(Integer key : plates.keySet()) {
            System.out.println(plates.get(key).border.size);
        }


        while(newPlates.size() < PLATE_COUNT - plates.size()) {
            Tile t;
            id = r.nextInt(0xffffff);
            if(plates.get(id) != null || newPlates.get(id) != null) continue;
            // need random border tile from random major plate
            keysArray = new ArrayList<Integer>(plates.keySet());
            int rId = keysArray.get(r.nextInt(keysArray.size()));
            Plate rPlate = plates.get(rId);
            // random border tile
            t = rPlate.border.get(r.nextInt(rPlate.border.size));
            if(r.nextFloat() < 0.2) { // border tile with two different neighbor plates
                Array<Integer> nbrPlates = new Array<Integer>();
                for(Tile bdr : rPlate.border) {
                    int count = 0;
                    for(Tile nbr : bdr.nbrs) {
                        if(nbr.plateId != bdr.plateId && !nbrPlates.contains(nbr.plateId, true)) {
                            nbrPlates.add(nbr.plateId);
                            count++;
                        }
                    }
                    if(count == 2) {
                        t = bdr;
                        rPlate.members.removeValue(t, false);
                        break;
                    }
                }
            }
            newPlates.put(id, new Plate(t, id));

        }
        // flood fill new plates... need new/modified grow algo
        keysArray = new ArrayList<Integer>(newPlates.keySet());
        for(int i = 0; i < tiles.size*.5; i++) {
            newPlates.get(keysArray.get(r.nextInt(keysArray.size()))).grow(points, newPlates);
        }
        // add newPlates to plates
        plates.putAll(newPlates);
        newPlates.clear();
        // recalibrate all plate borders
        for (Plate plate : plates.values()) {
            plate.calibrateBorder();
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
        boolean firstIsSmaller = p1 < p2;
        Long smallerIndex = (long)(firstIsSmaller ? p1 : p2);
        Long greaterIndex = (long)(firstIsSmaller ? p2 : p1);
        Long key = (smallerIndex << 32) + greaterIndex;
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


}
