package com.mygdx.game;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

public class Face{
    public Vector3[] pts = new Vector3[3];
    public Array<Face> nbrs = new Array<Face>();

    Face(Vector3 p0, Vector3 p1, Vector3 p2) {
        this.pts[0] = p0;
        this.pts[1] = p1;
        this.pts[2] = p2;
    }

    boolean testNeighbor(Face b) {
        if(nbrs.contains(b, false))
            return true;
        int count = 0;
        for(int i = 0; i < pts.length; i++) {
            for(int j = 0; j < pts.length; j++) {
                if (pts[i] == b.pts[j]) //if true
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
