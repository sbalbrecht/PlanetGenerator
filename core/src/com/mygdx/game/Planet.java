package com.mygdx.game;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.mygdx.game.util.Log;
import com.mygdx.game.util.VMath;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

public class Planet{
    public final int PLATE_COUNT = 72;

    public Array<Vector3> points = new Array<Vector3>();
    public Array<Face> faces = new Array<Face>();
    public Array<Tile> tiles = new Array<Tile>();
    public Array<Plate> plates = new Array<Plate>();

    public Map<Long, Integer> midpointCache = new HashMap<Long, Integer>();
    
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
        System.out.println("Plates: " + plates.size);
	}

    public void setFaceNeighbors() {
        for(int i = 0; i < faces.size; i++) {
            for(int j = i+1; j < faces.size; j++) {
                if(faces.get(i).nbrs.size == 3) break;
                if(faces.get(i).testNeighbor(faces.get(j))) {
                    faces.get(i).addNbr(faces.get(j));
                    faces.get(j).addNbr(faces.get(i));
                }
            }
        }
    }

    public void setTileNeighbors() {
        for(int i = 0; i < tiles.size; i++) {
            for(int j = i+1; j < tiles.size; j++) {
                if(tiles.get(i).nbrs.size == 6) break;
                if(tiles.get(i).testNeighbor(tiles.get(j))) {
                    tiles.get(i).addNbr(tiles.get(j));
                    tiles.get(j).addNbr(tiles.get(i));
                }
            }
        }
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
    public void subdivide(int degree) {
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

    public void convertToDual() {
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

    public int getCwPt(Face face, int TileCentroid) {
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

    public void generatePlates() {
        Random r = new Random();
        int id;
        int tileInd;
        boolean taken = false;
        // create initial plates
        // generate 72 random plates
        while(plates.size < PLATE_COUNT) {
            id = r.nextInt(0xffffff);
            tileInd = r.nextInt(tiles.size);
            if(tiles.get(tileInd).plateId != -1) continue;
            for(Plate plate : plates) {
                if(plate.id == id) {
                    taken = true;
                    break;
                }
            }
            if(!taken) {
                plates.add(new Plate(tiles.get(tileInd), id));
            } else {
                continue;
            }
        }
        // flood fill randoms
        for(int i = 0; i < tiles.size*1.6; i++) {
            plates.get(r.nextInt(plates.size)).grow(points);
        }
        // calculate area
        for(Plate plate : plates) {
            plate.calibrateBorder();
        }
        while(plates.size > 8) {
            // find longest plate
            Map<Integer, Integer> numOccurrences = new HashMap<Integer, Integer>();
            Plate longest = plates.get(0);
            float longestRatio = (float)longest.border.size / (float)longest.members.size;
            float newRatio = longestRatio;
            for(int i = 1; i < plates.size; i++) {
                newRatio = (float)plates.get(i).border.size / (float)plates.get(i).members.size;
                if(newRatio > longestRatio) {
                    longest = plates.get(i);
                    longestRatio = newRatio;
                }
            }
//            System.out.printf("%.2f, %.2f\n", longestRatio, newRatio);
            // find its neighbor which takes up most of its border
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
            Plate biggestNbr = longest;
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
            int index = 0;
            for(int i = 0; i < plates.size; i++) {
                if(plates.get(i).id == plateId) {
                    biggestNbr = plates.get(i);
                    index = i;
                    break;
                }
            }
            // absorb that neighbor if their combined area < 20% of globe
//            if((biggestNbr.members.size + longest.members.size) / tiles.size > 0.3) {
                for (Tile t : biggestNbr.members) {
                    t.plateId = longest.id;
                    longest.members.add(t);
                }
                plates.removeIndex(index);
                longest.calibrateBorder();
//            }
        }

        // flood fill
//        float roll;
//        for(int i = 0; i < tiles.size*1.2; i++) {
//            roll = r.nextFloat()*100;
//            if(roll < 2f) {
//                plates.get(r.nextInt(54) + 18).grow();
//            } else if(roll < 14.7f) {
//                plates.get(r.nextInt(10) + 8).grow();
//            } else
//                plates.get(r.nextInt(7)).grow();
//        }
    }
    
    public void addBaseAttributes(){
        for (Tile t : tiles){
            t.setElevation(0.0f);   //0m above sea level
            t.setTemperature(0.0f); //0K
        }
    }
    
    public void randomizeElevations(){
        for (Tile t : tiles){
            t.setElevation((0.5f - (float)Math.random())*100.0f);
        }
    }
    public void randomizeTemperatures(){
        for (Tile t : tiles){
            t.setTemperature((float)Math.random()*300.0f);
        }
    }
    
    public void generateSolPower(Sun S){

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

    private int addVertex(Vector3 p)
    {
        float length = (float)Math.sqrt(p.x * p.x + p.y * p.y + p.z * p.z);
        points.add(new Vector3(p.x/length, p.y/length, p.z/length));
        return points.size - 1;
    }

    public int mid(int p1, int p2) {
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
