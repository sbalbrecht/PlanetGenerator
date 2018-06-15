package com.mygdx.game;

import com.badlogic.gdx.math.Vector3;

public final class utils {

    /* gets the middle point b/w two vectors, then shifts it to the unit sphere */
    public static Vector3 getMiddlePoint(Vector3 u, Vector3 v) {
        // need to check if point already exists --> need storage for midpoints?
        // only need to store midpoints if it would be faster to search for them
        //  than to calculate them again

        Vector3 w = u.cpy().add(v).scl(0.5f);
        w = convertToUnitSphere(w);

        // store w for later
        return w;
    }

    /* Converts the given vector to one that lies on the unit sphere */
    public static Vector3 convertToUnitSphere(Vector3 v) {
        float length = (float)Math.sqrt(v.x * v.x + v.y * v.y + v.z * v.z);
        return new Vector3(v.x/length, v.y/length, v.z/length);
    }
}
