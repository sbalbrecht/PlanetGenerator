package com.mygdx.game.util;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

public final class VMath {

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
        return sum;
    }

    /**
     * Calculates latitude of input v in radians. Assumes the input is a point on a sphere.
     * Range is 0 (North) to PI (South)
     * @param v The vector to convert
     * @return The latitude of v
     */
    public static float cartesianToLatitude(Vector3 v, float radius) {
//        return MathUtils.atan2((float)Math.sqrt(v.x*v.x + v.z*v.z), v.y);
        return (float)(Math.asin(v.y / radius));
    }

    /**
     * Calculates longitude of input v in radians. Assumes the input is a point on a sphere.
     * Range is -PI to PI
     * @param v The vector to convert
     * @return The longitude of v
     */
    public static float cartesianToLongitude(Vector3 v) {
        return (float)Math.atan2(v.z, v.x);
    }

    public static Vector3 latitudeToCartesian(float latitude, float longitude, float radius) {
        return new Vector3(
                (float)(radius * Math.cos(latitude) * Math.cos(longitude)),
                (float)(radius * Math.sin(latitude)),
                (float)(radius * Math.cos(latitude) * Math.sin(longitude))
        );
    }
}
