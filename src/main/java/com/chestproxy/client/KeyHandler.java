package com.chestproxy.client;

import com.chestproxy.config.ChestProxyConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.lwjgl.input.Keyboard;

public class KeyHandler {

    public static final KeyBinding TOGGLE_KEY = new KeyBinding(
        "key.chestproxy.toggle",
        Keyboard.KEY_NONE,
        "key.categories.chestproxy"
    );

    private static boolean wasDown = false;

    public static void register() {
        ClientRegistry.registerKeyBinding(TOGGLE_KEY);
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        boolean down = TOGGLE_KEY.isKeyDown();
        if (down && !wasDown) {
            boolean enabled = ChestProxyConfig.toggle();
            if (Minecraft.getMinecraft().player != null) {
                String status = (enabled ? TextFormatting.GREEN : TextFormatting.RED) + (enabled ? "ON" : "OFF");
                Minecraft.getMinecraft().player.sendMessage(
                    new TextComponentString(TextFormatting.GOLD + "[ChestProxy] " + TextFormatting.RESET + "Toggled " + status)
                );
            }
        }
        wasDown = down;
    }
}
