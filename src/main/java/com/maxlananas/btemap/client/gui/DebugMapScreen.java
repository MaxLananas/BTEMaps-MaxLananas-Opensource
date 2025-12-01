package com.maxlananas.btemap.client.gui;

import com.maxlananas.btemap.client.BteMapClient;
import com.maxlananas.btemap.client.map.MapProvider;
import com.maxlananas.btemap.client.map.MapTileManager;
import com.maxlananas.btemap.client.map.providers.OSMProvider;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.opengl.GL11;

import java.util.concurrent.CompletableFuture;

public class DebugMapScreen extends Screen {

    private MapProvider provider = new OSMProvider();
    private int testMode = 0;
    private String debugInfo = "";
    
    // Paris coordinates
    private double lat = 48.8584;
    private double lon = 2.2945;
    private int zoom = 18; // ZOOM ÉLEVÉ pour avoir des détails

    public DebugMapScreen() {
        super(Text.literal("Debug Map"));
    }

    @Override
    protected void init() {
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Mode 0: Standard"), button -> {
            testMode = 0;
        }).dimensions(10, 10, 150, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Mode 1: NEAREST"), button -> {
            testMode = 1;
        }).dimensions(10, 35, 150, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Zoom -"), button -> {
            if (zoom > 1) zoom--;
        }).dimensions(10, 70, 70, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Zoom +"), button -> {
            if (zoom < 19) zoom++;
        }).dimensions(85, 70, 70, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Clear Cache"), button -> {
            BteMapClient.getTileCache().clearDiskCache();
        }).dimensions(10, 100, 150, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);

        int tileX = MapTileManager.lonToTileX(lon, zoom);
        int tileY = MapTileManager.latToTileY(lat, zoom);

        CompletableFuture<Identifier> tileFuture = BteMapClient.getTileCache()
                .getTileTexture(provider, tileX, tileY, zoom);

        int drawX = 200;
        int drawY = 30;
        int drawSize = 256;

        // Fond
        context.fill(drawX - 2, drawY - 2, drawX + drawSize + 2, drawY + drawSize + 2, 0xFF333333);

        // Info zoom
        context.drawTextWithShadow(this.textRenderer, "Zoom: " + zoom, 10, 130, 0xFFFF00);
        context.drawTextWithShadow(this.textRenderer, "Tile: " + tileX + ", " + tileY, 10, 145, 0xFFFF00);
        context.drawTextWithShadow(this.textRenderer, "Mode: " + testMode, 10, 160, 0x00FF00);

        if (tileFuture.isDone() && !tileFuture.isCompletedExceptionally()) {
            Identifier textureId = tileFuture.join();
            if (textureId != null) {
                debugInfo = "Texture: OK";
                
                AbstractTexture texture = this.client.getTextureManager().getTexture(textureId);
                if (texture != null) {
                    int glId = texture.getGlId();
                    debugInfo += "\nGL ID: " + glId;
                    
                    // Vérifier les paramètres actuels
                    GL11.glBindTexture(GL11.GL_TEXTURE_2D, glId);
                    int minFilter = GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER);
                    int magFilter = GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER);
                    debugInfo += "\nMIN: " + getFilterName(minFilter);
                    debugInfo += "\nMAG: " + getFilterName(magFilter);
                }

                switch (testMode) {
                    case 0 -> renderMode0(context, textureId, drawX, drawY, drawSize);
                    case 1 -> renderMode1(context, textureId, drawX, drawY, drawSize);
                }
            } else {
                debugInfo = "Texture: NULL";
            }
        } else {
            debugInfo = "Loading...";
            context.drawCenteredTextWithShadow(this.textRenderer, "Loading...", drawX + drawSize / 2, drawY + drawSize / 2, 0xFFFFFF);
        }

        // Afficher debug info
        String[] lines = debugInfo.split("\n");
        int y = 180;
        for (String line : lines) {
            context.drawTextWithShadow(this.textRenderer, line, 10, y, 0xFFFFFF);
            y += 12;
        }

        // Dessiner une tuile plus petite pour comparer (128x128)
        context.drawTextWithShadow(this.textRenderer, "Small (128x128):", drawX, drawY + drawSize + 10, 0xAAAAAA);
        
        if (tileFuture.isDone() && !tileFuture.isCompletedExceptionally()) {
            Identifier textureId = tileFuture.join();
            if (textureId != null) {
                if (testMode == 1) {
                    AbstractTexture texture = this.client.getTextureManager().getTexture(textureId);
                    if (texture != null) {
                        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture.getGlId());
                        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
                        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
                    }
                }
                
                context.drawTexture(
                        RenderLayer::getGuiTextured,
                        textureId,
                        drawX, drawY + drawSize + 25,
                        0, 0,
                        128, 128,
                        256, 256
                );
            }
        }

        super.render(context, mouseX, mouseY, delta);
    }

    // Mode 0: Standard DrawContext
    private void renderMode0(DrawContext context, Identifier textureId, int x, int y, int size) {
        context.drawTexture(
                RenderLayer::getGuiTextured,
                textureId,
                x, y,
                0, 0,
                size, size,
                size, size
        );
    }

    // Mode 1: Forcer NEAREST avant le rendu
    private void renderMode1(DrawContext context, Identifier textureId, int x, int y, int size) {
        AbstractTexture texture = this.client.getTextureManager().getTexture(textureId);
        if (texture != null) {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture.getGlId());
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        }

        context.drawTexture(
                RenderLayer::getGuiTextured,
                textureId,
                x, y,
                0, 0,
                size, size,
                size, size
        );
    }

    private String getFilterName(int filter) {
        return switch (filter) {
            case GL11.GL_NEAREST -> "NEAREST";
            case GL11.GL_LINEAR -> "LINEAR";
            case 9984 -> "NEAREST_MIPMAP_NEAREST";
            case 9985 -> "LINEAR_MIPMAP_NEAREST";
            case 9986 -> "NEAREST_MIPMAP_LINEAR";
            case 9987 -> "LINEAR_MIPMAP_LINEAR";
            default -> "UNKNOWN(" + filter + ")";
        };
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}