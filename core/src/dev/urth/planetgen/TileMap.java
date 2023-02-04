package dev.urth.planetgen;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import java.util.Arrays;
import java.util.Comparator;

public class TileMap {
    private final Array<Tile> tiles;
    private final float planetRadius2;
    private final float range;
    private Array<Tile> tilesLong;
    private Array<Tile> tilesLat;

    private final Comparator<Tile> compareLongitudes =
            (a, b) -> Float.compare(a.getLongitude(), b.getLongitude());

    private final Comparator<Tile> compareLatitudes =
            (a, b) -> Float.compare(a.getLatitude(), b.getLatitude());

    // Latitude ranges from 0 (north) to pi (south)
    // Longitude ranges from -pi to pi
    public TileMap(Array<Tile> tiles, float planetRadius) {
        this.tiles = tiles;
        this.planetRadius2 = planetRadius * planetRadius;

        // Calculate range of indices to look at when searching for nearest tiles
        // Power formula derived from graph
        range = (float) (5.158 * Math.pow(tiles.size, -0.51)) + 0.01f;
        buildTileMap();
    }

    public void buildTileMap() {
        tilesLat = new Array<>();
        tilesLong = new Array<>();
        tilesLat.ensureCapacity(tiles.size);
        tilesLong.ensureCapacity(tiles.size);

        Tile[] sortedTiles = new Tile[tiles.size];
        for (int i = 0; i < sortedTiles.length; i++) {
            sortedTiles[i] = tiles.get(i);
        }

        Arrays.sort(sortedTiles, compareLatitudes);
        tilesLat.addAll(sortedTiles);

        Arrays.sort(sortedTiles, compareLongitudes);
        tilesLong.addAll(sortedTiles);
    }

    public Tile getNearest(float latitude, float longitude) {

        // Find range of indices to search for each array
        Array<Tile> subset = new Array<>();
        float percentOfLat = latitude / MathUtils.PI;
        float percentOfLong = (longitude + MathUtils.PI) / MathUtils.PI2;
        int minLatIndex = (int) (percentOfLat - range) * tiles.size;
        int maxLatIndex = (int) (percentOfLat + range) * tiles.size;
        int minLongIndex = (int) (percentOfLong - range) * tiles.size;
        int maxLongIndex = (int) (percentOfLong + range) * tiles.size;

        if (minLatIndex < 0) {
            minLatIndex = 0;
        }
        if (maxLatIndex >= tiles.size) {
            maxLatIndex = tiles.size - 1;
        }

        // Form subset of guess of the closest tiles
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

                if (tilesLat.get(i) == tilesLong.get(index)) {
                    subset.add(tilesLat.get(i));
                }
            }
        }

        // Find minimum distance of the subset
        Tile closestTile = tiles.get(0);
        float minDist =
                getDistanceLatLong(
                        latitude, longitude, closestTile.getLatitude(), closestTile.getLongitude());
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
        return (x * x + y * y) * planetRadius2;
    }
}
