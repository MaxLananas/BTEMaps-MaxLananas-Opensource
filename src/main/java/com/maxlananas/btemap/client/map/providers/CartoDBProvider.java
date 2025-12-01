package com.maxlananas.btemap.client.map.providers;

import com.maxlananas.btemap.client.map.MapProvider;
import com.maxlananas.btemap.util.WebHelper;

import java.awt.image.BufferedImage;
import java.util.concurrent.CompletableFuture;

public class CartoDBProvider implements MapProvider {
    
    public enum MapType {
        LIGHT("light_all", "Light"),
        DARK("dark_all", "Dark"),
        VOYAGER("rastertiles/voyager", "Voyager");
        
        private final String code;
        private final String displayName;
        
        MapType(String code, String displayName) {
            this.code = code;
            this.displayName = displayName;
        }
        
        public String getCode() { return code; }
        public String getDisplayName() { return displayName; }
    }
    
    private MapType mapType = MapType.VOYAGER;
    
    @Override
    public String getName() {
        return "Carto " + mapType.getDisplayName();
    }
    
    @Override
    public String getId() {
        return "carto";
    }
    
    @Override
    public String getIcon() {
        return "🎨";
    }
    
    @Override
    public CompletableFuture<BufferedImage> getTile(int x, int y, int zoom) {
        return WebHelper.downloadImage(getTileUrl(x, y, zoom));
    }
    
    @Override
    public String getTileUrl(int x, int y, int zoom) {
        String subdomain = new String[]{"a", "b", "c", "d"}[(x + y) % 4];
        return String.format(
            "https://%s.basemaps.cartocdn.com/%s/%d/%d/%d.png",
            subdomain, mapType.getCode(), zoom, x, y
        );
    }
    
    @Override
    public int getMaxZoom() {
        return 20;
    }
    
    @Override
    public int getMinZoom() {
        return 0;
    }
    
    @Override
    public float getBrightness() {
        return mapType == MapType.DARK ? 1.3f : 1.0f;
    }
    
    public void setMapType(MapType type) {
        this.mapType = type;
    }
    
    public MapType getMapType() {
        return mapType;
    }
}