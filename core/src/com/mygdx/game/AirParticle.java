package com.mygdx.game;

import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.EllipseShapeBuilder;
import com.badlogic.gdx.math.Vector3;
import com.mygdx.game.util.VMath;

import java.util.Random;

import static com.mygdx.game.util.VMath.*;

public class AirParticle {
    private Vector3 Position;
    private float Temperature_C;
    private float Elevation_masl;
    public boolean isInUpperAtmosphere;
    private float PlanetRadius;

    public AirParticle(float planetRadius) {
        Random r = new Random();

        float x = (r.nextFloat()-0.5f);
        float y = (r.nextFloat()-0.5f);
        float z = (r.nextFloat()-0.5f);

        PlanetRadius = planetRadius;
        Position = new Vector3(x, y, z);
        Position.nor().scl(PlanetRadius);
        Elevation_masl = r.nextFloat() * 14000;
        isInUpperAtmosphere = (Elevation_masl >= 7000);
        Temperature_C = calculateTemperature();
    }

    public Vector3 getPosition() {
        return Position;
    }

    public void setPosition(Vector3 position) {
        Position = position;
    }

    public float getTemperature() {
        return Temperature_C;
    }

    public void setTemperature(float temperature_C) {
        Temperature_C = temperature_C;
    }

    private float calculateTemperature() {
        Random r = new Random();
        float lat = VMath.cartesianToLatitude(Position, PlanetRadius) + (float)(Math.PI/2);
        if (lat > Math.PI / 2) {
            // reducing range to 0 - PI/2
            lat = (float)(Math.PI - lat);
        }

        float percentToBoundary;
        float temperature;

//        if (lat < PI_3)
//        {
//            percentToBoundary = lat / PI_3;
//            temperature = (isInUpperAtmosphere)
//                    ? -20 - percentToBoundary * 35  // -55 to -20
//                    : -20 + percentToBoundary * 20; // -20 to 0
//        }
//        else if (lat < _2PI_3)
//        {
//            percentToBoundary = (lat - PI_3) / PI_3;
//            temperature = (isInUpperAtmosphere)
//                    ? -10 - percentToBoundary * 20  // -10 to -30
//                    : 15 - percentToBoundary * 25;  // 15 to -10
//        }
//        else
//        {
//            percentToBoundary = (lat - _2PI_3) / PI_3;
//            temperature = (isInUpperAtmosphere)
//                    ? -20 - percentToBoundary * 20  // -40 to -20
//                    : 10 + percentToBoundary * 20;  // 10 to 30
//        }

        if (lat < PI_6)
        {
            percentToBoundary = lat / PI_6;
            temperature = (isInUpperAtmosphere)
                    ? -20 - percentToBoundary * 35  // -55 to -20
                    : -20 + percentToBoundary * 20; // -20 to 0
        }
        else if (lat < PI_3)
        {
            percentToBoundary = (lat - PI_6) / (PI_3 - PI_6);
            temperature = (isInUpperAtmosphere)
                    ? -10 - percentToBoundary * 20  // -10 to -30
                    : 15 - percentToBoundary * 25;  // 15 to -10
        }
        else
        {
            percentToBoundary = (lat - PI_3) / (PI_2 - PI_3);
            temperature = (isInUpperAtmosphere)
                    ? -20 - percentToBoundary * 20  // -40 to -20
                    : 10 + percentToBoundary * 20;  // 10 to 30
        }

        float randomOffset = ((r.nextFloat() - 0.5f)*4);
        return temperature + randomOffset;
    }

    public void update() {
        float lat = VMath.cartesianToLatitude(Position, PlanetRadius);
        float lon = VMath.cartesianToLongitude(Position);

        float percentToBoundary;
        float lonDelta = .005f;
        float latDelta = .001f;

        // Change position based on layer, belt, coriolis force
        if (lat < PI_6)
        {
            percentToBoundary = lat / PI_6;
            lon += (isInUpperAtmosphere)
                    ?
                    getLonDelta(lon, lonDelta * percentToBoundary)
                    :
                    getLonDelta(lon, -lonDelta * (1 - percentToBoundary))
            ;

            lat += (isInUpperAtmosphere)
                    ? getLatDelta(lat, latDelta)
                    : getLatDelta(lat, -latDelta);
        }
        else if (lat < PI_3)
        {
            percentToBoundary = (lat - PI_6) / (PI_3 - PI_6);
            lon += (isInUpperAtmosphere)
                    ?
                    getLonDelta(lon, -lonDelta * (1 - percentToBoundary))
                    :
                    getLonDelta(lon, lonDelta * percentToBoundary)
            ;

            lat += (isInUpperAtmosphere)
                    ? getLatDelta(lat, -latDelta)
                    : getLatDelta(lat, latDelta);
        }
        else if (lat < PI_2)
        {
            percentToBoundary = (lat - PI_3) / (PI_2 - PI_3);
            lon += (isInUpperAtmosphere)
                    ?
                    getLonDelta(lon, lonDelta * percentToBoundary)
                    :
                    getLonDelta(lon, -lonDelta * (1 - percentToBoundary))
            ;

            lat += (isInUpperAtmosphere)
                    ? getLatDelta(lat, latDelta)
                    : getLatDelta(lat, -latDelta);
        }
        else if (lat < _2PI_3)
        {
            percentToBoundary = (lat - PI_2) / (_2PI_3 - PI_2);
            lon += (isInUpperAtmosphere)
                    ?
                    getLonDelta(lon, lonDelta * (1 - percentToBoundary))
                    :
                    getLonDelta(lon, -lonDelta * percentToBoundary)
            ;

            lat += (isInUpperAtmosphere)
                    ? getLatDelta(lat, -latDelta)
                    : getLatDelta(lat, latDelta);
        }
        else if (lat < _5PI_6)
        {
            percentToBoundary = (lat - _2PI_3) / (_5PI_6 - _2PI_3);
            lon += (isInUpperAtmosphere)
                    ?
                    getLonDelta(lon, -lonDelta * percentToBoundary)
                    :
                    getLonDelta(lon, lonDelta * (1 - percentToBoundary))
            ;

            lat += (isInUpperAtmosphere)
                    ? getLatDelta(lat, latDelta)
                    : getLatDelta(lat, -latDelta);
        }
        else
        {
            percentToBoundary = (lat - _5PI_6) / (float)(Math.PI - _5PI_6);
            lon += (isInUpperAtmosphere)
                    ?
                    getLonDelta(lon, lonDelta * (1 - percentToBoundary))
                    :
                    getLonDelta(lon, -lonDelta * percentToBoundary)
            ;

            lat += (isInUpperAtmosphere)
                    ? getLatDelta(lat, -latDelta)
                    : getLatDelta(lat, latDelta);
        }

        // Change temperature accordingly
        Temperature_C += (isInUpperAtmosphere) ? -1 : 1;

        // Change elevation based on temp
        Elevation_masl += (Temperature_C + 50) * 0.2;

        // Change layer if elevation passes threshold
        isInUpperAtmosphere = (Elevation_masl >= 7000);

        Position = VMath.sphericalToCartesian(lat, lon, PlanetRadius);
    }

    private float getLonDelta(float lon, float delta) {
        if (lon + delta > Math.PI) {
            delta -= 2*Math.PI;
        } else if (lon < -Math.PI) {
            delta += 2*Math.PI;
        }
        return delta;
    }

    private float getLatDelta(float lat, float delta) {
        if (lat + delta > Math.PI) {
            return 0;
        } else if (lat + delta < 0) {
            return 0;
        }
        return delta;
    }
}
