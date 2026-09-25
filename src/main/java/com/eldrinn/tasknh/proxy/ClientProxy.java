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
import cpw.mods.fml.common.network.FMLNetworkEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ClientProxy extends CommonProxy {

    public static final KeyBinding KEY_OPEN_GUI = new KeyBinding(
        "key.tasknh.open",
        Keyboard.KEY_Y,
        "key.categories.tasknh");

    /** Set on the network thread when the player leaves a server, handled on the next client tick. */
    private volatile boolean disconnected = false;

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
        MinecraftForge.EVENT_BUS.register(this); // theme reloads are posted on the Forge bus
        TaskNHClientCache.loadConfig();
        if (cpw.mods.fml.common.Loader.isModLoaded("navigator")) {
            com.gtnewhorizons.navigator.api.NavigatorApi
                .registerLayerManager(com.eldrinn.tasknh.navigator.TaskLayerManager.INSTANCE);
        }
        if (com.eldrinn.tasknh.integration.BQIntegration.isAvailable()) {
            com.eldrinn.tasknh.integration.BetterQuestingIntegration.register();
        }
        // Checked here rather than in the integration class, so that class never loads without the mods it uses.
        if (com.cleanroommc.modularui.ModularUI.Mods.NEI.isLoaded()
            && cpw.mods.fml.common.Loader.isModLoaded("blockrenderer6343")) {
            com.eldrinn.tasknh.integration.MultiblockTaskIntegration.register();
        }
    }

    @Override
    public void serverStarting(FMLServerStartingEvent event) {
        super.serverStarting(event);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (disconnected) {
            disconnected = false;
            TaskNHClientCache.clear();
        }
        if (KEY_OPEN_GUI.isPressed()) {
            TaskNHGui.open();
        }
        TaskNHGui.tick();
    }

    // The cache is only safe on the client thread, so the event just leaves a flag for the tick above.
    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        disconnected = true;
    }

    @SubscribeEvent
    public void onThemeReload(com.cleanroommc.modularui.theme.ReloadThemeEvent.Post event) {
        TaskNHGui.notifyThemeReloaded();
    }
}
