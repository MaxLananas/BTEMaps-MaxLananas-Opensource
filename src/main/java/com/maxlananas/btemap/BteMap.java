package com.maxlananas.btemap;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BteMap implements ModInitializer {
    public static final String MOD_ID = "btemap";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("BTEMap initialized! Ready to explore Earth in Minecraft!");
    }
}