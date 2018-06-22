package com.mygdx.game;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;

import java.util.Random;

public class Plate {
    Tile root;          // root plate
    int id;
    Color color;
    float percentArea;
    Random r = new Random();
    Array<Tile> border = new Array<Tile>();
    Array<Tile> members = new Array<Tile>();
    Array<Tile> front = new Array<Tile>();

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

    public void grow() {
        if(front.size == 0) return;                    // plate full
        int ind = 0;                                       // closest front tile
        float tmp, currMin = root.centroid.dst(front.get(0).centroid);
        for(int i = 0; i < front.size; i++) {
            tmp = root.centroid.dst(front.get(i).centroid);
            if(tmp < currMin) {
                ind = i;
            }
        }

        if(r.nextInt(100) < 60) ind=r.nextInt(front.size);

        if(front.get(ind).plateId != -1) {             // if tile is no longer avail,
            front.removeIndex(ind);                    //   remove it from fronts
            this.grow();                                // try again
        } else {
            front.get(ind).plateId = id;               // set tile id
            members.add(front.get(ind));               // add tile to members
            for(Tile nbr : front.get(ind).nbrs) {      // for each neighbor
                if(nbr.plateId == -1) {                 //   if it is avail
                    front.add(nbr);                    //   add to front
                }
            }
            border.add(front.get(ind));                // add tile to border
            front.removeIndex(ind);                    // remove curr tile from front
        }
    }
}
