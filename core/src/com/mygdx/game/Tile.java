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

    boolean testNeighbor(Tile b) {
        if(nbrs.contains(b, false))
            return true;
        int count = 0;
        for(int i = 0; i < pts.size; i++) {
            for(int j = 0; j < b.pts.size; j++) {
                if (pts.get(i) == b.pts.get(j)) //if true
                    count++;
            }
        }
        if(count == 2) {
            nbrs.add(b);
            b.nbrs.add(this);
            return true;
        } else
            return false;
    }
}
