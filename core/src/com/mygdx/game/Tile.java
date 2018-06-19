package com.mygdx.game;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

public class Tile {
    public Vector3 centroid;
    public Array<Vector3> pts = new Array<Vector3>();
    public Array<Tile> nbrs = new Array<Tile>();
    public boolean drawn = false;
    public int plateId = -1;

    Tile(Vector3 centroid, Array<Vector3> points) {
        this.centroid = centroid;
        for(Vector3 pt : points) {
            this.pts.add(pt);
        }
    }

    public void addNbr(Tile tile) {
        nbrs.add(tile);
    }

    public Tile getNbr(Vector3 p1, Vector3 p2) {
        for(Tile nbr : nbrs) {
            if(nbr.pts.contains(p1, false) && nbr.pts.contains(p2, false))
                return nbr;
        }
        return null;
    }

    boolean testNeighbor(Tile b) {
        if(nbrs.contains(b, false))
            return false;
        int count = 0;
        for(int i = 0; i < pts.size; i++) {
            for(int j = 0; j < b.pts.size; j++) {
                if (pts.get(i) == b.pts.get(j)) {
                    count++;
                    break;
                }
            }
            if(count == 2) break;
        }
        if(count == 2) {
            return true;
        } else
            return false;
    }
}
