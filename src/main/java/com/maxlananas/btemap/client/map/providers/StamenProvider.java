package com.maxlananas.btemap.client.map.providers;

import com.maxlananas.btemap.client.map.MapProvider;
import com.maxlananas.btemap.util.WebHelper;

import java.awt.image.BufferedImage;
import java.util.concurrent.CompletableFuture;

public class StamenProvider implements MapProvider {
    
    public enum MapType {
        WATERCOLOR("watercolor", "Watercolor"),
        TONER("toner", "Toner"),
        TERRAIN("terrain", "Terrain");
        
        private final String code;
        private final String displayName;
        
        MapType(String code, String displayName) {
            this.code = code;
            this.displayName = displayName;
        }
        
        public String getCode() { return code; }
        public String getDisplayName() { return displayName; }
    }
    
    private MapType mapType = MapType.WATERCOLOR;
    
    @Override
    public String getName() {
        return "Stamen " + mapType.getDisplayName();
    }
    
    @Override
    public String getId() {
        return "stamen";
    }
    
    @Override
    public String getIcon() {
        return "🖼️";
    }
    
    @Override
    public CompletableFuture<BufferedImage> getTile(int x, int y, int zoom) {
        return WebHelper.downloadImage(getTileUrl(x, y, zoom));
    }
    
    @Override
    public String getTileUrl(int x, int y, int zoom) {
        // Stamen tiles via Stadia Maps
        return String.format(
            "https://tiles.stadiamaps.com/tiles/stamen_%s/%d/%d/%d.png",
            mapType.getCode(), zoom, x, y
        );
    }
    
    @Override
    public int getMaxZoom() {
        return mapType == MapType.WATERCOLOR ? 18 : 20;
    }
    
    @Override
    public int getMinZoom() {
        return 0;
    }
    
    @Override
    public float getBrightness() {
        return 1.1f;
    }
    
    public void setMapType(MapType type) {
        this.mapType = type;
    }
    
    public MapType getMapType() {
        return mapType;
    }
}