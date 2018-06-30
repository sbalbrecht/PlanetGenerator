package com.mygdx.game;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

import java.util.Arrays;
import java.util.Comparator;

public class TileMap{
	Array<Tile> tiles;
	Array<Tile> tiles_long;
	Array<Tile> tiles_lat;
	//float[] u, v; // u is Longitude, v is latitude. Values from -pi to pi.

	public TileMap(Array<Tile> tiles){
		this.tiles = tiles;
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
				return a.longitude.compareTo(b.longitude);
			}
		});
		tiles_long.addAll(temp);

		for (int i = 0; i < temp.length; i++){
			temp[i] = tiles.get(i);
		}
		Arrays.sort(temp, new Comparator<Tile>(){
			@Override public int compare(Tile a, Tile b){
				return a.latitude.compareTo(b.latitude);
			}
		});
		tiles_lat.addAll(temp);

	}

	public Tile getNearest(float longitude, float latitude){

		if(tiles_lat.size != tiles.size || tiles_lat.size != tiles_long.size){update();}

		float k = tiles.size/MathUtils.PI2;
		int i = MathUtils.floor(k*(longitude+MathUtils.PI));
		int j = MathUtils.floor(k*(latitude+MathUtils.PI));


		int matchlimit = 2;
		int matchcount = 0;
		int counter = 0;
		Tile[] matches = new Tile[matchlimit];


		while(matchcount < matchlimit){
				for (int l = 0-counter; l <= counter; l++){
					for (int m = 0-counter; m <= counter; m++){
						if (tiles_long.get((i+l+tiles.size)%tiles.size) == tiles_lat.get((j+m+tiles.size)%tiles.size) && (matchcount < matchlimit)){
							matches[matchcount] = tiles_lat.get((j+m+tiles.size)%tiles.size);
							matchcount++;
						}
					}
				}
				counter++;
		}


		Vector3 temp = new Vector3(
				MathUtils.sin(latitude)*MathUtils.cos(longitude),
				MathUtils.sin(latitude)*MathUtils.sin(longitude),
				MathUtils.cos(latitude)).nor();
		int bestMatch = 0;
		Vector3 temp2;
		float d;
		float dleast = 0.0f;
		for (int l = 0; l < matches.length; l++){
			temp2 = new Vector3(matches[l].centroid);
			d = temp2.nor().dot(temp);
			if(d < 0) continue;
			else if (d < dleast){
				dleast = d;
				bestMatch = l;
			}
		}

		return matches[bestMatch];
	}


}
