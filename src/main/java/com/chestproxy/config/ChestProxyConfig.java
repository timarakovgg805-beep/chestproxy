package com.chestproxy.config;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ChestProxyConfig {

    private static final Logger LOG = LogManager.getLogger("ChestProxy");

    private static boolean enabled = true;
    private static int searchRadius = 7;

    public static void load(FMLPreInitializationEvent event) {
        Configuration config = new Configuration(event.getSuggestedConfigurationFile());
        try {
            config.load();
            enabled = config.getBoolean("Enabled", "general", true, "Enable or disable ChestProxy crafting from chests");
            searchRadius = config.getInt("SearchRadius", "general", 7, 1, 32, "Radius in blocks to search for nearby inventories");
        } catch (Exception e) {
            LOG.error("Failed to load config", e);
        } finally {
            config.save();
        }
        LOG.info("ChestProxy config loaded: enabled={}, radius={}", enabled, searchRadius);
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
        LOG.info("ChestProxy toggled {}", value ? "ON" : "OFF");
    }

    public static boolean toggle() {
        enabled = !enabled;
        LOG.info("ChestProxy toggled {}", enabled ? "ON" : "OFF");
        return enabled;
    }

    public static int getSearchRadius() {
        return searchRadius;
    }
}
