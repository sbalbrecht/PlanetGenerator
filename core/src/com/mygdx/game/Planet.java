package com.mygdx.game;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.mygdx.game.util.vmath;

public class Planet{
    public Array<Face> faces = new Array<Face>();
    public Array<Tile> tiles = new Array<Tile>();

    Planet() {}

	void generateIcosphere(int subdivisions){
		//hardcoded ico shit

		float phi = (float)((1.0f + Math.sqrt(5.0f))/2.0f);
		float u = 1.0f/(float)Math.sqrt(phi*phi + 1.0f);
		float v = phi*u;


		// Points are scaled x10 so the camera is more flexible
		Vector3[] points =
			{
				new Vector3(0.0f,    +v,   +u).scl(10),
				new Vector3(0.0f,    +v,   -u).scl(10),
				new Vector3(0.0f,    -v,   +u).scl(10),
				new Vector3(0.0f,    -v,   -u).scl(10),
				new Vector3(+u,    0.0f,   +v).scl(10),
				new Vector3(-u,    0.0f,   +v).scl(10),
				new Vector3(+u,    0.0f,   -v).scl(10),
				new Vector3(-u,    0.0f,   -v).scl(10),
				new Vector3(+v,      +u, 0.0f).scl(10),
				new Vector3(+v,      -u, 0.0f).scl(10),
				new Vector3(-v,      +u, 0.0f).scl(10),
				new Vector3(-v,      -u, 0.0f).scl(10)
			};

	    // 20 faces
		faces.addAll(
			new Face(points[0], points[ 8], points[ 1]),
			new Face(points[0], points[ 5], points[ 4]),
			new Face(points[0], points[10], points[ 5]),
			new Face(points[0], points[ 4], points[ 8]),
			new Face(points[0], points[ 1], points[10]),
			new Face(points[1], points[ 8], points[ 6]),
			new Face(points[1], points[ 6], points[ 7]),
			new Face(points[1], points[ 7], points[10]),
			new Face(points[2], points[11], points[ 3]),
			new Face(points[2], points[ 9], points[ 4]),
			new Face(points[2], points[ 4], points[ 5]),
			new Face(points[2], points[ 3], points[ 9]),
			new Face(points[2], points[ 5], points[11]),
			new Face(points[3], points[ 7], points[ 6]),
			new Face(points[3], points[11], points[ 7]),
			new Face(points[3], points[ 6], points[ 9]),
			new Face(points[4], points[ 9], points[ 8]),
			new Face(points[5], points[10], points[11]),
			new Face(points[6], points[ 8], points[ 9]),
			new Face(points[7], points[11], points[10])
		 );

        subdivide(subdivisions);
        setFaceNeighbors();
        //convertToDual();
        System.out.println("Faces: " + faces.size);
        System.out.println("Tiles: " + tiles.size);
	}

    public void setFaceNeighbors() {
        for(int i = 0; i < faces.size; i++) {
            for(int j = 0; j < faces.size; j++) {
                if(i == j) continue;
                if(faces.get(i).nbrs.size == 3) break;
                if(faces.get(i).testNeighbor(faces.get(j))) {
                    System.out.println(i + " and " + j + " are neighbors");
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

    public void convertToDual() {
        Array<Vector3> points = new Array<Vector3>();             // Array for Tile points
        Face curr;
        boolean isTile;
        for(Face face : faces) {
            curr = face;
            isTile = false;
            Vector3 p1 = curr.pts[0];                       // Tile centroid

            for(Tile tile : tiles) {                        // Check if tile with
                if(tile.centroid == p1) {                   //   that center exists
                    p1 = curr.pts[1];                       // If so, go to next point
                    break;
                }
            }
            for(Tile tile : tiles) {                        // Check if tile with
                if(tile.centroid == p1) {                   //   next center exists
                    isTile = true;                          // If so, every point on face
                    break;                                  //   is a tile, move on
                }
            }
            if(isTile) continue;

            do {
                points.add(curr.centroid);                  // add current centroid
                Vector3 p2 = curr.pts[getCwPt(curr, p1)];   // CCW point
//                System.out.println("Cn: " + p1.x + "," + p1.y + "," + p1.z);
//                System.out.println("Cw: " + p2.x + "," + p2.y + "," + p2.z);

                for (Face nbr : curr.nbrs) {                // find CCW neighbor
                    int count = 0;
                    for(int i = 0; i < nbr.pts.length; i++) {
                        if(nbr.pts[i] == p1 || nbr.pts[i] == p2) {
                            count++;
                        }
                    }
                    if(count == 2) {
                        curr = nbr;
//                        System.out.println("Nbr p1: " + nbr.pts[0].x + "," + nbr.pts[0].y + "," + nbr.pts[0].z);
//                        System.out.println("Nbr p2: " + nbr.pts[1].x + "," + nbr.pts[1].y + "," + nbr.pts[1].z);
//                        System.out.println("Nbr p3: " + nbr.pts[2].x + "," + nbr.pts[2].y + "," + nbr.pts[2].z);

                        break;
                    }
                }
            } while(curr != face);

            tiles.add(new Tile(p1, points));
            System.out.println("Tile added, centroid (" + p1.x + "," + p1.y + "," + p1.z
                    + "), " + points.size + " points");
            points.clear();                                 // clear points for next tile
        }
    }

    public int getCwPt(Face face, Vector3 TileCentroid) {
        int index = 0;
        for(int i = 0; i < face.pts.length; i++) {
            if(face.pts[i] == TileCentroid) {
                index = i;
                break;
            }
        }
        if(index + 2 >= face.pts.length) {
            return index - 1;
        } else
            return index + 2;
    }
}
