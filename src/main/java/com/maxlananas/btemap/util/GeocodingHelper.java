package com.maxlananas.btemap.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.maxlananas.btemap.BteMap;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class GeocodingHelper {
    
    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    
    public static class GeocodingResult {
        public final double lat;
        public final double lon;
        public final String displayName;
        
        public GeocodingResult(double lat, double lon, String displayName) {
            this.lat = lat;
            this.lon = lon;
            this.displayName = displayName;
        }
    }
    
    public static CompletableFuture<GeocodingResult> search(String query) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
                String url = "https://nominatim.openstreetmap.org/search?q=" + encodedQuery 
                           + "&format=json&limit=1";
                
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("User-Agent", "BTEMap Minecraft Mod/1.0")
                        .timeout(Duration.ofSeconds(10))
                        .GET()
                        .build();
                
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() == 200) {
                    JsonArray results = JsonParser.parseString(response.body()).getAsJsonArray();
                    
                    if (results.size() > 0) {
                        JsonObject first = results.get(0).getAsJsonObject();
                        double lat = first.get("lat").getAsDouble();
                        double lon = first.get("lon").getAsDouble();
                        String displayName = first.get("display_name").getAsString();
                        
                        // Raccourcir le nom
                        if (displayName.length() > 50) {
                            displayName = displayName.substring(0, 47) + "...";
                        }
                        
                        return new GeocodingResult(lat, lon, displayName);
                    }
                }
            } catch (Exception e) {
                BteMap.LOGGER.error("Geocoding error", e);
            }
            return null;
        });
    }
    
    public static CompletableFuture<String> reverseGeocode(double lat, double lon) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = String.format(
                    "https://nominatim.openstreetmap.org/reverse?lat=%.6f&lon=%.6f&format=json",
                    lat, lon
                );
                
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("User-Agent", "BTEMap Minecraft Mod/1.0")
                        .timeout(Duration.ofSeconds(10))
                        .GET()
                        .build();
                
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() == 200) {
                    JsonObject result = JsonParser.parseString(response.body()).getAsJsonObject();
                    if (result.has("display_name")) {
                        return result.get("display_name").getAsString();
                    }
                }
            } catch (Exception e) {
                BteMap.LOGGER.error("Reverse geocoding error", e);
            }
            return null;
        });
    }
}