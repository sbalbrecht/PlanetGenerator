package com.mygdx.game;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

public class Face{
    
    public int[] pts = new int[3];
    public Vector3 centroid;
    public Array<Face> nbrs = new Array<Face>();
    public Array<Integer> ptsUsedAsTileCentroid = new Array<Integer>();

    Face(int p0, int p1, int p2, Array<Vector3> points) {
        this.pts[0] = p0;
        this.pts[1] = p1;
        this.pts[2] = p2;
        this.centroid = getCentroid(p0, p1, p2, points).nor();
    }

    public void addNbr(Face face) {
        nbrs.add(face);
    }
    public void addPtUsedInTileCentroid(int pt) {
        ptsUsedAsTileCentroid.add(pt);
    }

    boolean testNeighbor(Face b) {
        if(nbrs.contains(b, false))
            return false;
        int count = 0;
        for(int i = 0; i < pts.length; i++) {
            for(int j = 0; j < b.pts.length; j++) {
                if (pts[i] == b.pts[j]) {
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

    Vector3 getCentroid(int p0, int p1, int p2, Array<Vector3> pts) {
        return new Vector3(
                (pts.get(p0).x + pts.get(p1).x + pts.get(p2).x)/3,
                (pts.get(p0).y + pts.get(p1).y + pts.get(p2).y)/3,
                (pts.get(p0).z + pts.get(p1).z + pts.get(p2).z)/3);
    }
}
