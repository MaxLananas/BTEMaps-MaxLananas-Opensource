package com.maxlananas.btemap.client.map.providers;

import com.maxlananas.btemap.client.map.MapProvider;
import com.maxlananas.btemap.util.WebHelper;

import java.awt.image.BufferedImage;
import java.util.concurrent.CompletableFuture;

public class GoogleMapsProvider implements MapProvider {
    
    public enum MapType {
        ROADMAP("m", "Roads"),
        SATELLITE("s", "Satellite"),
        TERRAIN("p", "Terrain"),
        HYBRID("y", "Hybrid");
        
        private final String code;
        private final String displayName;
        
        MapType(String code, String displayName) {
            this.code = code;
            this.displayName = displayName;
        }
        
        public String getCode() { return code; }
        public String getDisplayName() { return displayName; }
    }
    
    private MapType mapType = MapType.SATELLITE;
    
    @Override
    public String getName() {
        return "Google " + mapType.getDisplayName();
    }
    
    @Override
    public String getId() {
        return "google";
    }
    
    @Override
    public String getIcon() {
        return "🌐";
    }
    
    @Override
    public CompletableFuture<BufferedImage> getTile(int x, int y, int zoom) {
        return WebHelper.downloadImage(getTileUrl(x, y, zoom));
    }
    
    @Override
    public String getTileUrl(int x, int y, int zoom) {
        int server = (x + y) % 4;
        return String.format(
            "https://mt%d.google.com/vt/lyrs=%s&x=%d&y=%d&z=%d",
            server, mapType.getCode(), x, y, zoom
        );
    }
    
    @Override
    public int getMaxZoom() {
        return 21;
    }
    
    @Override
    public int getMinZoom() {
        return 0;
    }
    
    @Override
    public float getBrightness() {
        return 1.2f; // Plus lumineux !
    }
    
    public void setMapType(MapType type) {
        this.mapType = type;
    }
    
    public MapType getMapType() {
        return mapType;
    }
}