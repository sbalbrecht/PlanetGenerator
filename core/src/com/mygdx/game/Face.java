package com.mygdx.game;

import com.badlogic.gdx.utils.Array;

public class Face{

	Array<Edge> edges;
	Array<Node> nodes;
	
	public Face(Array<Node> nodes, Array<Edge> edges){
		this.nodes.addAll(nodes);
		this.edges.addAll(edges);
	}
	
}
