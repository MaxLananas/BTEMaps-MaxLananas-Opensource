package com.maxlananas.btemap.client;

import com.maxlananas.btemap.BteMap;
import com.maxlananas.btemap.client.keybind.KeyBindings;
import com.maxlananas.btemap.client.map.TileCache;
import net.fabricmc.api.ClientModInitializer;

public class BteMapClient implements ClientModInitializer {
    
    private static TileCache tileCache;
    
    @Override
    public void onInitializeClient() {
        BteMap.LOGGER.info("BTEMap Client initialized!");
        
        tileCache = new TileCache();
        
        KeyBindings.register();
    }
    
    public static TileCache getTileCache() {
        return tileCache;
    }
}