package com.mygdx.game;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.mygdx.game.util.vmath;

public class Planet{
    public Array<Face> faces = new Array<Face>();
    public Array<Tile> tiles = new Array<Tile>();
    // create Planet constructor
    Planet() {

    }

	void generateIcosphere(int subdivisions){
		//hardcoded ico shit

		float phi = (float)((1.0f + Math.sqrt(5.0f))/2.0f);
		float u = 1.0f/(float)Math.sqrt(phi*phi + 1.0f);
		float v = phi*u;


		// Points are scaled x10 so the camera is more flexible
		Vector3[] points =
			{
				new Vector3(0.0f,   +u,   +v).scl(10),
				new Vector3(0.0f,   -u,   +v).scl(10),
				new Vector3(0.0f,   +u,   -v).scl(10),
				new Vector3(0.0f,   -u,   -v).scl(10),
				new Vector3(+u,     +v, 0.0f).scl(10),
				new Vector3(-u,     +v, 0.0f).scl(10),
				new Vector3(+u,     -v, 0.0f).scl(10),
				new Vector3(-u,     -v, 0.0f).scl(10),
				new Vector3(+v,   0.0f,   +u).scl(10),
				new Vector3(+v,   0.0f,   -u).scl(10),
				new Vector3(-v,   0.0f,   +u).scl(10),
				new Vector3(-v,   0.0f,   -u).scl(10)
			};

	    // 20 faces
		faces.addAll(
			new Face(points[0], points[ 1], points[ 8]),
			new Face(points[0], points[ 4], points[ 5]),
			new Face(points[0], points[ 5], points[10]),
			new Face(points[0], points[ 8], points[ 4]),
			new Face(points[0], points[10], points[ 1]),
			new Face(points[1], points[ 6], points[ 8]),
			new Face(points[1], points[ 7], points[ 6]),
			new Face(points[1], points[10], points[ 7]),
			new Face(points[2], points[ 3], points[11]),
			new Face(points[2], points[ 4], points[ 9]),
			new Face(points[2], points[ 5], points[ 4]),
			new Face(points[2], points[ 9], points[ 3]),
			new Face(points[2], points[11], points[ 5]),
			new Face(points[3], points[ 6], points[ 7]),
			new Face(points[3], points[ 7], points[11]),
			new Face(points[3], points[ 9], points[ 6]),
			new Face(points[4], points[ 8], points[ 9]),
			new Face(points[5], points[11], points[10]),
			new Face(points[6], points[ 9], points[ 8]),
			new Face(points[7], points[10], points[11])
		 );

        subdivide(subdivisions);
        setFaceNeighbors();
        convertToDual();
        System.out.println("Nbrs:  " + faces.get(0).nbrs.size);
        System.out.println("Faces: " + faces.size);
        System.out.println("Tiles: " + tiles.size);
	}

	/* Iterates through tiles and sets every neighbor */
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
                if(faces.get(i).testNeighbor(faces.get(j))) {
                    faces.get(i).addNbr(faces.get(j));
                    faces.get(j).addNbr(faces.get(i));
                }
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

                Vector3 q0 = vmath.getMiddlePoint(p0, p1);
                Vector3 q1 = vmath.getMiddlePoint(p1, p2);
                Vector3 q2 = vmath.getMiddlePoint(p2, p0);

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
    public void convertToDual() {
        Array<Vector3> points = new Array<Vector3>();           // Array for Tile points
        Face curr;
        boolean tileExists;

        for(Face face : faces) {
            curr = face;
            tileExists = false;
            Vector3 p1 = curr.pts[0];                       // Tile centroid

            for(Tile tile : tiles) {                        // Check if tile exists
                if(tile.centroid == p1) {
                    tileExists = true;
                    break;
                }
            }
            if(tileExists) continue;

            do {
                //System.out.println(c++);
                points.add(curr.centroid);                  // add current centroid
                Vector3 p2 = curr.pts[getCcwPt(curr, p1)];  // CCW point
                for (Face nbr : curr.nbrs) {                // find CCW neighbor
                    int count = 0;
                    for(int i = 0; i < nbr.pts.length; i++) {
                        if(nbr.pts[i] == p1 || nbr.pts[i] == p2) {
                            count++;
                        }
                    }
                    if(count == 2) {
                        System.out.println("Neighbor found.");
                        curr = nbr;
                        break;
                    }
                }
            } while(curr.pts[getCcwPt(curr, p1)] != face.pts[getCwPt(face, p1)]);

            tiles.add(new Tile(p1, points));
            points.clear();                                 // clear points for next tile
        }
    }

    public int getCcwPt(Face face, Vector3 centroid) {
        int index = 0;
        for(int i = 0; i < face.pts.length; i++) {
            if(face.pts[i] == centroid) {
                index = i;
                break;
            }
        }
        if(index + 2 > face.pts.length) {
            return face.pts.length - index;
        } else
            return index + 2;
    }

    public int getCwPt(Face face, Vector3 centroid) {
        int index = 0;
        for(int i = 0; i < face.pts.length; i++) {
            if(face.pts[i] == centroid) {
                index = i;
                break;
            }
        }
        if(index - 1 < 0) {
            return face.pts.length - 1;
        } else
            return index - 1;
    }
}
