package dev.urth.planetgen;

import com.badlogic.gdx.math.Vector3;

public class Sun {
    private Vector3 position;
    private float totalPower;

    public Sun(Vector3 pos, float totalPower) {
        this.position = pos;
        this.totalPower = totalPower;
    }

    public Vector3 getPosition() {
        return position;
    }

    public void setPosition(Vector3 position) {
        this.position = position;
    }

    public float getTotalPower() {
        return totalPower;
    }

    public void setTotalPower(float totalPower) {
        this.totalPower = totalPower;
    }
}
