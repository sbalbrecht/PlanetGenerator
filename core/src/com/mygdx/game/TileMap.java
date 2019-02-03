package com.mygdx.game;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.mygdx.game.util.Log;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;

public class TileMap{
	Array<Tile> tiles;
	Array<Tile> tiles_long;
	Array<Tile> tiles_lat;
	float planetRadius2;
	float range;
	//float[] u, v; // u is Longitude, v is latitude. Values from -pi to pi.

	public TileMap(Array<Tile> tiles, float planetRadius){
	    this.planetRadius2 = planetRadius*planetRadius;
		this.tiles = tiles;
        range = (float)(5.158*Math.pow(tiles.size, -0.51));
        System.out.println(range);
        tiles_lat = new Array<Tile>();
		tiles_long = new Array<Tile>();
		tiles_long.ensureCapacity(tiles.size);
		tiles_lat.ensureCapacity(tiles.size);
		update();
	}

	public void update(){
		tiles_lat.clear();
		tiles_long.clear();
		//1: sort copy of array by longitudes
		//2: sort copy of array by latitudes
		Tile[] temp = new Tile[tiles.size];
		for (int i = 0; i < temp.length; i++){
			temp[i] = tiles.get(i);
		}

		Arrays.sort(temp, new Comparator<Tile>(){
			@Override public int compare(Tile a, Tile b){
				if(a.getLongitude() < b.getLongitude()) return -1;
				if(a.getLongitude() > b.getLongitude()) return 1;
				else return 0;
			}
		});
		tiles_long.addAll(temp);

		for (int i = 0; i < temp.length; i++){
			temp[i] = tiles.get(i);
		}
		Arrays.sort(temp, new Comparator<Tile>(){
			@Override public int compare(Tile a, Tile b){
				if(a.getLatitude() < b.getLatitude()) return -1;
				if(a.getLatitude() > b.getLatitude()) return 1;
				else return 0;
			}
		});
		tiles_lat.addAll(temp);
	}

	public Tile getNearest(float latitude, float longitude) {

        // Find range of indices to search for each array
        Array<Tile> subset = new Array<Tile>();
	    float percentOfLat = (float)(latitude / MathUtils.PI);
	    float percentOfLong = (float)((longitude + MathUtils.PI) / MathUtils.PI2);
	    int minLatIndex = (int)((percentOfLat-range) * tiles.size);
	    int maxLatIndex = (int)((percentOfLat+range) * tiles.size);
	    int minLongIndex = (int)((percentOfLong-range) * tiles.size);
	    int maxLongIndex = (int)((percentOfLong+range) * tiles.size);

	    if (minLatIndex < 0) {
	        minLatIndex = 0;
        }
        if (maxLatIndex >= tiles.size) {
	        maxLatIndex = tiles.size-1;
        }

        // Form subset of guess of closest tiles
        int longIndexRange = maxLongIndex - minLongIndex;
        for (int i = minLatIndex; i < maxLatIndex; i++) {
            for (int j = 0; j < longIndexRange; j++) {
                int currentIndex = minLongIndex + j;
                int index = currentIndex;

                // long indexing must wrap around; could be negative or positive
                if (currentIndex < 0) {
                    index = tiles.size + currentIndex;
                } else if (currentIndex >= tiles.size) {
                    index = currentIndex % tiles.size;
                }

                if (tiles_lat.get(i) == tiles_long.get(index)) {
                    subset.add(tiles_lat.get(i));
                }
            }
        }

        // Find minimum distance of the subset
        if (subset.size == 0) {
            System.out.println("No matches found");
        }
        Tile closestTile = tiles.get(0);
        float minDist = getDistanceLatLong(latitude, longitude, closestTile.getLatitude(), closestTile.getLongitude());
        for (Tile t : subset) {
            float dist = getDistanceLatLong(latitude, longitude, t.getLatitude(), t.getLongitude());
            if (dist < minDist) {
                minDist = dist;
                closestTile = t;
            }
        }

        return closestTile;
    }

    // Uses flat earth approximation to reduce calculation time
    private float getDistanceLatLong(float lat1, float long1, float lat2, float long2) {
        float x = (long1 - long2) * MathUtils.cos(lat1);
        float y = (lat1 - lat2);
        return (x*x + y*y) * planetRadius2;
    }
}
