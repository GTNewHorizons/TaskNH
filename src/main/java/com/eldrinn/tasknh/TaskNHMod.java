package com.eldrinn.tasknh;

import com.eldrinn.tasknh.proxy.CommonProxy;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

@Mod(
    modid = TaskNHMod.MODID,
    version = Tags.VERSION,
    name = "TaskNH",
    acceptedMinecraftVersions = "[1.7.10]",
    // FML checks the version of an after: mod only when it is installed, so NEI stays optional.
    dependencies = "required-after:gtnhlib@[0.11.51,);required-after:modularui2@[2.3.91,);"
        + "after:navigator;after:betterquesting;after:NotEnoughItems@[2.8.150,)")
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
