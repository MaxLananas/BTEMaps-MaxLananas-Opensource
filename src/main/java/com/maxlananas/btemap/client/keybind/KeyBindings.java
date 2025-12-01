package com.maxlananas.btemap.client.keybind;

import com.maxlananas.btemap.client.gui.MapScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class KeyBindings {

    public static KeyBinding openMapKey;

    public static void register() {
        openMapKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.btemap.open_map",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_M,
                "category.btemap.main"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openMapKey.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(new MapScreen());
                }
            }
        });
    }
}