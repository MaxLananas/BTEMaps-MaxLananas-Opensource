package com.maxlananas.btemap.client.map.providers;

import com.maxlananas.btemap.client.map.MapProvider;
import com.maxlananas.btemap.util.WebHelper;

import java.awt.image.BufferedImage;
import java.util.concurrent.CompletableFuture;

public class EsriProvider implements MapProvider {
    
    public enum MapType {
        WORLD_IMAGERY("World_Imagery", "Satellite"),
        WORLD_STREET("World_Street_Map", "Streets"),
        WORLD_TOPO("World_Topo_Map", "Topographic"),
        NATGEO("NatGeo_World_Map", "NatGeo");
        
        private final String code;
        private final String displayName;
        
        MapType(String code, String displayName) {
            this.code = code;
            this.displayName = displayName;
        }
        
        public String getCode() { return code; }
        public String getDisplayName() { return displayName; }
    }
    
    private MapType mapType = MapType.WORLD_IMAGERY;
    
    @Override
    public String getName() {
        return "Esri " + mapType.getDisplayName();
    }
    
    @Override
    public String getId() {
        return "esri";
    }
    
    @Override
    public String getIcon() {
        return "🛰️";
    }
    
    @Override
    public CompletableFuture<BufferedImage> getTile(int x, int y, int zoom) {
        return WebHelper.downloadImage(getTileUrl(x, y, zoom));
    }
    
    @Override
    public String getTileUrl(int x, int y, int zoom) {
        return String.format(
            "https://server.arcgisonline.com/ArcGIS/rest/services/%s/MapServer/tile/%d/%d/%d",
            mapType.getCode(), zoom, y, x
        );
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
        return 1.15f;
    }
    
    public void setMapType(MapType type) {
        this.mapType = type;
    }
    
    public MapType getMapType() {
        return mapType;
    }
}