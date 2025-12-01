package com.maxlananas.btemap.util;

import com.maxlananas.btemap.BteMap;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class WebHelper {
    
    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    
    public static CompletableFuture<BufferedImage> downloadImage(String url) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("User-Agent", "BTEMap Minecraft Mod/1.0")
                        .timeout(Duration.ofSeconds(15))
                        .GET()
                        .build();
                
                HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
                
                if (response.statusCode() == 200) {
                    return ImageIO.read(new java.io.ByteArrayInputStream(response.body()));
                } else {
                    BteMap.LOGGER.warn("Failed to download tile: HTTP {}", response.statusCode());
                }
            } catch (Exception e) {
                BteMap.LOGGER.error("Error downloading image from: " + url, e);
            }
            return null;
        });
    }
}