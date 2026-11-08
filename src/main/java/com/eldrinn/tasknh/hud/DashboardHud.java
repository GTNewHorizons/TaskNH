package com.eldrinn.tasknh.hud;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.StatCollector;

import com.eldrinn.tasknh.cache.TaskNHClientCache;
import com.eldrinn.tasknh.config.PinnedTasksConfig;
import com.eldrinn.tasknh.data.ChecklistItem;
import com.eldrinn.tasknh.data.Task;
import com.eldrinn.tasknh.data.TaskStatus;
import com.gtnewhorizons.dashboard.api.HudBounds;
import com.gtnewhorizons.dashboard.api.HudElement;
import com.gtnewhorizons.dashboard.api.HudRegistry;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Lets players move, resize and hide the pinned task list in Dashboard's HUD editor. The only class using Dashboard,
 * so only call it while Dashboard is loaded.
 */
@SideOnly(Side.CLIENT)
public final class DashboardHud {

    private static final HudElement PINNED_TASKS = new HudElement("tasknh:pinned_tasks", DashboardHud::getBounds);
    private static final HudRenderer RENDERER = new HudRenderer();

    private static Task sample;

    private DashboardHud() {}

    public static void register() {
        PINNED_TASKS.setPreview((screenWidth, screenHeight) -> HudRegistry.render(PINNED_TASKS, () -> {
            PinnedTasksConfig cfg = TaskNHClientCache.getPinConfig();
            RENDERER.draw(getTasksOrSample(cfg), cfg);
        }));
        HudRegistry.register(PINNED_TASKS);
    }

    /** False means the player hid the list, so skip drawing it and do not call {@link #endRender()}. */
    static boolean beginPinnedTasks(ScaledResolution resolution) {
        return HudRegistry.beginRender(PINNED_TASKS, resolution);
    }

    static void endRender() {
        HudRegistry.endRender();
    }

    /** The list keeps drawing in Dashboard's editor, like the rest of the HUD. */
    static boolean isEditorOpen() {
        return HudRegistry.isLayoutScreenOpen();
    }

    /** Where HudRenderer draws the list with its background, so the box grows and shrinks with the tasks. */
    private static HudBounds getBounds(int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) {
            return new HudBounds(2, 2, 80, 20);
        }
        PinnedTasksConfig cfg = TaskNHClientCache.getPinConfig();
        int[] pos = HudRenderer
            .computeHudPosition(cfg, screenWidth, screenHeight, mc.fontRenderer, getTasksOrSample(cfg));
        double scale = cfg.getScale();
        int x = (int) (pos[0] / scale);
        int y = (int) (pos[1] / scale);
        int left = (int) Math.floor((x - HudRenderer.PADDING) * scale);
        int top = (int) Math.floor((y - HudRenderer.PADDING) * scale);
        int right = (int) Math.ceil((x + pos[2] + HudRenderer.PADDING) * scale);
        int bottom = (int) Math.ceil((y + pos[3] + HudRenderer.PADDING) * scale);
        return new HudBounds(left, top, right - left, bottom - top);
    }

    /** A sample task when nothing is pinned, so the editor still shows what is being placed. */
    private static List<Task> getTasksOrSample(PinnedTasksConfig cfg) {
        List<Task> tasks = HudRenderer.getShownTasks(cfg);
        return tasks.isEmpty() ? Collections.singletonList(getSample()) : tasks;
    }

    /** Made on first use, once the language is loaded. */
    private static Task getSample() {
        if (sample == null) {
            sample = new Task(
                UUID.randomUUID(),
                StatCollector.translateToLocal("dashboard.element.tasknh.pinned_tasks.sample"),
                "",
                TaskStatus.IN_PROGRESS);
            sample.checklist.add(
                new ChecklistItem(
                    UUID.randomUUID(),
                    StatCollector.translateToLocal("dashboard.element.tasknh.pinned_tasks.sample_item"),
                    false));
        }
        return sample;
    }
}
