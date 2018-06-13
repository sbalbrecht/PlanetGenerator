package com.mygdx.game;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

public class Node{
	Vector3 point;
	Array<Edge> edges = new Array<Edge>();
	Array<Face> faces = new Array<Face>();
	public Node(Vector3 point){
		this.point = point;
	}
	
	
}
