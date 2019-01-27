package com.mygdx.game;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

public class Tile {
    public int centroid;
    public Array<Integer> pts;
    public Array<Tile> nbrs;
    
    public Array<TileAttribute> attributes;
    private float temperature;
    private float elevation;
    private float area;
    private float power;
    private float density;
    private float latitude;
    private float longitude;
    private float thickness;

    public float getTemperature(){
        return temperature;
    }

    public float getElevation(){
        return elevation;
    }

    public float getArea(){
        return area;
    }

    public float getPower(){
        return power;
    }

    public void setPower(float power){
        this.power = power;
    }

    public float getDensity(){
        return density;
    }

    public void setDensity(float density){
        this.density = density;
    }

    public float getLatitude(){
        return latitude;
    }

    public void setLatitude(float latitude){
        this.latitude = latitude;
    }

    public float getLongitude(){
        return longitude;
    }

    public void setLongitude(float longitude){
        this.longitude = longitude;
    }

    public float getThickness(){
        return thickness;
    }

    public void setThickness(float thickness){
        this.thickness = thickness;
    }


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
