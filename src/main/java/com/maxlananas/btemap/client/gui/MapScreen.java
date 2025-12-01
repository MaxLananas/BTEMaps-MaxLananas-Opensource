package com.maxlananas.btemap.client.gui;

import com.maxlananas.btemap.client.BteMapClient;
import com.maxlananas.btemap.client.map.MapProvider;
import com.maxlananas.btemap.client.map.MapState;
import com.maxlananas.btemap.client.map.MapTileManager;
import com.maxlananas.btemap.client.map.providers.BingMapsProvider;
import com.maxlananas.btemap.client.map.providers.CartoDBProvider;
import com.maxlananas.btemap.client.map.providers.EsriProvider;
import com.maxlananas.btemap.client.map.providers.GoogleMapsProvider;
import com.maxlananas.btemap.client.map.providers.OSMProvider;
import com.maxlananas.btemap.client.map.providers.OpenTopoProvider;
import com.maxlananas.btemap.client.map.providers.StamenProvider;
import com.maxlananas.btemap.util.GeocodingHelper;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class MapScreen extends Screen {

    private static final int TILE_SIZE = 256;
    private static final int DISPLAY_TILE_SIZE = 128;

    private final List<MapProvider> providers = new ArrayList<>();
    private int currentProviderIndex = 0;

    private double centerLat;
    private double centerLon;
    private int zoom;

    private boolean isDragging = false;
    private double lastMouseX, lastMouseY;

    private int mapX, mapY, mapWidth, mapHeight;

    private TextFieldWidget searchField;
    private String statusMessage = "";
    private int statusColor = 0xFFFFFF;
    private long statusTime = 0;

    private boolean showContextMenu = false;
    private int contextMenuX, contextMenuY;
    private double contextMenuLat, contextMenuLon;

    private boolean showProviderMenu = false;

    private final Deque<double[]> history = new java.util.ArrayDeque<>();
    private static final int MAX_HISTORY = 50;

    private ButtonWidget providerButton;

    public MapScreen() {
        super(Text.literal("BTE Map"));

        providers.add(new OSMProvider());
        providers.add(new GoogleMapsProvider());
        providers.add(new EsriProvider());
        providers.add(new BingMapsProvider());
        providers.add(new CartoDBProvider());
        providers.add(new StamenProvider());
        providers.add(new OpenTopoProvider());

        MapState state = MapState.get();
        centerLat = state.centerLat;
        centerLon = state.centerLon;
        zoom = state.zoom;

        for (int i = 0; i < providers.size(); i++) {
            if (providers.get(i).getId().equals(state.providerId)) {
                currentProviderIndex = i;
                break;
            }
        }

        if (getCurrentProvider() instanceof GoogleMapsProvider google) {
            try {
                google.setMapType(GoogleMapsProvider.MapType.valueOf(state.googleMapType));
            } catch (Exception ignored) {
            }
        }

        WaypointManager.load();
    }

    private MapProvider getCurrentProvider() {
        return providers.get(currentProviderIndex);
    }

    @Override
    protected void init() {
        super.init();

        mapX = 10;
        mapY = 50;
        mapWidth = this.width - 60;
        mapHeight = this.height - 100;

        searchField = new TextFieldWidget(this.textRenderer, 10, 10, 280, 20, Text.literal("Search"));
        searchField.setPlaceholder(Text.literal("Search address or lat,lon..."));
        searchField.setMaxLength(256);
        this.addDrawableChild(searchField);

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Go"), button -> performSearch())
                .dimensions(295, 10, 30, 20).build());

        int btnX = this.width - 45;
        int btnY = mapY + 10;

        this.addDrawableChild(ButtonWidget.builder(Text.literal("+"), button -> zoomIn())
                .dimensions(btnX, btnY, 35, 30).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("-"), button -> zoomOut())
                .dimensions(btnX, btnY + 35, 35, 30).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("H"), button -> goToPlayerPosition())
                .dimensions(btnX, btnY + 80, 35, 30).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("<"), button -> goBack())
                .dimensions(btnX, btnY + 115, 35, 30).build());

        int bottomY = this.height - 35;

        providerButton = ButtonWidget.builder(
                        Text.literal(getShortProviderName()),
                        button -> showProviderMenu = !showProviderMenu)
                .dimensions(10, bottomY, 140, 20).build();
        this.addDrawableChild(providerButton);

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Style"), button -> cycleMapStyle())
                .dimensions(155, bottomY, 60, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Markers"), button -> {
            this.client.setScreen(new WaypointScreen(this, this::executeTeleport));
        }).dimensions(220, bottomY, 70, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Clear"), button -> {
            BteMapClient.getTileCache().clearDiskCache();
            setStatus("Cache cleared!", 0x00FF00);
        }).dimensions(295, bottomY, 50, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Teleport"), button -> teleportToCenter())
                .dimensions(this.width - 90, bottomY, 80, 20).build());
    }

    private String getShortProviderName() {
        String name = getCurrentProvider().getName();
        return name.length() > 15 ? name.substring(0, 13) + ".." : name;
    }

    private void cycleMapStyle() {
        MapProvider provider = getCurrentProvider();
        BteMapClient.getTileCache().clearCache();

        if (provider instanceof GoogleMapsProvider google) {
            GoogleMapsProvider.MapType[] types = GoogleMapsProvider.MapType.values();
            google.setMapType(types[(google.getMapType().ordinal() + 1) % types.length]);
            setStatus("Google: " + google.getMapType().getDisplayName(), 0x00FF00);
        } else if (provider instanceof EsriProvider esri) {
            EsriProvider.MapType[] types = EsriProvider.MapType.values();
            esri.setMapType(types[(esri.getMapType().ordinal() + 1) % types.length]);
            setStatus("Esri: " + esri.getMapType().getDisplayName(), 0x00FF00);
        } else if (provider instanceof BingMapsProvider bing) {
            BingMapsProvider.MapType[] types = BingMapsProvider.MapType.values();
            bing.setMapType(types[(bing.getMapType().ordinal() + 1) % types.length]);
            setStatus("Bing: " + bing.getMapType().getDisplayName(), 0x00FF00);
        } else if (provider instanceof CartoDBProvider carto) {
            CartoDBProvider.MapType[] types = CartoDBProvider.MapType.values();
            carto.setMapType(types[(carto.getMapType().ordinal() + 1) % types.length]);
            setStatus("Carto: " + carto.getMapType().getDisplayName(), 0x00FF00);
        } else if (provider instanceof StamenProvider stamen) {
            StamenProvider.MapType[] types = StamenProvider.MapType.values();
            stamen.setMapType(types[(stamen.getMapType().ordinal() + 1) % types.length]);
            setStatus("Stamen: " + stamen.getMapType().getDisplayName(), 0x00FF00);
        } else {
            setStatus("No styles available", 0xFFFF00);
        }
    }

    private void setStatus(String message, int color) {
        statusMessage = message;
        statusColor = color;
        statusTime = System.currentTimeMillis();
    }

    private void performSearch() {
        String query = searchField.getText().trim();
        if (query.isEmpty()) return;

        setStatus("Searching...", 0xFFFF00);

        if (query.matches("-?\\d+\\.?\\d*\\s*,\\s*-?\\d+\\.?\\d*")) {
            String[] parts = query.split("\\s*,\\s*");
            try {
                double lat = Double.parseDouble(parts[0]);
                double lon = Double.parseDouble(parts[1]);
                goToLocation(lat, lon);
                setStatus("Found!", 0x00FF00);
                return;
            } catch (NumberFormatException ignored) {
            }
        }

        GeocodingHelper.search(query).thenAccept(result -> {
            if (result != null) {
                goToLocation(result.lat, result.lon);
                setStatus(result.displayName, 0x00FF00);
            } else {
                setStatus("Not found", 0xFF0000);
            }
        });
    }

    public void goToLocation(double lat, double lon) {
        saveToHistory();
        centerLat = lat;
        centerLon = lon;
    }

    private void saveToHistory() {
        history.push(new double[]{centerLat, centerLon, zoom});
        if (history.size() > MAX_HISTORY) history.removeLast();
    }

    private void goBack() {
        if (!history.isEmpty()) {
            double[] prev = history.pop();
            centerLat = prev[0];
            centerLon = prev[1];
            zoom = (int) prev[2];
            setStatus("Back", 0x888888);
        } else {
            setStatus("No history", 0xFF8800);
        }
    }

    private void goToPlayerPosition() {
        saveToHistory();
        centerLat = 48.8584;
        centerLon = 2.2945;
        zoom = 15;
        setStatus("Paris (default)", 0x00FF00);
    }

    private void zoomIn() {
        if (zoom < getCurrentProvider().getMaxZoom()) zoom++;
    }

    private void zoomOut() {
        if (zoom > getCurrentProvider().getMinZoom()) zoom--;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);

        context.fill(mapX, mapY, mapX + mapWidth, mapY + mapHeight, 0xFF1a1a2e);

        renderMapTiles(context);
        renderWaypoints(context);

        context.drawBorder(mapX - 1, mapY - 1, mapWidth + 2, mapHeight + 2, 0xFFFFFFFF);

        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 35, 0xFFFFFF);

        String coords = String.format("Lat: %.6f, Lon: %.6f | Zoom: %d", centerLat, centerLon, zoom);
        context.drawTextWithShadow(this.textRenderer, coords, mapX, mapY + mapHeight + 5, 0xAAAAAA);

        if (!statusMessage.isEmpty() && System.currentTimeMillis() - statusTime < 3000) {
            context.drawTextWithShadow(this.textRenderer, statusMessage, 340, 15, statusColor);
        }

        if (isInMapArea(mouseX, mouseY) && !showProviderMenu && !showContextMenu) {
            double[] mouseCoords = screenToLatLon(mouseX, mouseY);
            String mouseText = String.format("Mouse: %.6f, %.6f", mouseCoords[0], mouseCoords[1]);
            context.drawTextWithShadow(this.textRenderer, mouseText, mapX, mapY + mapHeight + 17, 0x00FF00);
            renderCrosshair(context, mouseX, mouseY);
        }

        if (showContextMenu) renderContextMenu(context, mouseX, mouseY);
        if (showProviderMenu) renderProviderMenu(context, mouseX, mouseY);

        renderScale(context);
        renderMinimap(context);

        super.render(context, mouseX, mouseY, delta);
    }

    private void renderMapTiles(DrawContext context) {
        int centerTileX = MapTileManager.lonToTileX(centerLon, zoom);
        int centerTileY = MapTileManager.latToTileY(centerLat, zoom);

        double exactTileX = lonToTileXExact(centerLon, zoom);
        double exactTileY = latToTileYExact(centerLat, zoom);

        double offsetX = (exactTileX - centerTileX) * DISPLAY_TILE_SIZE;
        double offsetY = (exactTileY - centerTileY) * DISPLAY_TILE_SIZE;

        int tilesX = (mapWidth / DISPLAY_TILE_SIZE) + 3;
        int tilesY = (mapHeight / DISPLAY_TILE_SIZE) + 3;

        int startTileX = centerTileX - tilesX / 2;
        int startTileY = centerTileY - tilesY / 2;

        int mapCenterX = mapX + mapWidth / 2;
        int mapCenterY = mapY + mapHeight / 2;

        context.enableScissor(mapX, mapY, mapX + mapWidth, mapY + mapHeight);

        for (int tx = 0; tx < tilesX; tx++) {
            for (int ty = 0; ty < tilesY; ty++) {
                int tileX = startTileX + tx;
                int tileY = startTileY + ty;

                int maxTile = (1 << zoom) - 1;
                if (tileX < 0 || tileX > maxTile || tileY < 0 || tileY > maxTile) {
                    continue;
                }

                int deltaX = tileX - centerTileX;
                int deltaY = tileY - centerTileY;

                int screenX = (int) (mapCenterX + deltaX * DISPLAY_TILE_SIZE - offsetX);
                int screenY = (int) (mapCenterY + deltaY * DISPLAY_TILE_SIZE - offsetY);

                CompletableFuture<Identifier> tileFuture = BteMapClient.getTileCache()
                        .getTileTexture(getCurrentProvider(), tileX, tileY, zoom);

                if (tileFuture.isDone() && !tileFuture.isCompletedExceptionally()) {
                    Identifier textureId = tileFuture.join();
                    if (textureId != null) {
                        drawTile(context, textureId, screenX, screenY);
                    } else {
                        renderLoadingTile(context, screenX, screenY);
                    }
                } else {
                    renderLoadingTile(context, screenX, screenY);
                }
            }
        }

        context.disableScissor();

        renderCenterCrosshair(context);
    }

    private void drawTile(DrawContext context, Identifier textureId, int x, int y) {
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
                0.0f, 0.0f,                     
                DISPLAY_TILE_SIZE, DISPLAY_TILE_SIZE,  
                TILE_SIZE, TILE_SIZE            
        );
    }

    private void renderLoadingTile(DrawContext context, int x, int y) {
        context.fill(x, y, x + DISPLAY_TILE_SIZE, y + DISPLAY_TILE_SIZE, 0xFF2a2a4a);
        context.drawCenteredTextWithShadow(this.textRenderer, "...", x + DISPLAY_TILE_SIZE / 2, y + DISPLAY_TILE_SIZE / 2 - 4, 0x888888);
    }

    private void renderCenterCrosshair(DrawContext context) {
        int cx = mapX + mapWidth / 2;
        int cy = mapY + mapHeight / 2;

        context.fill(cx - 10, cy - 1, cx + 10, cy + 1, 0xFFFF0000);
        context.fill(cx - 1, cy - 10, cx + 1, cy + 10, 0xFFFF0000);
    }

    private double lonToTileXExact(double lon, int zoom) {
        return (lon + 180.0) / 360.0 * (1 << zoom);
    }

    private double latToTileYExact(double lat, int zoom) {
        double latRad = Math.toRadians(lat);
        return (1.0 - Math.log(Math.tan(latRad) + 1.0 / Math.cos(latRad)) / Math.PI) / 2.0 * (1 << zoom);
    }

    private void renderWaypoints(DrawContext context) {
        List<WaypointManager.Waypoint> waypoints = WaypointManager.getWaypoints();
        if (waypoints.isEmpty()) return;

        context.enableScissor(mapX, mapY, mapX + mapWidth, mapY + mapHeight);

        for (WaypointManager.Waypoint wp : waypoints) {
            int[] pos = latLonToScreen(wp.lat, wp.lon);
            int sx = pos[0];
            int sy = pos[1];

            if (sx >= mapX - 50 && sx < mapX + mapWidth + 50 && sy >= mapY - 50 && sy < mapY + mapHeight + 50) {
                context.fill(sx - 6, sy - 6, sx + 6, sy + 6, wp.color | 0xFF000000);
                context.fill(sx - 4, sy - 4, sx + 4, sy + 4, 0xFFFFFFFF);
                context.fill(sx - 2, sy - 2, sx + 2, sy + 2, wp.color | 0xFF000000);

                int nameWidth = this.textRenderer.getWidth(wp.name);
                context.fill(sx - nameWidth / 2 - 2, sy + 8, sx + nameWidth / 2 + 2, sy + 20, 0xCC000000);
                context.drawCenteredTextWithShadow(this.textRenderer, wp.name, sx, sy + 9, 0xFFFFFF);
            }
        }

        context.disableScissor();
    }

    private int[] latLonToScreen(double lat, double lon) {
        double scale = DISPLAY_TILE_SIZE * (1 << zoom);

        double centerPixelX = (centerLon + 180.0) / 360.0 * scale;
        double centerLatRad = Math.toRadians(centerLat);
        double centerPixelY = (1.0 - Math.log(Math.tan(centerLatRad) + 1.0 / Math.cos(centerLatRad)) / Math.PI) / 2.0 * scale;

        double wpPixelX = (lon + 180.0) / 360.0 * scale;
        double latRad = Math.toRadians(lat);
        double wpPixelY = (1.0 - Math.log(Math.tan(latRad) + 1.0 / Math.cos(latRad)) / Math.PI) / 2.0 * scale;

        int sx = mapX + mapWidth / 2 + (int) (wpPixelX - centerPixelX);
        int sy = mapY + mapHeight / 2 + (int) (wpPixelY - centerPixelY);

        return new int[]{sx, sy};
    }

    private void renderCrosshair(DrawContext context, int mouseX, int mouseY) {
        context.fill(mouseX - 10, mouseY, mouseX - 2, mouseY + 1, 0xAAFFFFFF);
        context.fill(mouseX + 2, mouseY, mouseX + 10, mouseY + 1, 0xAAFFFFFF);
        context.fill(mouseX, mouseY - 10, mouseX + 1, mouseY - 2, 0xAAFFFFFF);
        context.fill(mouseX, mouseY + 2, mouseX + 1, mouseY + 10, 0xAAFFFFFF);
    }

    private void renderScale(DrawContext context) {
        int scaleX = mapX + 10;
        int scaleY = mapY + mapHeight - 20;

        double metersPerPixel = 156543.03392 * Math.cos(Math.toRadians(centerLat)) / Math.pow(2, zoom);
        metersPerPixel *= (double) TILE_SIZE / DISPLAY_TILE_SIZE;

        double[] scaleValues = {1, 2, 5, 10, 20, 50, 100, 200, 500, 1000, 2000, 5000, 10000, 20000, 50000, 100000};
        double targetPixels = 80;
        double scaleMeters = metersPerPixel * targetPixels;

        double bestScale = scaleValues[0];
        for (double sv : scaleValues) {
            if (sv <= scaleMeters * 1.2) {
                bestScale = sv;
            }
        }

        int scalePixels = (int) (bestScale / metersPerPixel);

        String scaleText = bestScale >= 1000
                ? String.format("%.0f km", bestScale / 1000)
                : String.format("%.0f m", bestScale);

        context.fill(scaleX, scaleY, scaleX + scalePixels, scaleY + 3, 0xFFFFFFFF);
        context.fill(scaleX, scaleY - 5, scaleX + 2, scaleY + 3, 0xFFFFFFFF);
        context.fill(scaleX + scalePixels - 2, scaleY - 5, scaleX + scalePixels, scaleY + 3, 0xFFFFFFFF);
        context.drawTextWithShadow(this.textRenderer, scaleText, scaleX + scalePixels / 2 - this.textRenderer.getWidth(scaleText) / 2, scaleY + 5, 0xFFFFFF);
    }

    private void renderMinimap(DrawContext context) {
        int mmX = mapX + mapWidth - 105;
        int mmY = mapY + mapHeight - 70;
        int mmW = 100;
        int mmH = 60;

        context.fill(mmX - 2, mmY - 2, mmX + mmW + 2, mmY + mmH + 2, 0xFF000000);
        context.fill(mmX, mmY, mmX + mmW, mmY + mmH, 0xFF16213e);

        int dotX = mmX + (int) ((centerLon + 180) / 360 * mmW);
        int dotY = mmY + (int) ((90 - centerLat) / 180 * mmH);

        context.fill(dotX - 2, dotY - 2, dotX + 2, dotY + 2, 0xFFFF0000);
    }

    private void renderContextMenu(DrawContext context, int mouseX, int mouseY) {
        int menuWidth = 130;
        int menuHeight = 70;

        int menuX = Math.min(contextMenuX, this.width - menuWidth - 5);
        int menuY = Math.min(contextMenuY, this.height - menuHeight - 5);

        context.fill(menuX, menuY, menuX + menuWidth, menuY + menuHeight, 0xEE222222);
        context.drawBorder(menuX, menuY, menuWidth, menuHeight, 0xFFFFFFFF);

        String coordText = String.format("%.4f, %.4f", contextMenuLat, contextMenuLon);
        context.drawTextWithShadow(this.textRenderer, coordText, menuX + 5, menuY + 5, 0x888888);

        String[] options = {"Teleport", "Add marker", "Copy coords"};
        int optY = menuY + 18;

        for (String option : options) {
            boolean hover = mouseX >= menuX && mouseX < menuX + menuWidth && mouseY >= optY && mouseY < optY + 15;
            if (hover) context.fill(menuX + 2, optY, menuX + menuWidth - 2, optY + 15, 0xFF444444);
            context.drawTextWithShadow(this.textRenderer, option, menuX + 8, optY + 3, 0xFFFFFF);
            optY += 16;
        }
    }

    private void renderProviderMenu(DrawContext context, int mouseX, int mouseY) {
        int menuX = 10;
        int menuY = this.height - 35 - providers.size() * 20 - 5;
        int menuWidth = 160;
        int menuHeight = providers.size() * 20 + 5;

        context.fill(menuX, menuY, menuX + menuWidth, menuY + menuHeight, 0xEE222222);
        context.drawBorder(menuX, menuY, menuWidth, menuHeight, 0xFFFFFFFF);

        int optY = menuY + 5;
        for (int i = 0; i < providers.size(); i++) {
            MapProvider p = providers.get(i);
            boolean hover = mouseX >= menuX && mouseX < menuX + menuWidth && mouseY >= optY && mouseY < optY + 18;
            boolean selected = (i == currentProviderIndex);

            if (selected) {
                context.fill(menuX + 2, optY, menuX + menuWidth - 2, optY + 18, 0xFF3a5a8a);
            } else if (hover) {
                context.fill(menuX + 2, optY, menuX + menuWidth - 2, optY + 18, 0xFF444444);
            }

            context.drawTextWithShadow(this.textRenderer, p.getName(), menuX + 8, optY + 5, 0xFFFFFF);
            optY += 20;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (showProviderMenu) {
            int menuX = 10;
            int menuY = this.height - 35 - providers.size() * 20 - 5;
            int menuWidth = 160;

            if (mouseX >= menuX && mouseX < menuX + menuWidth) {
                int optY = menuY + 5;
                for (int i = 0; i < providers.size(); i++) {
                    if (mouseY >= optY && mouseY < optY + 18) {
                        currentProviderIndex = i;
                        BteMapClient.getTileCache().clearCache();
                        setStatus("Map: " + getCurrentProvider().getName(), 0x00FF00);
                        providerButton.setMessage(Text.literal(getShortProviderName()));
                        showProviderMenu = false;
                        return true;
                    }
                    optY += 20;
                }
            }
            showProviderMenu = false;
            return true;
        }

        if (showContextMenu) {
            int menuX = Math.min(contextMenuX, this.width - 135);
            int menuY = Math.min(contextMenuY, this.height - 75);

            if (mouseX >= menuX && mouseX < menuX + 130) {
                int optY = menuY + 18;

                if (mouseY >= optY && mouseY < optY + 15) {
                    executeTeleport(contextMenuLat, contextMenuLon);
                    showContextMenu = false;
                    return true;
                }
                optY += 16;

                if (mouseY >= optY && mouseY < optY + 15) {
                    String name = "Marker " + (WaypointManager.getWaypoints().size() + 1);
                    WaypointManager.addWaypoint(name, contextMenuLat, contextMenuLon,
                            WaypointManager.getNextColor(), WaypointManager.getNextIcon());
                    setStatus("Marker added!", 0x00FF00);
                    showContextMenu = false;
                    return true;
                }
                optY += 16;

                if (mouseY >= optY && mouseY < optY + 15) {
                    String coordStr = String.format("%.6f, %.6f", contextMenuLat, contextMenuLon);
                    if (this.client != null) {
                        this.client.keyboard.setClipboard(coordStr);
                        setStatus("Copied!", 0x00FF00);
                    }
                    showContextMenu = false;
                    return true;
                }
            }
            showContextMenu = false;
            return true;
        }

        if (button == 0 && isInMapArea(mouseX, mouseY)) {
            isDragging = true;
            lastMouseX = mouseX;
            lastMouseY = mouseY;
            return true;
        }

        if (button == 1 && isInMapArea(mouseX, mouseY)) {
            double[] coords = screenToLatLon(mouseX, mouseY);
            contextMenuLat = coords[0];
            contextMenuLon = coords[1];
            contextMenuX = (int) mouseX;
            contextMenuY = (int) mouseY;
            showContextMenu = true;
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            isDragging = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (isDragging && button == 0) {
            double pixelsPerDegreeLon = (DISPLAY_TILE_SIZE * (1 << zoom)) / 360.0;
            double pixelsPerDegreeLat = pixelsPerDegreeLon * Math.cos(Math.toRadians(centerLat));

            double dx = mouseX - lastMouseX;
            double dy = mouseY - lastMouseY;

            centerLon -= dx / pixelsPerDegreeLon;
            centerLat += dy / pixelsPerDegreeLat;

            centerLat = Math.max(-85, Math.min(85, centerLat));
            if (centerLon > 180) centerLon -= 360;
            if (centerLon < -180) centerLon += 360;

            lastMouseX = mouseX;
            lastMouseY = mouseY;

            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (isInMapArea(mouseX, mouseY)) {
            if (verticalAmount > 0) zoomIn();
            else if (verticalAmount < 0) zoomOut();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (searchField.isFocused() && (keyCode == 257 || keyCode == 335)) {
            performSearch();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private boolean isInMapArea(double x, double y) {
        return x >= mapX && x < mapX + mapWidth && y >= mapY && y < mapY + mapHeight;
    }

    private double[] screenToLatLon(double screenX, double screenY) {
        double pixelsPerDegreeLon = (DISPLAY_TILE_SIZE * (1 << zoom)) / 360.0;
        double pixelsPerDegreeLat = pixelsPerDegreeLon * Math.cos(Math.toRadians(centerLat));

        double dx = screenX - (mapX + mapWidth / 2.0);
        double dy = screenY - (mapY + mapHeight / 2.0);

        double lon = centerLon + dx / pixelsPerDegreeLon;
        double lat = centerLat - dy / pixelsPerDegreeLat;

        return new double[]{lat, lon};
    }

    private void teleportToCenter() {
        executeTeleport(centerLat, centerLon);
    }

    private void executeTeleport(double lat, double lon) {
        if (this.client != null && this.client.player != null) {
            String command = String.format("tpll %.6f %.6f", lat, lon);
            this.client.player.networkHandler.sendChatCommand(command);
            this.close();
        }
    }

    @Override
    public void close() {
        String googleType = "SATELLITE";
        if (getCurrentProvider() instanceof GoogleMapsProvider google) {
            googleType = google.getMapType().name();
        }
        MapState.get().update(centerLat, centerLon, zoom, getCurrentProvider().getId(), googleType);
        super.close();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}