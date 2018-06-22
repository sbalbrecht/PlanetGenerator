package com.mygdx.game;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

public class Tile {
    public Vector3 centroid;
    public Array<Vector3> pts;
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
    public int plateId = -1;

    Tile(Vector3 centroid, Array<Vector3> points) {
        this.centroid = centroid;
        pts = new Array<Vector3>();
        nbrs = new Array<Tile>();
        
        attributes = new Array<TileAttribute>();
            this.temperature = new TileAttribute("Temperature");
            this.elevation = new TileAttribute("Elevation");
            this.area = new TileAttribute("Area");
            this.power = new TileAttribute("Solar Power");
            this.density = new TileAttribute("Density");
            this.latitude = new TileAttribute("Latitude");
            this.longitude = new TileAttribute("Longitude");
        attributes.addAll(temperature, elevation, area, power);
        
        for(Vector3 pt : points) {
            this.pts.add(pt);
        }
        
        Vector3 temp = new Vector3(this.centroid);
        this.longitude.setValue(MathUtils.atan2(temp.y, temp.x));
        this.latitude.setValue(MathUtils.atan2((float)Math.sqrt(temp.x*temp.x + temp.y*temp.y), temp.z));
        
    }

    public void addNbr(Tile tile) {
        nbrs.add(tile);
    }

    public Tile getNbr(Vector3 p1, Vector3 p2) {
        for(Tile nbr : nbrs) {
            if(nbr.pts.contains(p1, false) && nbr.pts.contains(p2, false))
                return nbr;
        }
        return null;
    }
    
    public void addAttribute(TileAttribute a){
        attributes.add(a);
    }

    boolean testNeighbor(Tile b) {
        if(nbrs.contains(b, false))
            return false;
        int count = 0;
        for(int i = 0; i < pts.size; i++) {
            for(int j = 0; j < b.pts.size; j++) {
                if (pts.get(i) == b.pts.get(j)) {
                    count++;
                    break;
                }
            }
            if(count == 2) break;
        }
        if(count == 2) {
            return true;
        } else
            return false;
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
