package com.mygdx.game;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

public class Tile {
    public Vector3 centroid;
    public Array<Vector3> pts = new Array<Vector3>();
    public Array<Tile> nbrs = new Array<Tile>();

    Tile(Vector3 centroid, Array<Vector3> points) {
        this.centroid = centroid;
        for(Vector3 pt : points) {
            this.pts.add(pt);
        }
    }
}
