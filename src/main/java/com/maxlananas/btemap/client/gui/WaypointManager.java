package com.maxlananas.btemap.client.gui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.maxlananas.btemap.BteMap;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class WaypointManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path WAYPOINTS_FILE = Path.of("btemap_waypoints.json");

    private static List<Waypoint> waypoints = new ArrayList<>();

    public static final int[] COLORS = {
            0xFF0000, 0x00FF00, 0x0000FF, 0xFFFF00, 0xFF00FF,
            0x00FFFF, 0xFFA500, 0x800080, 0xFFFFFF, 0x00FF7F
    };

    public static final String[] ICONS = {
            "P", "H", "S", "X", "T", "C", "M", "F", "W", "B"
    };

    public static class Waypoint {
        public String name;
        public double lat;
        public double lon;
        public int color;
        public String icon;

        public Waypoint(String name, double lat, double lon, int color, String icon) {
            this.name = name;
            this.lat = lat;
            this.lon = lon;
            this.color = color;
            this.icon = icon;
        }
    }

    public static void load() {
        try {
            if (Files.exists(WAYPOINTS_FILE)) {
                String json = Files.readString(WAYPOINTS_FILE);
                waypoints = GSON.fromJson(json, new TypeToken<List<Waypoint>>() {
                }.getType());
                if (waypoints == null) waypoints = new ArrayList<>();
                BteMap.LOGGER.info("Loaded {} waypoints", waypoints.size());
            }
        } catch (Exception e) {
            BteMap.LOGGER.error("Failed to load waypoints", e);
            waypoints = new ArrayList<>();
        }
    }

    public static void save() {
        try {
            Files.writeString(WAYPOINTS_FILE, GSON.toJson(waypoints));
        } catch (IOException e) {
            BteMap.LOGGER.error("Failed to save waypoints", e);
        }
    }

    public static Waypoint addWaypoint(String name, double lat, double lon, int color, String icon) {
        Waypoint wp = new Waypoint(name, lat, lon, color, icon);
        waypoints.add(wp);
        save();
        return wp;
    }

    public static void removeWaypoint(int index) {
        if (index >= 0 && index < waypoints.size()) {
            waypoints.remove(index);
            save();
        }
    }

    public static void updateWaypoint(int index, String name, int color, String icon) {
        if (index >= 0 && index < waypoints.size()) {
            Waypoint wp = waypoints.get(index);
            wp.name = name;
            wp.color = color;
            wp.icon = icon;
            save();
        }
    }

    public static List<Waypoint> getWaypoints() {
        return waypoints;
    }

    public static Waypoint getWaypoint(int index) {
        if (index >= 0 && index < waypoints.size()) {
            return waypoints.get(index);
        }
        return null;
    }

    public static void clearAll() {
        waypoints.clear();
        save();
    }

    public static int getNextColor() {
        return COLORS[waypoints.size() % COLORS.length];
    }

    public static String getNextIcon() {
        return ICONS[waypoints.size() % ICONS.length];
    }
}