package com.maxlananas.btemap.client.map.providers;

import com.maxlananas.btemap.client.map.MapProvider;
import com.maxlananas.btemap.util.WebHelper;

import java.awt.image.BufferedImage;
import java.util.concurrent.CompletableFuture;

public class BingMapsProvider implements MapProvider {
    
    public enum MapType {
        AERIAL("a", "Aerial"),
        ROAD("r", "Road"),
        HYBRID("h", "Hybrid");
        
        private final String code;
        private final String displayName;
        
        MapType(String code, String displayName) {
            this.code = code;
            this.displayName = displayName;
        }
        
        public String getCode() { return code; }
        public String getDisplayName() { return displayName; }
    }
    
    private MapType mapType = MapType.AERIAL;
    
    @Override
    public String getName() {
        return "Bing " + mapType.getDisplayName();
    }
    
    @Override
    public String getId() {
        return "bing";
    }
    
    @Override
    public String getIcon() {
        return "🅱️";
    }
    
    @Override
    public CompletableFuture<BufferedImage> getTile(int x, int y, int zoom) {
        return WebHelper.downloadImage(getTileUrl(x, y, zoom));
    }
    
    @Override
    public String getTileUrl(int x, int y, int zoom) {
        String quadKey = tileToQuadKey(x, y, zoom);
        int server = (x + y) % 4;
        return String.format(
            "https://ecn.t%d.tiles.virtualearth.net/tiles/%s%s.jpeg?g=1",
            server, mapType.getCode(), quadKey
        );
    }
    
    private String tileToQuadKey(int x, int y, int zoom) {
        StringBuilder quadKey = new StringBuilder();
        for (int i = zoom; i > 0; i--) {
            char digit = '0';
            int mask = 1 << (i - 1);
            if ((x & mask) != 0) digit++;
            if ((y & mask) != 0) digit += 2;
            quadKey.append(digit);
        }
        return quadKey.toString();
    }
    
    @Override
    public int getMaxZoom() {
        return 19;
    }
    
    @Override
    public int getMinZoom() {
        return 1;
    }
    
    @Override
    public float getBrightness() {
        return 1.2f;
    }
    
    public void setMapType(MapType type) {
        this.mapType = type;
    }
    
    public MapType getMapType() {
        return mapType;
    }
}