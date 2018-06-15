package com.mygdx.game;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

public class Planet{
    public Array<Face> faces = new Array<Face>();
    // create Planet constructor
    Planet() {

    }

	void generateIcosphere(int subdivisions){
		//hardcoded ico shit

		float phi = 1.0f + (float)Math.sqrt(5.0f)/2.0f;
		float u = 1.0f/(float)Math.sqrt(phi*phi + 1.0f);
		float v = phi*u;
		
		Array<Node> nodes = new Array<Node>();
		
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
		for (Vector3 p : points) nodes.add(new Node(p));

	    // 20 faces
		faces.add(new Face(points[0], points[ 1], points[ 8]));
		faces.add(new Face(points[0], points[ 4], points[ 5]));
		faces.add(new Face(points[0], points[ 5], points[10]));
		faces.add(new Face(points[0], points[ 8], points[ 4]));
		faces.add(new Face(points[0], points[10], points[ 1]));
		faces.add(new Face(points[1], points[ 6], points[ 8]));
		faces.add(new Face(points[1], points[ 7], points[ 6]));
		faces.add(new Face(points[1], points[10], points[ 7]));
		faces.add(new Face(points[2], points[ 3], points[11]));
		faces.add(new Face(points[2], points[ 4], points[ 9]));
		faces.add(new Face(points[2], points[ 5], points[ 4]));
		faces.add(new Face(points[2], points[ 9], points[ 3]));
		faces.add(new Face(points[2], points[11], points[ 5]));
		faces.add(new Face(points[3], points[ 6], points[ 7]));
		faces.add(new Face(points[3], points[ 7], points[11]));
		faces.add(new Face(points[3], points[ 9], points[ 6]));
		faces.add(new Face(points[4], points[ 8], points[ 9]));
		faces.add(new Face(points[5], points[11], points[10]));
		faces.add(new Face(points[6], points[ 9], points[ 8]));
		faces.add(new Face(points[7], points[10], points[11]));

		// subdivide here
        subdivide(subdivisions);



        // OH GOD IT HURTS
//		Array<Edge> edges = new Array<Edge>();
//		edges.add(new Edge(nodes.get(0), nodes.get(1)));
//		edges.add(new Edge(nodes.get(0), nodes.get(4)));
//		edges.add(new Edge(nodes.get(0), nodes.get(5)));
//		edges.add(new Edge(nodes.get(0), nodes.get(8)));
//		edges.add(new Edge(nodes.get(0), nodes.get(10)));
//		edges.add(new Edge(nodes.get(1), nodes.get(6)));
//		edges.add(new Edge(nodes.get(1), nodes.get(7)));
//		edges.add(new Edge(nodes.get(1), nodes.get(8)));
//		edges.add(new Edge(nodes.get(1), nodes.get(10)));
//		edges.add(new Edge(nodes.get(2), nodes.get(3)));
//		edges.add(new Edge(nodes.get(2), nodes.get(1)));
//		edges.add(new Edge(nodes.get(2), nodes.get(1)));
//		edges.add(new Edge(nodes.get(2), nodes.get(1)));
//		edges.add(new Edge(nodes.get(2), nodes.get(1)));
//		edges.add(new Edge(nodes.get(3), nodes.get(1)));
//		edges.add(new Edge(nodes.get(3), nodes.get(1)));
//		edges.add(new Edge(nodes.get(3), nodes.get(1)));
//		edges.add(new Edge(nodes.get(3), nodes.get(1)));
//		edges.add(new Edge(nodes.get(4), nodes.get(1)));
//		edges.add(new Edge(nodes.get(4), nodes.get(1)));
//		edges.add(new Edge(nodes.get(4), nodes.get(1)));
//		edges.add(new Edge(nodes.get(5), nodes.get(1)));
//		edges.add(new Edge(nodes.get(5), nodes.get(1)));
//		edges.add(new Edge(nodes.get(6), nodes.get(1)));
//		edges.add(new Edge(nodes.get(6), nodes.get(1)));
//		edges.add(new Edge(nodes.get(6), nodes.get(1)));
//		edges.add(new Edge(nodes.get(7), nodes.get(1)));
//		edges.add(new Edge(nodes.get(7), nodes.get(1)));
//		edges.add(new Edge(nodes.get(8), nodes.get(1)));
//		edges.add(new Edge(nodes.get(10), nodes.get(1)));
	}

	/* Iterates through faces and sets the neighbors of every face */
    public void setNeighbors() {
        for(int i = 0; i < faces.size-1; i++) {
            if(faces.get(i).nbrs.size == 5)     // only for icosahedron, not dodecahedron
                continue;
            for(int j = i+1; j < faces.size; j++) {
                faces.get(i).testNeighbor(faces.get(j));
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

                Vector3 q0 = utils.getMiddlePoint(p0, p1);
                Vector3 q1 = utils.getMiddlePoint(p1, p2);
                Vector3 q2 = utils.getMiddlePoint(p2, p0);

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
}
