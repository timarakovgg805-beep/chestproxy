package com.chestproxy;

import com.chestproxy.client.KeyHandler;
import com.chestproxy.config.ChestProxyConfig;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(
    modid = ChestProxyMod.MOD_ID,
    name = ChestProxyMod.MOD_NAME,
    version = ChestProxyMod.VERSION,
    acceptedMinecraftVersions = "[1.12.2,1.13)",
    dependencies = "required-after:jei@[4.16.0,)",
    clientSideOnly = true
)
public class ChestProxyMod {

    public static final String MOD_ID = "chestproxy";
    public static final String MOD_NAME = "ChestProxy";
    public static final String VERSION = Tags.VERSION;
    public static final Logger LOG = LogManager.getLogger(MOD_ID);

    @Mod.Instance(MOD_ID)
    public static ChestProxyMod instance;

    @Mod.EventHandler
    @SideOnly(Side.CLIENT)
    public void preInit(FMLPreInitializationEvent event) {
        ChestProxyConfig.load(event);
        KeyHandler.register();
    }

    @Mod.EventHandler
    @SideOnly(Side.CLIENT)
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(new KeyHandler());
    }
}
