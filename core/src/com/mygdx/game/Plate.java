package com.mygdx.game;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

import java.util.Map;
import java.util.Random;

public class Plate {
    Tile root;          // root plate
    int id;
    Color color;
    Random r = new Random();
    
    //TODO
    Array<Tile> border = new Array<Tile>();
    Array<Tile> members = new Array<Tile>();
    private Array<Tile> front = new Array<Tile>();

    Vector3 axisOfRotation;
    
    float speed_m_Ma;
    float density_gm_cm3;
    float thickness_km;
    float area_km2;
    float mass_Mg;              //Megagrams == Metric_tons
    float angularSpeed_rad_yr;
    float momentum_cmMg_yr;
    
    Vector3 angularVelocity;
    Vector3 tangentialVelocity;
    Vector3 centerOfMass;
    
    boolean oceanic = true;

    Plate(Tile seed, int id) {
        this.root = seed;
        seed.root = true;
        seed.plateId = id;
        this.id = id;
        color = new Color(r.nextFloat(), r.nextFloat(), r.nextFloat(), 1.0f);
        members.add(seed);
        for(Tile nbr : root.nbrs) {
            if(nbr.plateId == -1) {
                front.add(nbr);
            }
        }
    }

    Plate(Tile seed, int id, Map<Integer, Plate> newPlates) {
        this.root = seed;
        seed.root = true;
        seed.plateId = id;
        this.id = id;
        color = new Color(r.nextFloat(), r.nextFloat(), r.nextFloat(), 1.0f);
        members.add(seed);
        for(Tile nbr : root.nbrs) {
            if(newPlates.get(nbr.plateId) == null) {
                front.add(nbr);
            }
        }
    }

    public void grow(Array<Vector3> pts) {
        if(front.size == 0) return;                    // plate full
        int ind = r.nextInt(front.size);;              // closest front tile
        float tmp, currMin = pts.get(root.centroid).dst(pts.get(front.get(0).centroid));
        for(int i = 0; i < front.size; i++) {
            tmp = pts.get(root.centroid).dst(pts.get(front.get(i).centroid));
            if(tmp < currMin) {
                ind = i;
            }
        }

        if(r.nextInt(100) < 60) ind=r.nextInt(front.size);

        if(front.get(ind).plateId != -1) {             // if tile is no longer avail,
            front.removeIndex(ind);                    //   remove it from fronts
            this.grow(pts);                            // try again
        } else {
            front.get(ind).plateId = id;               // set tile id
            members.add(front.get(ind));               // add tile to members
            for(Tile nbr : front.get(ind).nbrs) {      // for each neighbor
                if(nbr.plateId == -1) {                 //   if it is avail
                    front.add(nbr);                    //   add to front
                }
            }
            front.removeIndex(ind);                    // remove curr tile from front
        }
    }

    public void grow(Array<Vector3> pts, Map<Integer, Plate> plates) {
        if(front.size == 0) return;                    // plate full
        int ind = 0;                                       // closest front tile
        float tmp, currMin = pts.get(root.centroid).dst(pts.get(front.get(0).centroid));
        for(int i = 0; i < front.size; i++) {
            tmp = pts.get(root.centroid).dst(pts.get(front.get(i).centroid));
            if(tmp < currMin) {
                ind = i;
            }
        }

        if(r.nextInt(100) < 20) ind = r.nextInt(front.size);

        if(plates.get(front.get(ind).plateId) != null) {// if tile is taken by newPlate,
            front.removeIndex(ind);                     //   remove it from fronts
            grow(pts, plates);                             // try again
        } else {
            front.get(ind).plateId = id;               // set tile id
            members.add(front.get(ind));               // add tile to members
            for(Tile nbr : front.get(ind).nbrs) {      // for each neighbor
                if(plates.get(nbr.plateId) == null) {  //   if it is avail
                    front.add(nbr);                    //   add to front
                }
            }
            front.removeIndex(ind);                    // remove curr tile from front
        }
    }

    public void createBorder() {
        for(Tile t : members) {
            for(Tile nbr : t.nbrs) {
                if(nbr.plateId != id) {
                    border.add(t);
                    break;
                }
            }
        }
    }

    public void calibrateBorder() {
        boolean isBorder = false;
        Array<Tile> bdrTilesToRmv = new Array<Tile>();
        for(int i = 0; i < border.size; i++) {
            for(Tile nbr : border.get(i).nbrs) {
                if(nbr.plateId != id) {
                    isBorder = true;
                    break;
                }
            }
            if(!isBorder) {
                bdrTilesToRmv.add(border.get(i));
            }
        }
        border.removeAll(bdrTilesToRmv, false);
    }
}
