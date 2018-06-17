package com.mygdx.game;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.mygdx.game.util.vmath;

public class Face{
    public Vector3[] pts = new Vector3[3];
    public Vector3 centroid;
    public Array<Face> nbrs = new Array<Face>();

    Face(Vector3 p0, Vector3 p1, Vector3 p2) {
        this.pts[0] = p0;
        this.pts[1] = p1;
        this.pts[2] = p2;
        this.centroid = vmath.convertToUnitSphere(getCentroid(p0, p1, p2));
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

    Vector3 getCentroid(Vector3 p0, Vector3 p1, Vector3 p2) {
        return new Vector3(
                (p0.x + p1.x + p2.x)/3,
                (p0.y + p1.y + p2.y)/3,
                (p0.z + p1.z + p2.z)/3);
    }
}
