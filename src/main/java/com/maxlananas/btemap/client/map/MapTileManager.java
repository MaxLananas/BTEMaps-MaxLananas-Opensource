package com.maxlananas.btemap.client.map;

import com.maxlananas.btemap.client.BteMapClient;
import com.maxlananas.btemap.client.map.providers.GoogleMapsProvider;
import com.maxlananas.btemap.client.map.providers.OSMProvider;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class MapTileManager {
    
    private static final Map<String, MapProvider> providers = new HashMap<>();
    private MapProvider currentProvider;
    
    static {
        providers.put("osm", new OSMProvider());
        providers.put("google", new GoogleMapsProvider());
    }
    
    public MapTileManager() {
        this.currentProvider = providers.get("osm");
    }
    
    public void setProvider(String providerId) {
        MapProvider provider = providers.get(providerId);
        if (provider != null) {
            this.currentProvider = provider;
        }
    }
    
    public MapProvider getCurrentProvider() {
        return currentProvider;
    }
    
    public Map<String, MapProvider> getProviders() {
        return providers;
    }
    
    // Convertir lat/lon en tuile
    public static int lonToTileX(double lon, int zoom) {
        return (int) Math.floor((lon + 180.0) / 360.0 * (1 << zoom));
    }
    
    public static int latToTileY(double lat, int zoom) {
        double latRad = Math.toRadians(lat);
        return (int) Math.floor((1.0 - Math.log(Math.tan(latRad) + 1.0 / Math.cos(latRad)) / Math.PI) / 2.0 * (1 << zoom));
    }
    
    // Convertir tuile en lat/lon (coin nord-ouest)
    public static double tileXToLon(int x, int zoom) {
        return x / (double) (1 << zoom) * 360.0 - 180.0;
    }
    
    public static double tileYToLat(int y, int zoom) {
        double n = Math.PI - 2.0 * Math.PI * y / (1 << zoom);
        return Math.toDegrees(Math.atan(Math.sinh(n)));
    }
    
    // Convertir position pixel en lat/lon
    public static double[] pixelToLatLon(int tileX, int tileY, int pixelX, int pixelY, int zoom) {
        double lon = tileXToLon(tileX, zoom) + (pixelX / 256.0) * (360.0 / (1 << zoom));
        
        double lat1 = tileYToLat(tileY, zoom);
        double lat2 = tileYToLat(tileY + 1, zoom);
        double lat = lat1 + (pixelY / 256.0) * (lat2 - lat1);
        
        return new double[]{lat, lon};
    }
}