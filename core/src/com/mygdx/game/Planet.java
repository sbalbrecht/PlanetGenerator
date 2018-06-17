package com.mygdx.game;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.mygdx.game.util.vmath;

public class Planet{
    public Array<Face> faces = new Array<Face>();
    public Array<Tile> tiles = new Array<Tile>();
    public Array<Vector3> points = new Array<Vector3>();
    private float scale;
    // create Planet constructor
    Planet() {

    }

	void generateIcosphere(float scale, int subdivisions){
		//hardcoded ico shit

		float phi = (float)((1.0f + Math.sqrt(5.0f))/2.0f);
		float u = 1.0f/(float)Math.sqrt(phi*phi + 1.0f);
		float v = phi*u;

		this.scale = scale;

		// Points are scaled x10 so the camera is more flexible
		points.addAll(
				new Vector3(0.0f,   +u,   +v),
				new Vector3(0.0f,   -u,   +v),
				new Vector3(0.0f,   +u,   -v),
				new Vector3(0.0f,   -u,   -v),
				new Vector3(+u,     +v, 0.0f),
				new Vector3(-u,     +v, 0.0f),
				new Vector3(+u,     -v, 0.0f),
				new Vector3(-u,     -v, 0.0f),
				new Vector3(+v,   0.0f,   +u),
				new Vector3(+v,   0.0f,   -u),
				new Vector3(-v,   0.0f,   +u),
				new Vector3(-v,   0.0f,   -u)
			);
		
		for (Vector3 p: points){ p.scl(scale);}
		
	    // 20 faces
		faces.addAll(
			new Face(points.get(0), points.get( 1), points.get( 8)),
			new Face(points.get(0), points.get( 4), points.get( 5)),
			new Face(points.get(0), points.get( 5), points.get(10)),
			new Face(points.get(0), points.get( 8), points.get( 4)),
			new Face(points.get(0), points.get(10), points.get( 1)),
			new Face(points.get(1), points.get( 6), points.get( 8)),
			new Face(points.get(1), points.get( 7), points.get( 6)),
			new Face(points.get(1), points.get(10), points.get( 7)),
			new Face(points.get(2), points.get( 3), points.get(11)),
			new Face(points.get(2), points.get( 4), points.get( 9)),
			new Face(points.get(2), points.get( 5), points.get( 4)),
			new Face(points.get(2), points.get( 9), points.get( 3)),
			new Face(points.get(2), points.get(11), points.get( 5)),
			new Face(points.get(3), points.get( 6), points.get( 7)),
			new Face(points.get(3), points.get( 7), points.get(11)),
			new Face(points.get(3), points.get( 9), points.get( 6)),
			new Face(points.get(4), points.get( 8), points.get( 9)),
			new Face(points.get(5), points.get(11), points.get(10)),
			new Face(points.get(6), points.get( 9), points.get( 8)),
			new Face(points.get(7), points.get(10), points.get(11))
		 );

        subdivide(subdivisions);
		for (Vector3 p : points){
			p.nor().scl(scale);
		}

//        convertDual();

	
	}

	/* Iterates through tiles and sets the neighbors of every face */
    public void setTileNeighbors() {
        for(int i = 0; i < tiles.size-1; i++) {
            for(int j = i+1; j < tiles.size; j++) {
                if(tiles.get(i).nbrs.size == 6)
                    break;
                tiles.get(i).testNeighbor(tiles.get(j));
            }
        }
    }
    public void setFaceNeighbors() {
        for(int i = 0; i < faces.size-1; i++) {
            for(int j = i+1; j < faces.size; j++) {
                if(faces.get(i).nbrs.size == 4)
                    break;
                faces.get(i).testNeighbor(faces.get(j));
            }
        }
    }
	
	void randomizeTopography(){
    	Face tempFc;
    	for (int i = 0; i < faces.size; i++){
			tempFc = faces.get(i);
			for (int j = 0; j < tempFc.pts.length; j++){
				tempFc.pts[j].scl(1.0f + 0.1f*(float)Math.random());
			}
		}
	}

    /* subdivides faces n times */
    public void subdivide(int degree) {
        for(int i = 0; i < degree; i++) {
            Array<Face> newFaces = new Array<Face>();
            for(Face face : faces) {
                Vector3 p0 = face.pts[0];
                Vector3 p1 = face.pts[1];
                Vector3 p2 = face.pts[2];

                Vector3 q0 = vmath.mid(p0, p1);
                Vector3 q1 = vmath.mid(p1, p2);
                Vector3 q2 = vmath.mid(p2, p0);

                points.addAll(q0, q1, q2);
                
                newFaces.add(new Face(q0, q2, p0));
                newFaces.add(new Face(q1, q0, p1));
                newFaces.add(new Face(q2, q1, p2));
                newFaces.add(new Face(q0, q1, q2));
                
            }
            // set faces = newFaces
            faces.clear();
            faces.shrink();
            faces.ensureCapacity(newFaces.size);
            faces.addAll(newFaces);
        }
    }

    // unfinished
    public void convertDual() {
        Array<Vector3> points = new Array<Vector3>();
        for(Face face : faces) {
            Vector3 centroid = face.centroid;           // tile centroid
            Vector3 p1 = face.pts[0];
            Vector3 p2 = face.pts[2];
            for (int i = 0; i < faces.size; i++) {
                if(face.equals(faces.get(i)))          //
                    continue;
                int count = 0;
                for(int j = 0; j < faces.get(i).pts.length; j++) {
                    if (faces.get(i).pts[j] == p1 ||
                            faces.get(i).pts[j] == p2)
                        count++;
                }
                if(count == 2) {
                    // then they neighbors good
                    // this face becomes the next face to focus on
                }
            }
        }
    }
}
