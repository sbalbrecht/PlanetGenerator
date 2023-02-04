package dev.urth.planetgen;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import java.util.Map;
import java.util.Random;

public class Plate {
    private static final Random rand = new Random();
    private final Color color;
    private final int id;
    private final Tile root; // isRoot plate

    private final Array<Tile> border = new Array<>();
    private final Array<Tile> members = new Array<>();
    private final Array<Tile> front = new Array<>();

    // Meters per Millennia
    private float speedMPerMa;
    private float densityGmPerCm3;
    private float thicknessKm;
    private float areaKm2;
    // Megagrams == Metric_tons
    private float massMg;
    private float angularSpeedRadPerYr;
    private float momentumCmMgPerYr;

    private Vector3 axisOfRotation;
    private Vector3 angularVelocity;
    private Vector3 tangentialVelocity;
    private Vector3 centerOfMass;
    private boolean isOceanic = true;

    public Plate(Tile seed, int id) {
        this.root = seed;
        this.id = id;
        seed.setRoot(true);
        seed.setPlateId(id);
        color = new Color(rand.nextFloat(), rand.nextFloat(), rand.nextFloat(), 1.0f);
        members.add(seed);
        for (Tile nbr : root.getNeighbors()) {
            if (nbr.getPlateId() == -1) {
                front.add(nbr);
            }
        }
    }

    public Plate(Tile seed, int id, Map<Integer, Plate> newPlates) {
        this.root = seed;
        seed.setRoot(true);
        seed.setPlateId(id);
        this.id = id;
        color = new Color(rand.nextFloat(), rand.nextFloat(), rand.nextFloat(), 1.0f);
        members.add(seed);
        for (Tile nbr : root.getNeighbors()) {
            if (newPlates.get(nbr.getPlateId()) == null) {
                front.add(nbr);
            }
        }
    }

    public void grow(Array<Vector3> pts) {
        // plate full
        if (front.size == 0) {
            return;
        }
        // closest front tile
        int ind = rand.nextInt(front.size);
        float currMin = pts.get(root.getCentroid()).dst(pts.get(front.get(0).getCentroid()));
        for (int i = 0; i < front.size; i++) {
            float tmp = pts.get(root.getCentroid()).dst(pts.get(front.get(i).getCentroid()));
            if (tmp < currMin) {
                ind = i;
            }
        }

        if (rand.nextInt(100) < 60) {
            ind = rand.nextInt(front.size);
        }

        // if tile is no longer avail, remove it from fronts and try again
        if (front.get(ind).getPlateId() != -1) {
            front.removeIndex(ind);
            grow(pts);
        } else {
            // set tile id and add tile to members
            front.get(ind).setPlateId(id);
            members.add(front.get(ind));
            // for each neighbor, if it is avail, add to front
            for (Tile nbr : front.get(ind).getNeighbors()) {
                if (nbr.getPlateId() == -1) {
                    front.add(nbr);
                }
            }
            // remove curr tile from front
            front.removeIndex(ind);
        }
    }

    /** Possibly not needed... perhaps required for a future feature? */
    public void grow(Array<Vector3> pts, Map<Integer, Plate> plates) {
        // plate full
        if (front.size == 0) {
            return;
        }
        // closest front tile
        int ind = 0;
        float currMin = pts.get(root.getCentroid()).dst(pts.get(front.get(0).getCentroid()));
        for (int i = 0; i < front.size; i++) {
            float tmp = pts.get(root.getCentroid()).dst(pts.get(front.get(i).getCentroid()));
            if (tmp < currMin) {
                ind = i;
            }
        }

        if (rand.nextInt(100) < 20) {
            ind = rand.nextInt(front.size);
        }

        // if tile is taken by newPlate, remove it from fronts and try again
        if (plates.get(front.get(ind).getPlateId()) != null) {
            front.removeIndex(ind);
            grow(pts, plates);
        } else {
            // set tile id and add tile to members
            front.get(ind).setPlateId(id);
            members.add(front.get(ind));
            // for each neighbor, if it is avail, add to front
            for (Tile nbr : front.get(ind).getNeighbors()) {
                if (plates.get(nbr.getPlateId()) == null) {
                    front.add(nbr);
                }
            }
            // remove curr tile from front
            front.removeIndex(ind);
        }
    }

    public void createBorder() {
        for (Tile t : members) {
            for (Tile nbr : t.getNeighbors()) {
                if (nbr.getPlateId() != id) {
                    border.add(t);
                    break;
                }
            }
        }
    }

    public void calibrateBorder() {
        boolean isBorder = false;
        Array<Tile> bdrTilesToRmv = new Array<>();
        for (int i = 0; i < border.size; i++) {
            for (Tile nbr : border.get(i).getNeighbors()) {
                if (nbr.getPlateId() != id) {
                    isBorder = true;
                    break;
                }
            }
            if (!isBorder) {
                bdrTilesToRmv.add(border.get(i));
            }
        }
        border.removeAll(bdrTilesToRmv, false);
    }

    public Tile getRoot() {
        return root;
    }

    public int getId() {
        return id;
    }

    public Color getColor() {
        return color;
    }

    public Array<Tile> getBorder() {
        return border;
    }

    public Array<Tile> getMembers() {
        return members;
    }

    public Vector3 getAxisOfRotation() {
        return axisOfRotation;
    }

    public void setAxisOfRotation(Vector3 axisOfRotation) {
        this.axisOfRotation = axisOfRotation;
    }

    public Vector3 getAngularVelocity() {
        return angularVelocity;
    }

    public void setAngularVelocity(Vector3 angularVelocity) {
        this.angularVelocity = angularVelocity;
    }

    public Vector3 getTangentialVelocity() {
        return tangentialVelocity;
    }

    public void setTangentialVelocity(Vector3 tangentialVelocity) {
        this.tangentialVelocity = tangentialVelocity;
    }

    public Vector3 getCenterOfMass() {
        return centerOfMass;
    }

    public void setCenterOfMass(Vector3 centerOfMass) {
        this.centerOfMass = centerOfMass;
    }

    public float getSpeedMPerMa() {
        return speedMPerMa;
    }

    public void setSpeedMPerMa(float speedMPerMa) {
        this.speedMPerMa = speedMPerMa;
    }

    public float getDensityGmPerCm3() {
        return densityGmPerCm3;
    }

    public void setDensityGmPerCm3(float densityGmPerCm3) {
        this.densityGmPerCm3 = densityGmPerCm3;
    }

    public float getThicknessKm() {
        return thicknessKm;
    }

    public void setThicknessKm(float thicknessKm) {
        this.thicknessKm = thicknessKm;
    }

    public float getAreaKm2() {
        return areaKm2;
    }

    public void setAreaKm2(float areaKm2) {
        this.areaKm2 = areaKm2;
    }

    public float getMassMg() {
        return massMg;
    }

    public void setMassMg(float massMg) {
        this.massMg = massMg;
    }

    public float getAngularSpeedRadPerYr() {
        return angularSpeedRadPerYr;
    }

    public void setAngularSpeedRadPerYr(float angularSpeedRadPerYr) {
        this.angularSpeedRadPerYr = angularSpeedRadPerYr;
    }

    public float getMomentumCmMgPerYr() {
        return momentumCmMgPerYr;
    }

    public void setMomentumCmMgPerYr(float momentumCmMgPerYr) {
        this.momentumCmMgPerYr = momentumCmMgPerYr;
    }

    public boolean isOceanic() {
        return isOceanic;
    }

    public void setOceanic(boolean isOceanic) {
        this.isOceanic = isOceanic;
    }
}
