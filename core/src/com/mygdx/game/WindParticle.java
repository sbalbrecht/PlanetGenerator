package com.mygdx.game;

import com.badlogic.gdx.math.Vector3;
import com.mygdx.game.util.VMath;

import java.util.Random;

public class WindParticle {
    private Vector3 Position;
    private float Temperature_C;
    public boolean isInUpperAtmosphere;

    public WindParticle(float planetRadius) {
        Random r = new Random();

        float x = (r.nextFloat()-0.5f);
        float y = (r.nextFloat()-0.5f);
        float z = (r.nextFloat()-0.5f);

        Position = new Vector3(x, y, z);
        Position.nor().scl(planetRadius);
        isInUpperAtmosphere = (r.nextFloat() < 0.5f);
        Temperature_C = calculateTemperature(Position);
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

    private float calculateTemperature(Vector3 position) {
        Random r = new Random();
        float lat = VMath.cartesianToLatitude(position);
        if (lat > Math.PI / 2) {
            // reducing range to 0 - PI/2
            lat = (float)(Math.PI - lat);
        }

        final float pi_3 = (float)(Math.PI/3);
        final float _2pi_3 = (float)(2*Math.PI/3);

        float percentToBoundary;
        float temperature;

        if (lat < pi_3) {

            percentToBoundary = lat / pi_3;
            temperature = (isInUpperAtmosphere)
                    ? -20 - percentToBoundary * 35  // -55 to -20
                    : -20 + percentToBoundary * 20; // -20 to 0

        } else if (lat < _2pi_3) {

            percentToBoundary = (lat - pi_3) / pi_3;
            temperature = (isInUpperAtmosphere)
                    ? -10 - percentToBoundary * 20  // -10 to -30
                    : 15 - percentToBoundary * 25;  // 15 to -10

        } else {

            percentToBoundary = (lat - _2pi_3) / pi_3;
            temperature = (isInUpperAtmosphere)
                    ? -20 - percentToBoundary * 20  // -40 to -20
                    : 10 + percentToBoundary * 20;  // 10 to 30

        }

        float randomOffset = ((r.nextFloat() - 0.5f)*4);
        return temperature + randomOffset;
    }
}
