package com.mygdx.game.util;

import com.badlogic.gdx.math.Vector3;

public final class VMath{

    /* gets the middle point b/w two vectors*/
    public static Vector3 mid(Vector3 u, Vector3 v) {
        // need to check if point already exists --> need storage for midpoints?
        // only need to store midpoints if it would be faster to search for them
        //  than to calculate them again

        Vector3 w = u.cpy().add(v).scl(0.5f);
        return w;
    }
    
}
