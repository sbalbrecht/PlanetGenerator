package com.mygdx.game;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

public class Tile {
    public int centroid;
    public Array<Integer> pts;
    public Array<Tile> nbrs;
    
    public Array<TileAttribute> attributes;
    public float temperature;
    public float elevation;
    public float area;
    public float power;
    public float density;
    public float latitude;
    public float longitude;
    public float thickness;
    
    public Vector3 tangentialVelocity;

    public boolean drawn = false;
    public boolean root = false;
    public int plateId = -1;

    Tile(int centroid, Array<Integer> tilePts, Array<Vector3> allPoints) {
        this.centroid = centroid;
        pts = new Array<Integer>();
        nbrs = new Array<Tile>();

        
        for(Integer pt : tilePts) {
            pts.add(pt);
        }

        Vector3 temp = new Vector3(allPoints.get(this.centroid));
        longitude = MathUtils.atan2(temp.z, temp.x);
        latitude = MathUtils.atan2((float)Math.sqrt(temp.x*temp.x + temp.z*temp.z), temp.y);
        
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
       this.temperature = t;
    }
    
    public void setElevation(float e){
        this.elevation = e;
    }
    
    public void setArea(float a){
        this.area = a;
    }
    
    
}
