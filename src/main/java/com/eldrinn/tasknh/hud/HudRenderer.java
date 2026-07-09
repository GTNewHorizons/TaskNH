package com.eldrinn.tasknh.hud;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraftforge.client.event.RenderGameOverlayEvent;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import com.eldrinn.tasknh.cache.TaskNHClientCache;
import com.eldrinn.tasknh.config.PinnedTasksConfig;
import com.eldrinn.tasknh.data.Subtask;
import com.eldrinn.tasknh.data.Task;
import com.eldrinn.tasknh.data.TaskStatus;
import com.eldrinn.tasknh.gui.ColorUtils;
import com.eldrinn.tasknh.gui.widget.IconSlotWidget;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class HudRenderer {

    private static final RenderItem RENDER_ITEM = new RenderItem();

    private static final int MAX_BLOCK_WIDTH = 160;
    private static final int LINE_H = 10;
    private static final int ICON_SIZE = 10; // item icon scaled to match line height
    private static final int ICON_GAP = 2;
    private static final int BLOCK_GAP = 4;
    static final int PADDING = 4;

    @SubscribeEvent
    public void onRenderHud(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.TEXT) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.currentScreen != null && !(mc.currentScreen instanceof HudSettingsScreen)) return;

        PinnedTasksConfig cfg = TaskNHClientCache.getPinConfig();
        if (!cfg.isHudVisible()) return;

        List<Task> all = TaskNHClientCache.getPinnedTasks();
        if (all.isEmpty()) return;
        int maxTasks = cfg.getMaxPinnedTasks();
        List<Task> pinned = all.size() > maxTasks ? all.subList(0, maxTasks) : all;

        ScaledResolution res = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        int sw = res.getScaledWidth();
        int sh = res.getScaledHeight();

        int[] pos = computeHudPosition(cfg, sw, sh, mc.fontRenderer, pinned);
        int startX = pos[0];
        int startY = pos[1];
        int blockW = pos[2];
        int totalHeight = pos[3];

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushMatrix();
        GL11.glScaled(cfg.getScale(), cfg.getScale(), 1.0);

        // Coordinates must be divided by scale because GL matrix already scaled up
        double s = cfg.getScale();
        int sx = (int) (startX / s);
        int sy = (int) (startY / s);

        if (cfg.isShowBackground()) {
            net.minecraft.client.gui.Gui.drawRect(
                sx - PADDING,
                sy - PADDING,
                sx + blockW + PADDING,
                sy + totalHeight + PADDING,
                ColorUtils.BG_HUD.getColor());
        }

        int y = sy;
        for (Task task : pinned) {
            y = drawTaskBlock(mc.fontRenderer, cfg, task, sx, y, blockW);
            y += BLOCK_GAP;
        }

        GL11.glPopMatrix();
        GL11.glPopAttrib();
    }

    /**
     * Returns [startX, startY, blockW, totalH] for the HUD block in screen coordinates.
     * Used by HudSettingsScreen to position the drag handle.
     */
    public static int[] computeHudPosition(PinnedTasksConfig cfg, int sw, int sh, FontRenderer fr, List<Task> pinned) {
        int blockW = maxBlockWidth(pinned, fr, cfg);
        int totalH = totalHeight(pinned, cfg, fr);
        int x = anchorX(cfg.getAnchor(), sw, blockW) + cfg.getOffsetX();
        int y = anchorY(cfg.getAnchor(), sh, totalH) + cfg.getOffsetY();
        return new int[] { x, y, blockW, totalH };
    }

    private int drawTaskBlock(FontRenderer fr, PinnedTasksConfig cfg, Task task, int x, int y, int blockW) {
        int maxSubtasks = cfg.getMaxSubtasksShown();
        int textW = blockW - PADDING * 2;
        String statusText = "[" + task.status.displayName()
            .toUpperCase() + "]";
        fr.drawStringWithShadow(statusText, x, y, statusColor(task.status));
        y += LINE_H;

        ItemStack iconStack = IconSlotWidget.parseIconItem(task.iconItem);
        if (iconStack != null) {
            drawItemIcon(iconStack, x, y);
            for (String line : fr.listFormattedStringToWidth(task.title, textW - ICON_SIZE - ICON_GAP)) {
                fr.drawStringWithShadow(line, x + ICON_SIZE + ICON_GAP, y, ColorUtils.TEXT_WHITE.getColor());
                y += LINE_H;
            }
        } else {
            for (String line : fr.listFormattedStringToWidth(task.title, textW)) {
                fr.drawStringWithShadow(line, x, y, ColorUtils.TEXT_WHITE.getColor());
                y += LINE_H;
            }
        }

        if (!task.subtasks.isEmpty()) {
            List<Subtask> incomplete = task.subtasks.stream()
                .filter(st -> !st.checked)
                .collect(java.util.stream.Collectors.toList());
            List<Subtask> complete = task.subtasks.stream()
                .filter(st -> st.checked)
                .collect(java.util.stream.Collectors.toList());

            int prefixW = fr.getStringWidth("- ");
            int subtaskW = textW - PADDING - prefixW;
            int shown = 0;
            for (Subtask st : incomplete) {
                if (shown >= maxSubtasks) break;
                List<String> lines = fr.listFormattedStringToWidth(st.title, subtaskW);
                fr.drawStringWithShadow("- " + lines.get(0), x + PADDING, y, ColorUtils.TEXT_WHITE.getColor());
                y += LINE_H;
                for (int i = 1; i < lines.size(); i++) {
                    fr.drawStringWithShadow("  " + lines.get(i), x + PADDING, y, ColorUtils.TEXT_WHITE.getColor());
                    y += LINE_H;
                }
                shown++;
            }
            for (Subtask st : complete) {
                if (shown >= maxSubtasks) break;
                List<String> lines = fr.listFormattedStringToWidth(st.title, subtaskW);
                fr.drawStringWithShadow("§m- " + lines.get(0) + "§r", x + PADDING, y, ColorUtils.TEXT_GRAY.getColor());
                y += LINE_H;
                for (int i = 1; i < lines.size(); i++) {
                    fr.drawStringWithShadow(
                        "§m  " + lines.get(i) + "§r",
                        x + PADDING,
                        y,
                        ColorUtils.TEXT_GRAY.getColor());
                    y += LINE_H;
                }
                shown++;
            }

            int remaining = task.subtasks.size() - shown;
            if (remaining > 0) {
                fr.drawStringWithShadow(
                    StatCollector.translateToLocalFormatted("tasknh.gui.row.more", remaining),
                    x + PADDING,
                    y,
                    ColorUtils.TEXT_GRAY.getColor());
                y += LINE_H;
            }
        }

        return y;
    }

    static int totalHeight(List<Task> pinned, PinnedTasksConfig cfg, FontRenderer fr) {
        int maxSub = cfg.getMaxSubtasksShown();
        int blockW = maxBlockWidth(pinned, fr, cfg);
        int textW = blockW - PADDING * 2;
        int prefixW = fr.getStringWidth("- ");
        int subtaskW = textW - PADDING - prefixW;
        int h = 0;
        for (Task t : pinned) {
            h += LINE_H; // status
            int titleW = t.iconItem != null ? textW - ICON_SIZE - ICON_GAP : textW;
            h += LINE_H * fr.listFormattedStringToWidth(t.title, titleW)
                .size();

            List<Subtask> incomplete = t.subtasks.stream()
                .filter(st -> !st.checked)
                .collect(java.util.stream.Collectors.toList());
            List<Subtask> complete = t.subtasks.stream()
                .filter(st -> st.checked)
                .collect(java.util.stream.Collectors.toList());

            int shown = 0;
            for (Subtask st : incomplete) {
                if (shown >= maxSub) break;
                h += LINE_H * fr.listFormattedStringToWidth(st.title, subtaskW)
                    .size();
                shown++;
            }
            for (Subtask st : complete) {
                if (shown >= maxSub) break;
                h += LINE_H * fr.listFormattedStringToWidth(st.title, subtaskW)
                    .size();
                shown++;
            }
            if (t.subtasks.size() > shown) h += LINE_H; // "+N more"
            h += BLOCK_GAP;
        }
        return h;
    }

    static int maxBlockWidth(List<Task> pinned, FontRenderer fr, PinnedTasksConfig cfg) {
        int maxSub = cfg.getMaxSubtasksShown();
        int max = 80;
        for (Task t : pinned) {
            max = Math.max(
                max,
                fr.getStringWidth(
                    "[" + t.status.displayName()
                        .toUpperCase() + "]"));
            int titlePrefix = t.iconItem != null ? ICON_SIZE + ICON_GAP : 0;
            max = Math.max(max, titlePrefix + fr.getStringWidth(t.title));
            int shown = 0;
            for (Subtask st : t.subtasks) {
                if (shown >= maxSub) break;
                max = Math.max(max, PADDING + fr.getStringWidth("- " + st.title));
                shown++;
            }
        }
        return Math.min(max + PADDING * 2, MAX_BLOCK_WIDTH);
    }

    static int anchorX(PinnedTasksConfig.Anchor anchor, int sw, int blockW) {
        return switch (anchor) {
            case TOP_LEFT, MIDDLE_LEFT, BOTTOM_LEFT -> 2;
            case TOP_CENTER, MIDDLE_CENTER, BOTTOM_CENTER -> (sw - blockW) / 2;
            default -> sw - blockW - 2; // RIGHT
        };
    }

    static int anchorY(PinnedTasksConfig.Anchor anchor, int sh, int totalH) {
        return switch (anchor) {
            case TOP_LEFT, TOP_CENTER, TOP_RIGHT -> 2;
            case MIDDLE_LEFT, MIDDLE_CENTER, MIDDLE_RIGHT -> (sh - totalH) / 2;
            default -> sh - totalH - 2; // BOTTOM
        };
    }

    private void drawItemIcon(ItemStack stack, int x, int y) {
        Minecraft mc = Minecraft.getMinecraft();
        RenderHelper.enableGUIStandardItemLighting();
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);

        // Scale 16x16 item down to ICON_SIZE
        float s = ICON_SIZE / 16.0f;
        GL11.glPushMatrix();
        GL11.glTranslatef(x, y, 0);
        GL11.glScalef(s, s, s);
        RENDER_ITEM.renderItemIntoGUI(mc.fontRenderer, mc.renderEngine, stack, 0, 0);
        GL11.glPopMatrix();

        RenderHelper.disableStandardItemLighting();
        GL11.glColor4f(1f, 1f, 1f, 1f);
    }

    private int statusColor(TaskStatus status) {
        return switch (status) {
            case IN_PROGRESS -> ColorUtils.GOLD.getColor();
            case DONE -> ColorUtils.GREEN.getColor();
            default -> ColorUtils.TEXT_GRAY.getColor();
        };
    }
}
