package com.chestproxy.config;

import com.chestproxy.ChestProxyMod;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Config(modid = ChestProxyMod.MOD_ID, name = "chestproxy")
@Config.LangKey("chestproxy.config.title")
public class ChestProxyConfig {

    @Config.Name("Enabled")
    @Config.Comment("Enable or disable ChestProxy crafting from chests")
    @Config.LangKey("chestproxy.config.enabled")
    public static boolean enabled = true;

    @Config.Name("SearchRadius")
    @Config.Comment("Radius in blocks to search for nearby inventories")
    @Config.LangKey("chestproxy.config.radius")
    @Config.RangeInt(min = 1, max = 32)
    public static int searchRadius = 7;

    @Config.Name("LoggingEnabled")
    @Config.Comment("Enable debug logging")
    @Config.LangKey("chestproxy.config.logging")
    public static boolean loggingEnabled = false;

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    public static boolean toggle() {
        enabled = !enabled;
        return enabled;
    }

    public static int getSearchRadius() {
        return searchRadius;
    }

    public static boolean isLoggingEnabled() {
        return loggingEnabled;
    }

    @Mod.EventBusSubscriber(modid = ChestProxyMod.MOD_ID)
    private static class EventHandler {
        @SubscribeEvent
        public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
            if (event.getModID().equals(ChestProxyMod.MOD_ID)) {
                ConfigManager.sync(ChestProxyMod.MOD_ID, Config.Type.INSTANCE);
            }
        }
    }
}
