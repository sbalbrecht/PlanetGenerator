package com.mygdx.game;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;

import java.util.Random;

public class Plate {
    Tile root;          // root plate
    int id;
    Color color;
    Random r = new Random();
    Array<Tile> border = new Array<Tile>();

    Plate(Tile seed, int id) {
        this.root = seed;
        seed.plateId = id;
        this.id = id;
        color = new Color(r.nextFloat(), r.nextFloat(), r.nextFloat(), 1.0f);
        for(Tile nbr : root.nbrs) {
            if(nbr.plateId == -1) {
                border.add(nbr);
            }
        }
    }

    public void grow() {
        if(border.size == 0) return;                    // plate full
        int ind = 0;                                       // closest border tile
        float tmp, currMin = root.centroid.dst(border.get(0).centroid);
        for(int i = 0; i < border.size; i++) {
            tmp = root.centroid.dst(border.get(i).centroid);
            if(tmp < currMin) {
                ind = i;
            }
        }

        if(r.nextInt(100) < 50) ind=r.nextInt(border.size);

        if(border.get(ind).plateId != -1) {             // if tile is no longer avail,
            border.removeIndex(ind);                    //   remove it from borders
            this.grow();                                // try again
        } else {
            border.get(ind).plateId = id;               // set tile to plate
            for(Tile nbr : border.get(ind).nbrs) {      // for each neighbor
                if(nbr.plateId == -1) {                 //   if it is avail
                    border.add(nbr);                    //   add to border
                }
            }
            border.removeIndex(ind);                    // remove curr tile from border
        }
    }
}
