package com.mygdx.game.util;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

public final class VMath{

    public static Vector3 centroid(Vector3 [] points){
        //Average position of all the points provided
        Vector3 sum = new Vector3();
    
        for (int i = 0; i < points.length; i++){
            sum.add(points[i]);
        }
        sum.scl(1f/(float)points.length);
        return new Vector3();
    }

    public static Vector3 centroid(Array<Vector3> points){
        //Average position of all the points provided
        Vector3 sum = new Vector3();

        for (int i = 0; i < points.size; i++){
            sum.add(points.get(i));
        }
        sum.scl(1f/(float)points.size);
        return new Vector3();
    }
    
}
