package com.eldrinn.tasknh;

import com.eldrinn.tasknh.proxy.CommonProxy;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

@Mod(modid = TaskNHMod.MODID, version = Tags.VERSION, name = "TaskNH", acceptedMinecraftVersions = "[1.7.10]")
public class TaskNHMod {

    public static final String MODID = "tasknh";

    @SuppressWarnings("unused") // assigned by FML via @SidedProxy reflection
    @SidedProxy(
        clientSide = "com.eldrinn.tasknh.proxy.ClientProxy",
        serverSide = "com.eldrinn.tasknh.proxy.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        proxy.serverStarting(event);
    }
}
