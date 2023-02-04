package dev.urth.planetgen;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import dev.urth.planetgen.util.VMath;

public class Tile {
    private final int centroid;
    private final Array<Integer> points;
    private final Array<Tile> neighbors;
    private final float latitude;
    private final float longitude;
    private Array<TileAttribute> attributes;
    private float elevationMasl;
    private float temperature;
    private float area;
    private float power;
    private float density;
    private float thickness;
    private Vector3 tangentialVelocity;
    private boolean isDrawn = false;
    private boolean isRoot = false;
    private int plateId = -1;

    public Tile(int centroid, Array<Integer> points, Array<Vector3> allPoints) {
        this.centroid = centroid;
        this.points = new Array<>(points);
        neighbors = new Array<>();

        Vector3 temp = new Vector3(allPoints.get(this.centroid));
        longitude = VMath.cartesianToLongitude(temp);
        latitude = VMath.cartesianToLatitude(temp, 10f);
    }

    public int getCentroid() {
        return centroid;
    }

    public Array<Integer> getPoints() {
        return points;
    }

    public Array<Tile> getNeighbors() {
        return neighbors;
    }

    public float getTemperature() {
        return temperature;
    }

    public float getElevationMasl() {
        return elevationMasl;
    }

    public float getArea() {
        return area;
    }

    public float getPower() {
        return power;
    }

    public void setPower(float power) {
        this.power = power;
    }

    public float getDensity() {
        return density;
    }

    public void setDensity(float density) {
        this.density = density;
    }

    public float getLatitude() {
        return latitude;
    }

    public float getLongitude() {
        return longitude;
    }

    public float getThickness() {
        return thickness;
    }

    public void setThickness(float thickness) {
        this.thickness = thickness;
    }

    public Array<TileAttribute> getAttributes() {
        return attributes;
    }

    public void setAttributes(Array<TileAttribute> attributes) {
        this.attributes = attributes;
    }

    public Vector3 getTangentialVelocity() {
        return tangentialVelocity;
    }

    public void setTangentialVelocity(Vector3 tangentialVelocity) {
        this.tangentialVelocity = tangentialVelocity;
    }

    public boolean isDrawn() {
        return isDrawn;
    }

    public void setDrawn(boolean drawn) {
        isDrawn = drawn;
    }

    public boolean isRoot() {
        return isRoot;
    }

    public void setRoot(boolean root) {
        isRoot = root;
    }

    public int getPlateId() {
        return plateId;
    }

    public void setPlateId(int plateId) {
        this.plateId = plateId;
    }

    public Tile getNeighbor(int p1, int p2) {
        for (Tile nbr : neighbors) {
            if (nbr.points.contains(p1, false) && nbr.points.contains(p2, false)) {
                return nbr;
            }
        }
        return null;
    }

    public void addAttribute(TileAttribute a) {
        attributes.add(a);
    }

    public void setTemperature(float t) {
        this.temperature = t;
    }

    public void setElevationMasl(float e) {
        this.elevationMasl = e;
    }

    public void setArea(float a) {
        this.area = a;
    }
}
