package com.mygdx.game;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

public class Tile {
    public int centroid;
    public Array<Integer> pts;
    public Array<Tile> nbrs;
    
    public Array<TileAttribute> attributes;
    public TileAttribute temperature;
    public TileAttribute elevation;
    public TileAttribute area;
    public TileAttribute power;
    public TileAttribute density;

    public boolean drawn = false;
    public boolean root = false;
    public int plateId = -1;

    Tile(int centroid, Array<Integer> points) {
        this.centroid = centroid;
        pts = new Array<Integer>();
        nbrs = new Array<Tile>();
        
        attributes = new Array<TileAttribute>();
            this.temperature = new TileAttribute("Temperature");
            this.elevation = new TileAttribute("Elevation");
            this.area = new TileAttribute("Area");
            this.power = new TileAttribute("Solar Power");
            this.density = new TileAttribute("Density");
        attributes.addAll(temperature, elevation, area, power);
        
        for(Integer pt : points) {
            this.pts.add(pt);
        }
    }

    public void addNbr(Tile tile) {
        nbrs.add(tile);
    }

    public Tile getNbr(int p1, int p2) {
        for(Tile nbr : nbrs) {
            if(nbr.pts.contains(p1, false) && nbr.pts.contains(p2, false))
                return nbr;
        }
        return null;
    }
    
    public void addAttribute(TileAttribute a){
        attributes.add(a);
    }

    public void setTemperature(float t){
        if(temperature == null) {temperature = new TileAttribute("Temperature", t);attributes.add(temperature);}
        else temperature.setValue(t);
        
    }
    public void setElevation(float e){
        if(elevation == null) elevation = new TileAttribute("Elevation", e);
        else elevation.setValue(e);
    }
    
    public String getAttributes(){
        String out = "";
        for (TileAttribute a : attributes){
            out += a.toString() + "\n";
        }
        return out;
    }
    
    
}
