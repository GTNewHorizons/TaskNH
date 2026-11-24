package com.eldrinn.tasknh.proxy;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraftforge.common.MinecraftForge;

import com.eldrinn.tasknh.command.TaskNHCommand;
import com.eldrinn.tasknh.command.TaskNHPermissions;
import com.eldrinn.tasknh.config.TaskNHConfig;
import com.eldrinn.tasknh.event.ItemTrackHandler;
import com.eldrinn.tasknh.event.PlayerLoginHandler;
import com.eldrinn.tasknh.event.TeamMembersListener;
import com.eldrinn.tasknh.event.TeamMergeListener;
import com.eldrinn.tasknh.network.TaskNHNetwork;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.event.FMLServerStoppedEvent;

public class CommonProxy {

    /** In-memory cooldown map for remind actions. Key: "taskId:targetPlayerId". Resets on server restart. */
    public final Map<String, Long> remindCooldowns = new LinkedHashMap<>();
    private final ItemTrackHandler itemTrackHandler = new ItemTrackHandler();

    public void preInit(FMLPreInitializationEvent event) {
        TaskNHConfig.load(event);
        TaskNHNetwork.init();
    }

    public void init(FMLInitializationEvent event) {
        // PlayerLoggedInEvent fires on FML's bus, not MinecraftForge.EVENT_BUS
        FMLCommonHandler.instance()
            .bus()
            .register(new PlayerLoginHandler());
        // ItemTrackHandler listens on both buses: EntityJoinWorldEvent (Forge) and ServerTickEvent (FML)
        FMLCommonHandler.instance()
            .bus()
            .register(itemTrackHandler);
        MinecraftForge.EVENT_BUS.register(itemTrackHandler);
        MinecraftForge.EVENT_BUS.register(new TeamMergeListener());
        MinecraftForge.EVENT_BUS.register(new TeamMembersListener());
        TaskNHPermissions.registerNodes();
    }

    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new TaskNHCommand());
    }

    public void serverStopped(FMLServerStoppedEvent event) {
        itemTrackHandler.reset();
        remindCooldowns.clear();
    }
}
