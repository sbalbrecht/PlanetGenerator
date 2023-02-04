package dev.urth.planetgen;

public class Face {
    private final int[] pts = new int[3];
    private final int centroid;

    Face(int p0, int p1, int p2, int centroid) {
        pts[0] = p0;
        pts[1] = p1;
        pts[2] = p2;
        this.centroid = centroid;
    }

    public int getClockwisePt(int point) {
        // Find the index of the point
        int index = 0;
        for (int i = 0; i < pts.length; i++) {
            if (pts[i] == point) {
                index = i;
                break;
            }
        }
        // Find the index of the point CW from the given point
        if (index + 2 >= pts.length) {
            return pts[index - 1];
        } else {
            return pts[index + 2];
        }
    }

    public int[] getPts() {
        return pts;
    }

    public int getCentroid() {
        return centroid;
    }
}
