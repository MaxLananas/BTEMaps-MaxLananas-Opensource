package com.maxlananas.btemap.client.map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.maxlananas.btemap.BteMap;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class MapState {
    
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path STATE_FILE = Path.of("btemap_state.json");
    
    private static MapState instance;
    
    // État sauvegardé
    public double centerLat = 48.8584;
    public double centerLon = 2.2945;
    public int zoom = 15;
    public String providerId = "osm";
    public String googleMapType = "SATELLITE";
    
    public static MapState get() {
        if (instance == null) {
            load();
        }
        return instance;
    }
    
    public static void load() {
        try {
            if (Files.exists(STATE_FILE)) {
                String json = Files.readString(STATE_FILE);
                instance = GSON.fromJson(json, MapState.class);
                BteMap.LOGGER.info("Map state loaded: {}, {} zoom {}", 
                    instance.centerLat, instance.centerLon, instance.zoom);
            } else {
                instance = new MapState();
            }
        } catch (Exception e) {
            BteMap.LOGGER.error("Failed to load map state", e);
            instance = new MapState();
        }
    }
    
    public static void save() {
        try {
            Files.writeString(STATE_FILE, GSON.toJson(instance));
        } catch (IOException e) {
            BteMap.LOGGER.error("Failed to save map state", e);
        }
    }
    
    public void update(double lat, double lon, int zoom, String provider, String googleType) {
        this.centerLat = lat;
        this.centerLon = lon;
        this.zoom = zoom;
        this.providerId = provider;
        this.googleMapType = googleType;
        save();
    }
}