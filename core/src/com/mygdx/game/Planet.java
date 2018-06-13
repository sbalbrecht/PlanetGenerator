package com.mygdx.game;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

public class Planet{
	void generateIcosphere(){
		//hardcoded ico shit
		
		float phi = 1.0f + (float)Math.sqrt(5.0f)/2.0f;
		float u = 1.0f/(float)Math.sqrt(phi*phi + 1.0f);
		float v = phi*u;
		
		Array<Node> nodes = new Array<Node>();
		
		Vector3[] points =
				{
						new Vector3(0.0f, v, u),
						new Vector3(0.0f, v, -u),
						new Vector3(0.0f, -v, u),
						new Vector3(0.0f, -v, -u),
						new Vector3(+u, 0.0f, +v),
						new Vector3(-u, 0.0f, +v),
						new Vector3(+u, 0.0f, -v),
						new Vector3(-u, 0.0f, -v),
						new Vector3(+v, +u, 0.0f),
						new Vector3(+v, -u, 0.0f),
						new Vector3(-v, +u, 0.0f),
						new Vector3(-v, -u, 0.0f)
				};
		for (Vector3 p : points) nodes.add(new Node(p));
		
		Array<Edge> edges = new Array<Edge>();
		edges.add(new Edge(nodes.get(0), nodes.get(1)));
		edges.add(new Edge(nodes.get(0), nodes.get(4)));
		edges.add(new Edge(nodes.get(0), nodes.get(5)));
		edges.add(new Edge(nodes.get(0), nodes.get(8)));
		edges.add(new Edge(nodes.get(0), nodes.get(10)));
		edges.add(new Edge(nodes.get(1), nodes.get(6)));
		edges.add(new Edge(nodes.get(1), nodes.get(7)));
		edges.add(new Edge(nodes.get(1), nodes.get(8)));
		edges.add(new Edge(nodes.get(1), nodes.get(10)));
		edges.add(new Edge(nodes.get(2), nodes.get(3)));
		edges.add(new Edge(nodes.get(2), nodes.get(1)));
		edges.add(new Edge(nodes.get(2), nodes.get(1)));
		edges.add(new Edge(nodes.get(2), nodes.get(1)));
		edges.add(new Edge(nodes.get(2), nodes.get(1)));
		edges.add(new Edge(nodes.get(3), nodes.get(1)));
		edges.add(new Edge(nodes.get(3), nodes.get(1)));
		edges.add(new Edge(nodes.get(3), nodes.get(1)));
		edges.add(new Edge(nodes.get(3), nodes.get(1)));
		edges.add(new Edge(nodes.get(4), nodes.get(1)));
		edges.add(new Edge(nodes.get(4), nodes.get(1)));
		edges.add(new Edge(nodes.get(4), nodes.get(1)));
		edges.add(new Edge(nodes.get(5), nodes.get(1)));
		edges.add(new Edge(nodes.get(5), nodes.get(1)));
		edges.add(new Edge(nodes.get(6), nodes.get(1)));
		edges.add(new Edge(nodes.get(6), nodes.get(1)));
		edges.add(new Edge(nodes.get(6), nodes.get(1)));
		edges.add(new Edge(nodes.get(7), nodes.get(1)));
		edges.add(new Edge(nodes.get(7), nodes.get(1)));
		edges.add(new Edge(nodes.get(8), nodes.get(1)));
		edges.add(new Edge(nodes.get(10), nodes.get(1)));
		
		
		
		
		
		Vector3[] faces = new Vector3[20];
		
		
		
		
	}
}
