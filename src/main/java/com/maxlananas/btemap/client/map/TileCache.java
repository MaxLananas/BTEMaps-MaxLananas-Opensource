package com.maxlananas.btemap.client.map;

import com.maxlananas.btemap.BteMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import org.lwjgl.opengl.GL11;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;

public class TileCache {

    private final Map<String, Identifier> textureCache = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<Identifier>> loadingTiles = new ConcurrentHashMap<>();
    private final Path cacheDir;

    public TileCache() {
        this.cacheDir = Path.of("btemap_cache");
        try {
            Files.createDirectories(cacheDir);
        } catch (IOException e) {
            BteMap.LOGGER.error("Failed to create cache directory", e);
        }
    }

    public CompletableFuture<Identifier> getTileTexture(MapProvider provider, int x, int y, int zoom) {
        String key = String.format("%s_%d_%d_%d", provider.getId(), zoom, x, y);

        if (textureCache.containsKey(key)) {
            return CompletableFuture.completedFuture(textureCache.get(key));
        }

        if (loadingTiles.containsKey(key)) {
            return loadingTiles.get(key);
        }

        CompletableFuture<Identifier> future = CompletableFuture.supplyAsync(() -> {
            try {
                Path providerDir = cacheDir.resolve(provider.getId());
                Files.createDirectories(providerDir);
                Path tilePath = providerDir.resolve(zoom + "_" + x + "_" + y + ".png");

                byte[] pngBytes;

                if (Files.exists(tilePath)) {
                    pngBytes = Files.readAllBytes(tilePath);
                } else {
                    BufferedImage image = provider.getTile(x, y, zoom).join();
                    if (image == null) return null;

                    image = adjustBrightness(image, provider.getBrightness());

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ImageIO.write(image, "PNG", baos);
                    pngBytes = baos.toByteArray();

                    Files.write(tilePath, pngBytes);
                }

                return createTexture(key, pngBytes);

            } catch (Exception e) {
                BteMap.LOGGER.error("Failed to load tile: " + key, e);
            }
            return null;
        });

        loadingTiles.put(key, future);
        future.thenAccept(id -> {
            if (id != null) {
                textureCache.put(key, id);
            }
            loadingTiles.remove(key);
        });

        return future;
    }

    private BufferedImage adjustBrightness(BufferedImage image, float brightness) {
        if (Math.abs(brightness - 1.0f) < 0.01f) return image;

        int w = image.getWidth();
        int h = image.getHeight();
        BufferedImage result = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        for (int px = 0; px < w; px++) {
            for (int py = 0; py < h; py++) {
                int rgb = image.getRGB(px, py);
                int a = (rgb >> 24) & 0xFF;
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                r = Math.min(255, Math.max(0, (int) (r * brightness)));
                g = Math.min(255, Math.max(0, (int) (g * brightness)));
                b = Math.min(255, Math.max(0, (int) (b * brightness)));

                result.setRGB(px, py, (a << 24) | (r << 16) | (g << 8) | b);
            }
        }

        return result;
    }

    private Identifier createTexture(String key, byte[] pngBytes) {
        String safeKey = key.toLowerCase().replaceAll("[^a-z0-9_]", "_");
        Identifier id = Identifier.of(BteMap.MOD_ID, "tile/" + safeKey);

        MinecraftClient client = MinecraftClient.getInstance();
        CompletableFuture<Identifier> resultFuture = new CompletableFuture<>();

        client.execute(() -> {
            try {
                ByteArrayInputStream bais = new ByteArrayInputStream(pngBytes);
                NativeImage nativeImage = NativeImage.read(bais);

                NativeImageBackedTexture texture = new NativeImageBackedTexture(nativeImage);
                client.getTextureManager().registerTexture(id, texture);

                // Forcer NEAREST immédiatement après création
                int glId = texture.getGlId();
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, glId);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);

                resultFuture.complete(id);
            } catch (Exception e) {
                BteMap.LOGGER.error("Failed to create texture", e);
                resultFuture.complete(null);
            }
        });

        try {
            return resultFuture.get();
        } catch (Exception e) {
            return null;
        }
    }

    public void clearCache() {
        textureCache.clear();
    }

    public void clearDiskCache() {
        try {
            if (Files.exists(cacheDir)) {
                Files.walk(cacheDir)
                        .sorted((a, b) -> -a.compareTo(b))
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException ignored) {
                            }
                        });
                Files.createDirectories(cacheDir);
            }
        } catch (IOException e) {
            BteMap.LOGGER.error("Failed to clear disk cache", e);
        }
        textureCache.clear();
    }
}