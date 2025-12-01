package com.maxlananas.btemap.client.map.providers;

import com.maxlananas.btemap.client.map.MapProvider;
import com.maxlananas.btemap.util.WebHelper;

import java.awt.image.BufferedImage;
import java.util.concurrent.CompletableFuture;

public class OSMProvider implements MapProvider {
    
    private static final String[] TILE_SERVERS = {
        "https://a.tile.openstreetmap.org",
        "https://b.tile.openstreetmap.org",
        "https://c.tile.openstreetmap.org"
    };
    
    private int serverIndex = 0;
    
    @Override
    public String getName() {
        return "OpenStreetMap";
    }
    
    @Override
    public String getId() {
        return "osm";
    }
    
    @Override
    public String getIcon() {
        return "🗺️";
    }
    
    @Override
    public CompletableFuture<BufferedImage> getTile(int x, int y, int zoom) {
        return WebHelper.downloadImage(getTileUrl(x, y, zoom));
    }
    
    @Override
    public String getTileUrl(int x, int y, int zoom) {
        String server = TILE_SERVERS[serverIndex % TILE_SERVERS.length];
        serverIndex++;
        return String.format("%s/%d/%d/%d.png", server, zoom, x, y);
    }
    
    @Override
    public int getMaxZoom() {
        return 19;
    }
    
    @Override
    public int getMinZoom() {
        return 0;
    }
    
    @Override
    public float getBrightness() {
        return 1.0f;
    }
}