package com.maxlananas.btemap.client.map.providers;

import com.maxlananas.btemap.client.map.MapProvider;
import com.maxlananas.btemap.util.WebHelper;

import java.awt.image.BufferedImage;
import java.util.concurrent.CompletableFuture;

public class OpenTopoProvider implements MapProvider {
    
    @Override
    public String getName() {
        return "OpenTopoMap";
    }
    
    @Override
    public String getId() {
        return "opentopo";
    }
    
    @Override
    public String getIcon() {
        return "⛰️";
    }
    
    @Override
    public CompletableFuture<BufferedImage> getTile(int x, int y, int zoom) {
        return WebHelper.downloadImage(getTileUrl(x, y, zoom));
    }
    
    @Override
    public String getTileUrl(int x, int y, int zoom) {
        String subdomain = new String[]{"a", "b", "c"}[(x + y) % 3];
        return String.format(
            "https://%s.tile.opentopomap.org/%d/%d/%d.png",
            subdomain, zoom, x, y
        );
    }
    
    @Override
    public int getMaxZoom() {
        return 17;
    }
    
    @Override
    public int getMinZoom() {
        return 0;
    }
    
    @Override
    public float getBrightness() {
        return 1.05f;
    }
}