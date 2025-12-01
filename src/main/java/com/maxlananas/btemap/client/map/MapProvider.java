package com.maxlananas.btemap.client.map;

import java.awt.image.BufferedImage;
import java.util.concurrent.CompletableFuture;

public interface MapProvider {
    String getName();
    String getId();
    String getIcon();
    CompletableFuture<BufferedImage> getTile(int x, int y, int zoom);
    String getTileUrl(int x, int y, int zoom);
    int getMaxZoom();
    int getMinZoom();
    float getBrightness();
}