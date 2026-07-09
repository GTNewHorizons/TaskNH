package com.eldrinn.tasknh.proxy;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraftforge.common.MinecraftForge;

import com.eldrinn.tasknh.command.TaskNHCommand;
import com.eldrinn.tasknh.command.TaskNHPermissions;
import com.eldrinn.tasknh.event.PlayerLoginHandler;
import com.eldrinn.tasknh.event.PlayerLogoutHandler;
import com.eldrinn.tasknh.event.TeamMergeListener;
import com.eldrinn.tasknh.network.TaskNHNetwork;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

public class CommonProxy {

    /** In-memory cooldown map for remind actions. Key: "taskId:targetPlayerId". Resets on server restart. */
    public static final Map<String, Long> remindCooldowns = new LinkedHashMap<>();

    public void preInit(FMLPreInitializationEvent event) {
        TaskNHNetwork.init();
    }

    public void init(FMLInitializationEvent event) {
        // PlayerLoggedInEvent fires on FML's bus, not MinecraftForge.EVENT_BUS
        FMLCommonHandler.instance()
            .bus()
            .register(new PlayerLoginHandler());
        FMLCommonHandler.instance()
            .bus()
            .register(new PlayerLogoutHandler());
        MinecraftForge.EVENT_BUS.register(new TeamMergeListener());
        TaskNHPermissions.registerNodes();
    }

    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new TaskNHCommand());
    }
}
