package com.mygdx.game;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

public class Face{
    
    public int[] pts = new int[3];
    public int centroid;
    public Array<Face> nbrs = new Array<Face>();
    public Array<Integer> ptsUsedAsTileCentroid = new Array<Integer>();

    Face(int p0, int p1, int p2, int centroid) {
        this.pts[0] = p0;
        this.pts[1] = p1;
        this.pts[2] = p2;
        this.centroid = centroid;
    }

    public void addNbr(Face face) {
        nbrs.add(face);
    }

    public void addPtUsedInTileCentroid(int pt) {
        ptsUsedAsTileCentroid.add(pt);
    }
}
