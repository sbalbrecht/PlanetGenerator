package dev.urth.planetgen;

import static dev.urth.planetgen.util.VMath.*;

import com.badlogic.gdx.math.Vector3;
import dev.urth.planetgen.util.VMath;
import java.util.Random;

public class AirParticle {
    private static final Random rand = new Random();
    private final float planetRadius;
    private Vector3 position;
    private float temperatureC;
    private float elevationMasl;
    private boolean isInUpperAtmosphere;

    public AirParticle(float planetRadius) {
        float x = rand.nextFloat() - 0.5f;
        float y = rand.nextFloat() - 0.5f;
        float z = rand.nextFloat() - 0.5f;

        this.planetRadius = planetRadius;
        position = new Vector3(x, y, z);
        position.nor().scl(this.planetRadius);
        elevationMasl = rand.nextFloat() * 14000;
        isInUpperAtmosphere = (elevationMasl >= 7000);
        temperatureC = calculateTemperature();
    }

    public Vector3 getPosition() {
        return position;
    }

    public void setPosition(Vector3 position) {
        this.position = position;
    }

    public float getTemperature() {
        return temperatureC;
    }

    public void setTemperature(float temperatureC) {
        this.temperatureC = temperatureC;
    }

    public boolean isInUpperAtmosphere() {
        return isInUpperAtmosphere;
    }

    public void setInUpperAtmosphere(boolean inUpperAtmosphere) {
        isInUpperAtmosphere = inUpperAtmosphere;
    }

    private float calculateTemperature() {
        float lat = VMath.cartesianToLatitude(position, planetRadius) + (float) (Math.PI / 2);
        if (lat > Math.PI / 2) {
            // reducing range to 0 - PI/2
            lat = (float) (Math.PI - lat);
        }

        float percentToBoundary;
        float temperature;

        // if (lat < PI_3)
        // {
        //     percentToBoundary = lat / PI_3;
        //     temperature = (isInUpperAtmosphere)
        //             ? -20 - percentToBoundary * 35  // -55 to -20
        //             : -20 + percentToBoundary * 20; // -20 to 0
        // }
        // else if (lat < _2PI_3)
        // {
        //     percentToBoundary = (lat - PI_3) / PI_3;
        //     temperature = (isInUpperAtmosphere)
        //             ? -10 - percentToBoundary * 20  // -10 to -30
        //             : 15 - percentToBoundary * 25;  // 15 to -10
        // }
        // else
        // {
        //     percentToBoundary = (lat - _2PI_3) / PI_3;
        //     temperature = (isInUpperAtmosphere)
        //             ? -20 - percentToBoundary * 20  // -40 to -20
        //             : 10 + percentToBoundary * 20;  // 10 to 30
        // }

        if (lat < PI_6) {
            percentToBoundary = lat / PI_6;
            temperature =
                    (isInUpperAtmosphere)
                            ? -20 - percentToBoundary * 35 // -55 to -20
                            : -20 + percentToBoundary * 20; // -20 to 0
        } else if (lat < PI_3) {
            percentToBoundary = (lat - PI_6) / (PI_3 - PI_6);
            temperature =
                    (isInUpperAtmosphere)
                            ? -10 - percentToBoundary * 20 // -10 to -30
                            : 15 - percentToBoundary * 25; // 15 to -10
        } else {
            percentToBoundary = (lat - PI_3) / (PI_2 - PI_3);
            temperature =
                    (isInUpperAtmosphere)
                            ? -20 - percentToBoundary * 20 // -40 to -20
                            : 10 + percentToBoundary * 20; // 10 to 30
        }

        float randomOffset = ((rand.nextFloat() - 0.5f) * 4);
        return temperature + randomOffset;
    }

    public void update() {
        float lat = VMath.cartesianToLatitude(position, planetRadius);
        float lon = VMath.cartesianToLongitude(position);

        float percentToBoundary;
        float lonDelta = .005f;
        float latDelta = .001f;

        // Change position based on layer, belt, coriolis force
        if (lat < PI_6) {
            percentToBoundary = lat / PI_6;
            lon +=
                    (isInUpperAtmosphere)
                            ? getLonDelta(lon, lonDelta * percentToBoundary)
                            : getLonDelta(lon, -lonDelta * (1 - percentToBoundary));

            lat += (isInUpperAtmosphere) ? getLatDelta(lat, latDelta) : getLatDelta(lat, -latDelta);
        } else if (lat < PI_3) {
            percentToBoundary = (lat - PI_6) / (PI_3 - PI_6);
            lon +=
                    (isInUpperAtmosphere)
                            ? getLonDelta(lon, -lonDelta * (1 - percentToBoundary))
                            : getLonDelta(lon, lonDelta * percentToBoundary);

            lat += (isInUpperAtmosphere) ? getLatDelta(lat, -latDelta) : getLatDelta(lat, latDelta);
        } else if (lat < PI_2) {
            percentToBoundary = (lat - PI_3) / (PI_2 - PI_3);
            lon +=
                    (isInUpperAtmosphere)
                            ? getLonDelta(lon, lonDelta * percentToBoundary)
                            : getLonDelta(lon, -lonDelta * (1 - percentToBoundary));

            lat += (isInUpperAtmosphere) ? getLatDelta(lat, latDelta) : getLatDelta(lat, -latDelta);
        } else if (lat < PI2_3) {
            percentToBoundary = (lat - PI_2) / (PI2_3 - PI_2);
            lon +=
                    (isInUpperAtmosphere)
                            ? getLonDelta(lon, lonDelta * (1 - percentToBoundary))
                            : getLonDelta(lon, -lonDelta * percentToBoundary);

            lat += (isInUpperAtmosphere) ? getLatDelta(lat, -latDelta) : getLatDelta(lat, latDelta);
        } else if (lat < PI5_6) {
            percentToBoundary = (lat - PI2_3) / (PI5_6 - PI2_3);
            lon +=
                    (isInUpperAtmosphere)
                            ? getLonDelta(lon, -lonDelta * percentToBoundary)
                            : getLonDelta(lon, lonDelta * (1 - percentToBoundary));

            lat += (isInUpperAtmosphere) ? getLatDelta(lat, latDelta) : getLatDelta(lat, -latDelta);
        } else {
            percentToBoundary = (lat - PI5_6) / (float) (Math.PI - PI5_6);
            lon +=
                    (isInUpperAtmosphere)
                            ? getLonDelta(lon, lonDelta * (1 - percentToBoundary))
                            : getLonDelta(lon, -lonDelta * percentToBoundary);

            lat += (isInUpperAtmosphere) ? getLatDelta(lat, -latDelta) : getLatDelta(lat, latDelta);
        }

        // Change temperature accordingly
        temperatureC += (isInUpperAtmosphere) ? -1 : 1;

        // Change elevation based on temp
        elevationMasl += (temperatureC + 50) * 0.2;

        // Change layer if elevation passes threshold
        isInUpperAtmosphere = (elevationMasl >= 7000);

        position = VMath.sphericalToCartesian(lat, lon, planetRadius);
    }

    private float getLonDelta(float lon, float delta) {
        if (lon + delta > Math.PI) {
            delta -= 2 * Math.PI;
        } else if (lon < -Math.PI) {
            delta += 2 * Math.PI;
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
