package com.mygdx.game.util;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

public final class VMath {

    public final static float PI_6 = (float)(Math.PI/6);
    public final static float PI_3 = (float)(Math.PI/3);
    public final static float PI_2 = (float)(Math.PI/2);
    public final static float _2PI_3 = (float)(2*Math.PI/3);
    public final static float _5PI_6 = (float)(5*Math.PI/6);

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
     * Range is -PI/2 (South) to PI/2 (North)
     * @param v The vector to convert
     * @return The latitude of v
     */
    public static float cartesianToLatitude(Vector3 v, float radius) {
//        return (float)(Math.atan(Math.sqrt(v.x*v.x + v.z*v.z) / v.y));
        return (float)(Math.acos(v.y / Math.sqrt(v.x*v.x + v.y*v.y + v.z*v.z)));
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

    public static Vector3 sphericalToCartesian(float latitude, float longitude, float radius) {
        return new Vector3(
                (float)(radius * Math.sin(latitude) * Math.cos(longitude)),
                (float)(radius * Math.cos(latitude)),
                (float)(radius * Math.sin(latitude) * Math.sin(longitude))
        );
    }
}
