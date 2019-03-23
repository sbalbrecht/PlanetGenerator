package com.mygdx.game;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.mygdx.game.util.VMath;

import java.util.Random;

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
        float lat = VMath.cartesianToLatitude(Position, PlanetRadius);
        if (lat > Math.PI / 2) {
            // reducing range to 0 - PI/2
            lat = (float)(Math.PI - lat);
        }

        final float pi_3 = (float)(Math.PI/3);
        final float _2pi_3 = (float)(2*Math.PI/3);

        float percentToBoundary;
        float temperature;

        if (lat < pi_3)
        {
            percentToBoundary = lat / pi_3;
            temperature = (isInUpperAtmosphere)
                    ? -20 - percentToBoundary * 35  // -55 to -20
                    : -20 + percentToBoundary * 20; // -20 to 0
        }
        else if (lat < _2pi_3)
        {
            percentToBoundary = (lat - pi_3) / pi_3;
            temperature = (isInUpperAtmosphere)
                    ? -10 - percentToBoundary * 20  // -10 to -30
                    : 15 - percentToBoundary * 25;  // 15 to -10
        }
        else
        {
            percentToBoundary = (lat - _2pi_3) / pi_3;
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

        final float pi_3 = (float)(Math.PI/3);
        final float _2pi_3 = (float)(2*Math.PI/3);

        float percentToBoundary;

        lat += (isInUpperAtmosphere) ? .01 : -.01;
        lon += 0.01;
        if (lon > MathUtils.PI) {
            lon -= 2*MathUtils.PI;
        }

        Position = VMath.latitudeToCartesian(lat, lon, PlanetRadius);


        // Change position based on layer, belt, coriolis force
        // Change temperature accordingly
        // Change elevation based on temp
        // Change layer if elevation passes threshold
    }
}
