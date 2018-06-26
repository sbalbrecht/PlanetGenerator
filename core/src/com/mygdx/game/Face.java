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
        for(int ptA : pts) {
            for(int ptB : b.pts) {
                if (ptA == ptB) {
                    count++;
                    break;
                }
            }
            if(count == 2) break;
        }
        return (count == 2);
    }

    private Vector3 getCentroid(int p0, int p1, int p2, Array<Vector3> pts) {
        Vector3 u = pts.get(p0);
        Vector3 v = pts.get(p1);
        Vector3 w = pts.get(p2);
        return new Vector3(
                (u.x + v.x + w.x)/3,
                (u.y + v.y + w.y)/3,
                (u.z + v.z + w.z)/3);
    }
}
