package com.mygdx.game;

import com.badlogic.gdx.math.MathUtils;
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
    public TileAttribute latitude;
    public TileAttribute longitude;

    public boolean drawn = false;
    public boolean root = false;
    public int plateId = -1;

    Tile(int centroid, Array<Integer> tilePts, Array<Vector3> allPoints) {
        this.centroid = centroid;
        pts = new Array<Integer>();
        nbrs = new Array<Tile>();
        
        attributes = new Array<TileAttribute>();
            temperature = new TileAttribute("Temperature");
            elevation = new TileAttribute("Elevation");
            area = new TileAttribute("Area");
            power = new TileAttribute("Solar Power");
            density = new TileAttribute("Density");
            latitude = new TileAttribute("Latitude");
            longitude = new TileAttribute("Longitude");
        attributes.addAll(temperature, elevation, area, power);
        
        for(Integer pt : tilePts) {
            pts.add(pt);
        }

        Vector3 temp = new Vector3(allPoints.get(this.centroid));
        longitude.setValue(MathUtils.atan2(temp.y, temp.x));
        latitude.setValue(MathUtils.atan2((float)Math.sqrt(temp.x*temp.x + temp.y*temp.y), temp.z));
        
    }

//    public void addNbr(Tile tile) {
//        nbrs.add(tile);
//    }

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
