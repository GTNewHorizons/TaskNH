package com.eldrinn.tasknh.proxy;

import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.common.MinecraftForge;

import org.lwjgl.input.Keyboard;

import com.eldrinn.tasknh.cache.TaskNHClientCache;
import com.eldrinn.tasknh.gui.TaskNHGui;
import com.eldrinn.tasknh.hud.HudRenderer;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ClientProxy extends CommonProxy {

    public static final KeyBinding KEY_OPEN_GUI = new KeyBinding(
        "key.tasknh.open",
        Keyboard.KEY_Y,
        "key.categories.tasknh");

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
    }

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        ClientRegistry.registerKeyBinding(KEY_OPEN_GUI);
        FMLCommonHandler.instance()
            .bus()
            .register(this);
        MinecraftForge.EVENT_BUS.register(new HudRenderer());
        TaskNHClientCache.loadConfig();
        if (cpw.mods.fml.common.Loader.isModLoaded("navigator")) {
            com.gtnewhorizons.navigator.api.NavigatorApi
                .registerLayerManager(com.eldrinn.tasknh.navigator.TaskLayerManager.INSTANCE);
        }
        if (cpw.mods.fml.common.Loader.isModLoaded("betterquesting")) {
            com.eldrinn.tasknh.integration.BetterQuestingIntegration.register();
        }
    }

    @Override
    public void serverStarting(FMLServerStartingEvent event) {
        super.serverStarting(event);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (KEY_OPEN_GUI.isPressed()) {
            TaskNHGui.open();
        }
        TaskNHGui.tick();
    }
}
