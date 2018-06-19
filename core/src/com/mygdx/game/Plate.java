package com.mygdx.game;

import com.badlogic.gdx.utils.Array;

import java.awt.*;
import java.util.Random;

public class Plate {
    Tile root;          // root plate
    int id;
    Color color;
    Random r = new Random();
    Array<Tile> border = new Array<Tile>();

    Plate(Tile root) {
        this.root = root;
        id = r.nextInt(0xffffff);
        color = new Color(r.nextFloat(), r.nextFloat(), r.nextFloat());
    }

    public void grow() {

    }
}
