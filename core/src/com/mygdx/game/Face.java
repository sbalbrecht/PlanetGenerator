package com.mygdx.game;

import com.badlogic.gdx.utils.Array;

public class Face{
    
    public int[] pts = new int[3];
    public int centroid;

    Face(int p0, int p1, int p2, int centroid) {
        pts[0] = p0;
        pts[1] = p1;
        pts[2] = p2;
        this.centroid = centroid;
    }

    public int getClockwisePt(int point) {
        // Find the index of the point
        int index = 0;
        for(int i = 0; i < pts.length; i++) {
            if(pts[i] == point) {
                index = i;
                break;
            }
        }
        // Find the index of the point CW from the given point
        return pts[(index + 1) % pts.length];
    }
}
