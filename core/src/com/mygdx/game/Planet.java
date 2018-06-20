package com.mygdx.game;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.mygdx.game.util.Log;
import com.mygdx.game.util.VMath;

import java.util.Random;

public class Planet{
    public final int PLATE_COUNT = 72;

    public Array<Vector3> points = new Array<Vector3>();
    public Array<Face> faces = new Array<Face>();
    public Array<Tile> tiles = new Array<Tile>();
    public Array<Plate> plates = new Array<Plate>();
    
    private float scale;
    
    Vector3 position;
  
    // create Planet constructor

    Planet() {}

	void generateIcosphere(Vector3 position, float scale, int subdivisions){
		//hardcoded ico shit

		float phi = (float)((1.0f + Math.sqrt(5.0f))/2.0f);
		float u = 1.0f/(float)Math.sqrt(phi*phi + 1.0f);
		float v = phi*u;

		this.scale = scale;
		this.position = position;

		// Points are scaled x10 so the camera is more flexible

		points.addAll(
                new Vector3(0.0f,   +v,   +u),
                new Vector3(0.0f,   +v,   -u),
                new Vector3(0.0f,   -v,   +u),
                new Vector3(0.0f,   -v,   -u),
                new Vector3(+u,   0.0f,   +v),
                new Vector3(-u,   0.0f,   +v),
                new Vector3(+u,   0.0f,   -v),
                new Vector3(-u,   0.0f,   -v),
                new Vector3(+v,     +u, 0.0f),
                new Vector3(+v,     -u, 0.0f),
                new Vector3(-v,     +u, 0.0f),
                new Vector3(-v,     -u, 0.0f)
			);
		
		//for (Vector3 p: points){ p.scl(scale);}
		
	    // 20 faces
		faces.addAll(
                new Face(points.get(0), points.get( 8),  points.get( 1)),
                new Face(points.get(0), points.get( 5),  points.get( 4)),
                new Face(points.get(0), points.get(10),  points.get( 5)),
                new Face(points.get(0), points.get( 4),  points.get( 8)),
                new Face(points.get(0), points.get( 1),  points.get(10)),
                new Face(points.get(1), points.get( 8),  points.get( 6)),
                new Face(points.get(1), points.get( 6),  points.get( 7)),
                new Face(points.get(1), points.get( 7),  points.get(10)),
                new Face(points.get(2), points.get(11),  points.get( 3)),
                new Face(points.get(2), points.get( 9),  points.get( 4)),
                new Face(points.get(2), points.get( 4),  points.get( 5)),
                new Face(points.get(2), points.get( 3),  points.get( 9)),
                new Face(points.get(2), points.get( 5),  points.get(11)),
                new Face(points.get(3), points.get( 7),  points.get( 6)),
                new Face(points.get(3), points.get(11),  points.get( 7)),
                new Face(points.get(3), points.get( 6),  points.get( 9)),
                new Face(points.get(4), points.get( 9),  points.get( 8)),
                new Face(points.get(5), points.get(10),  points.get(11)),
                new Face(points.get(6), points.get( 8),  points.get( 9)),
                new Face(points.get(7), points.get(11),  points.get(10))
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
            generateSolPower(new Sun(new Vector3(20.0f, 20.0f, 20.0f), 40000.0f));
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
//        System.out.println("Faces:  " + faces.size);
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
            Array<Vector3> newPoints = new Array<Vector3>();
            
            for(Face face : faces) {
                Vector3 p0 = face.pts[0];
                Vector3 p1 = face.pts[1];
                Vector3 p2 = face.pts[2];
                
                Vector3 q0 = VMath.mid(p0, p1);
                Vector3 q1 = VMath.mid(p1, p2);
                Vector3 q2 = VMath.mid(p2, p0);

                if(!newPoints.contains(q0, false)){ newPoints.add(q0); }
					else { q0 = newPoints.get(newPoints.indexOf(q0, false)); }
				if(!newPoints.contains(q1, false)){ newPoints.add(q1); }
					else { q1 = newPoints.get(newPoints.indexOf(q1, false)); }
				if(!newPoints.contains(q2, false)){ newPoints.add(q2); }
					else { q2 = newPoints.get(newPoints.indexOf(q2, false)); }

				Face f0 = new Face(p0, q0, q2);
				Face f1 = new Face(p1, q1, q0);
				Face f2 = new Face(p2, q2, q1);
				Face f3 = new Face(q0, q1, q2);
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
            points.addAll(newPoints);
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

            Vector3 p1 = curr.pts[0];         // Tile centroid
            if(face.ptsUsedAsTileCentroid.contains(p1, false))
                p1 = curr.pts[1];
            if(face.ptsUsedAsTileCentroid.contains(p1, false))
                continue;

            do {
                pts.add(curr.centroid);                     // add current centroid
                Vector3 p2 = curr.pts[getCwPt(curr, p1)];   // CW point

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

    public int getCwPt(Face face, Vector3 TileCentroid) {
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
        // flood fill
        float roll;
        for(int i = 0; i < tiles.size*1.2; i++) {
            roll = r.nextFloat()*100;
            if(roll < 2f) {
                plates.get(r.nextInt(54) + 18).grow();
            } else if(roll < 14.7f) {
                plates.get(r.nextInt(10) + 8).grow();
            } else
                plates.get(r.nextInt(7)).grow();
        }
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
        
        
        for (Tile t : tiles){
            t.area.setValue((float)Math.PI*4.0f*scale/tiles.size);
            
            r1 = new Vector3(t.centroid).sub(this.position).nor();
            r2 = new Vector3(S.position).sub(this.position).nor();
            r3 = new Vector3(S.position).sub(t.centroid).nor();
            
            area_fractional = t.area.getValue()*(r1.dot(r2));
            
            area_fractional = (area_fractional < 0.0f) ? 0.0f : area_fractional;
            p = (k/r3.len2())*area_fractional;
            t.power.setValue(p);
        }
    }
    
    
}
