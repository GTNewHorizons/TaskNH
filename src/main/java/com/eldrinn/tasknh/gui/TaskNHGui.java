package com.eldrinn.tasknh.gui;

import net.minecraft.client.Minecraft;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cleanroommc.modularui.api.IMuiScreen;
import com.cleanroommc.modularui.factory.ClientGUI;
import com.cleanroommc.modularui.screen.ModularContainer;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.widgets.PagedWidget;
import com.eldrinn.tasknh.gui.widget.TaskDetailWidget;
import com.eldrinn.tasknh.gui.widget.TaskListWidget;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class TaskNHGui {

    private static final Logger LOG = LogManager.getLogger("TaskNHGui");

    public static final int WIDTH = 380;
    public static final int LEFT_WIDTH = 380;
    public static final int PADDING = 6;

    public static int getHeight() {
        Minecraft mc = Minecraft.getMinecraft();
        net.minecraft.client.gui.ScaledResolution sr = new net.minecraft.client.gui.ScaledResolution(
            mc,
            mc.displayWidth,
            mc.displayHeight);
        return Math.min(580, (int) (sr.getScaledHeight() * 0.9));
    }

    public static final String THEME_DARK = "tasknh_dark";
    public static final String THEME_LIGHT = "tasknh_light";

    /** Currently active theme id. Persists for the session. */
    private static String currentTheme = THEME_DARK;

    public static boolean isDarkTheme() {
        return THEME_DARK.equals(currentTheme);
    }

    public static void toggleTheme() {
        currentTheme = THEME_DARK.equals(currentTheme) ? THEME_LIGHT : THEME_DARK;
    }

    private static volatile TaskNHGuiData pendingOpen = null;
    /** Last data used to open the GUI — used to refresh on server sync. */
    private static TaskNHGuiData activeData = null;

    public static void open() {
        open(new TaskNHGuiData());
    }

    public static void open(TaskNHGuiData data) {
        LOG.info(
            "[TaskNHGui] open() queued — tab={} selected={} createMode={}",
            data.activeTab,
            data.selectedTaskId,
            data.createMode);
        pendingOpen = data;
    }

    /**
     * Called by SyncAllTasksPacket after cache is updated.
     * If the TaskNH GUI is currently open, schedules a rebuild with fresh data.
     */
    @SideOnly(Side.CLIENT)
    public static void notifySyncReceived() {
        if (activeData == null) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (!(mc.currentScreen instanceof IMuiScreen)) {
            activeData = null; // GUI was closed
            return;
        }
        if (pendingOpen == null) { // don't overwrite a user-triggered open
            open(activeData);
        }
    }

    /** Called from ClientProxy.onClientTick to drain pending open request. */
    public static void tick() {
        TaskNHGuiData data = pendingOpen;
        if (data == null) return;
        pendingOpen = null;
        LOG.info("[TaskNHGui] tick() draining — tab={}", data.activeTab);
        try {
            LOG.info("[TaskNHGui] tick() building panel");
            int height = getHeight();
            ModularPanel panel = ModularPanel.defaultPanel("tasknh_main", WIDTH, height);
            panel.themeOverride(currentTheme);
            LOG.info("[TaskNHGui] tick() building widgets");
            int initialPage = (data.createMode || data.selectedTaskId != null) ? 1 : 0;
            panel.child(
                new PagedWidget<>().size(WIDTH, height)
                    .addPage(new TaskListWidget(data))
                    .addPage(new TaskDetailWidget(data))
                    .controller(data.pageController)
                    .initialPage(initialPage));
            LOG.info("[TaskNHGui] tick() calling ClientGUI.open()");
            UISettings settings = new UISettings();
            settings.getRecipeViewerSettings()
                .enable();
            settings.customContainer(ModularContainer::new);
            ClientGUI.open(new ModularScreen("tasknh", panel), settings);
            activeData = data;
            LOG.info("[TaskNHGui] tick() done");
        } catch (Exception e) {
            LOG.error("[TaskNHGui] tick() FAILED", e);
        }
    }
}
