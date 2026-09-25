package com.eldrinn.tasknh.hud;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

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
import com.eldrinn.tasknh.data.ChecklistItem;
import com.eldrinn.tasknh.data.Task;
import com.eldrinn.tasknh.data.TaskStatus;
import com.eldrinn.tasknh.event.ItemTrackHandler;
import com.eldrinn.tasknh.gui.ColorUtils;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class HudRenderer {

    private static final RenderItem RENDER_ITEM = new RenderItem();

    /**
     * Carried counts by tracked stack, valid while the inventory matches the snapshot. Keyed by identity: a
     * tracked stack belongs to one task or checklist item, so it always comes with the same OreDictionary name.
     */
    private static final Map<ItemStack, Integer> COUNTS = new IdentityHashMap<>();
    private static ItemStack[] snapshotStacks = new ItemStack[0];
    private static int[] snapshotSizes = new int[0];

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
                ColorUtils.backgroundHud.getColor());
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
        // Every frame starts here, both on the HUD and on the settings screen, so the counts are checked once.
        refreshCounts();
        int blockW = maxBlockWidth(pinned, fr, cfg);
        int totalH = totalHeight(pinned, cfg, fr);
        int x = anchorX(cfg.getAnchor(), sw, blockW) + cfg.getOffsetX();
        int y = anchorY(cfg.getAnchor(), sh, totalH) + cfg.getOffsetY();
        return new int[] { x, y, blockW, totalH };
    }

    private int drawTaskBlock(FontRenderer fr, PinnedTasksConfig cfg, Task task, int x, int y, int blockW) {
        int maxChecklist = cfg.getMaxChecklistShown();
        int textW = blockW - PADDING * 2;
        String statusText = "[" + task.status.displayName()
            .toUpperCase() + "]";
        fr.drawStringWithShadow(statusText, x, y, statusColor(task.status));
        y += LINE_H;

        ItemStack iconStack = task.iconItem;
        int titleX = iconStack != null ? x + ICON_SIZE + ICON_GAP : x;
        int titleW = titleWidth(task, textW);
        if (iconStack != null) drawItemIcon(iconStack, x, y);
        y = drawCounted(
            fr,
            task.title,
            task.trackItem,
            titleCount(task),
            titleX,
            y,
            titleW,
            "",
            ColorUtils.textWhite.getColor());

        if (!task.checklist.isEmpty()) {
            List<ChecklistItem> incomplete = task.checklist.stream()
                .filter(st -> !st.checked)
                .collect(java.util.stream.Collectors.toList());
            List<ChecklistItem> complete = task.checklist.stream()
                .filter(st -> st.checked)
                .collect(java.util.stream.Collectors.toList());

            int prefixW = fr.getStringWidth("- ");
            int checklistW = textW - PADDING - prefixW;
            int shown = 0;
            for (ChecklistItem st : incomplete) {
                if (shown >= maxChecklist) break;
                y = drawChecklistItem(fr, st, x + PADDING, y, checklistW, prefixW);
                shown++;
            }
            for (ChecklistItem st : complete) {
                if (shown >= maxChecklist) break;
                y = drawChecklistItem(fr, st, x + PADDING, y, checklistW, prefixW);
                shown++;
            }

            int remaining = task.checklist.size() - shown;
            if (remaining > 0) {
                fr.drawStringWithShadow(
                    StatCollector.translateToLocalFormatted("tasknh.gui.row.more", remaining),
                    x + PADDING,
                    y,
                    ColorUtils.textGray.getColor());
                y += LINE_H;
            }
        }

        return y;
    }

    /** Draws one checklist line: dash, then the wrapped text with its count. Checked items are struck. */
    private int drawChecklistItem(FontRenderer fr, ChecklistItem st, int x, int y, int checklistW, int prefixW) {
        String strike = st.checked ? "§m" : "";
        int color = st.checked ? ColorUtils.textGray.getColor() : ColorUtils.textWhite.getColor();
        fr.drawStringWithShadow(strike + "- ", x, y, color);
        return drawCounted(fr, st.title, st.trackItem, checklistCount(st), x + prefixW, y, checklistW, strike, color);
    }

    /**
     * Draws wrapped text, then the tracked item icon and its count after the last line, or on a line of its own
     * when they do not fit. The icon sits next to the count because a task icon may show something else.
     */
    private int drawCounted(FontRenderer fr, String text, @Nullable ItemStack track, @Nullable String count, int x,
        int y, int w, String format, int color) {
        List<String> lines = fr.listFormattedStringToWidth(text, w);
        for (String line : lines) {
            fr.drawStringWithShadow(format + line + "§r", x, y, color);
            y += LINE_H;
        }
        if (track == null || count == null) return y;
        int cx = x;
        int cy = y;
        if (countFits(fr, text, count, w)) {
            cx += fr.getStringWidth(lines.get(lines.size() - 1)) + ICON_GAP * 2;
            cy -= LINE_H;
        } else {
            y += LINE_H;
        }
        drawItemIcon(track, cx, cy);
        fr.drawStringWithShadow(count, cx + ICON_SIZE + ICON_GAP, cy, ColorUtils.textGray.getColor());
        return y;
    }

    /** Lines taken by text drawn with {@link #drawCounted}. */
    private static int countedLines(FontRenderer fr, String text, @Nullable String count, int w) {
        int lines = fr.listFormattedStringToWidth(text, w)
            .size();
        return count != null && !countFits(fr, text, count, w) ? lines + 1 : lines;
    }

    /** Gap, tracked item icon, gap and the count text, as drawn after the text. */
    private static int countWidth(FontRenderer fr, @Nullable String count) {
        return count == null ? 0 : ICON_GAP * 2 + ICON_SIZE + ICON_GAP + fr.getStringWidth(count);
    }

    private static boolean countFits(FontRenderer fr, String text, String count, int w) {
        List<String> lines = fr.listFormattedStringToWidth(text, w);
        return fr.getStringWidth(lines.get(lines.size() - 1)) + countWidth(fr, count) <= w;
    }

    private static int titleWidth(Task t, int textW) {
        return t.iconItem != null ? textW - ICON_SIZE - ICON_GAP : textW;
    }

    /** "have/need" for the task's tracked item while the task is open, or null. */
    @Nullable
    private static String titleCount(Task t) {
        if (t.status == TaskStatus.DONE || t.trackItem == null) return null;
        // Same floor as the server check, which treats a count below one as one.
        return countText(t.trackItem, "", Math.max(1, t.trackItemCount));
    }

    /** "have/need" for an unchecked tracked item, or null. */
    @Nullable
    private static String checklistCount(ChecklistItem st) {
        // A checked item stays checked once the items are spent, so its count would only confuse.
        if (st.checked || st.trackItem == null) return null;
        return countText(st.trackItem, st.trackOre, st.trackItemCount);
    }

    /**
     * Only the local inventory is counted: the server also checks each member on their own, so this is what
     * the player still has to gather.
     */
    private static String countText(ItemStack track, String ore, int need) {
        Integer have = COUNTS.get(track);
        if (have == null) {
            have = ItemTrackHandler.countItem(Minecraft.getMinecraft().thePlayer.inventory.mainInventory, track, ore);
            COUNTS.put(track, have);
        }
        return have + "/" + need;
    }

    /**
     * Drops the cached counts once the inventory differs from the snapshot they were taken from. Comparing the
     * stacks is far cheaper than counting: counting reads item names and OreDictionary ids for every slot, and a
     * frame asks for each count several times, for the width, the height and the drawing.
     * Each slot update from the server brings a new stack, and a size change on the client keeps the stack,
     * so the reference and the size cover both.
     */
    private static void refreshCounts() {
        ItemStack[] inventory = Minecraft.getMinecraft().thePlayer.inventory.mainInventory;
        boolean changed = inventory.length != snapshotStacks.length;
        for (int i = 0; !changed && i < inventory.length; i++) {
            ItemStack stack = inventory[i];
            changed = stack != snapshotStacks[i] || (stack != null && stack.stackSize != snapshotSizes[i]);
        }
        // Task syncs replace the tracked stacks, so stale keys pile up until the inventory changes. The cap
        // keeps a long idle session from growing the map.
        if (!changed && COUNTS.size() < 256) return;

        COUNTS.clear();
        if (snapshotStacks.length != inventory.length) {
            snapshotStacks = new ItemStack[inventory.length];
            snapshotSizes = new int[inventory.length];
        }
        for (int i = 0; i < inventory.length; i++) {
            snapshotStacks[i] = inventory[i];
            snapshotSizes[i] = inventory[i] != null ? inventory[i].stackSize : 0;
        }
    }

    static int totalHeight(List<Task> pinned, PinnedTasksConfig cfg, FontRenderer fr) {
        int maxSub = cfg.getMaxChecklistShown();
        int blockW = maxBlockWidth(pinned, fr, cfg);
        int textW = blockW - PADDING * 2;
        int prefixW = fr.getStringWidth("- ");
        int checklistW = textW - PADDING - prefixW;
        int h = 0;
        for (Task t : pinned) {
            h += LINE_H; // status
            int titleW = titleWidth(t, textW);
            h += LINE_H * countedLines(fr, t.title, titleCount(t), titleW);

            List<ChecklistItem> incomplete = t.checklist.stream()
                .filter(st -> !st.checked)
                .collect(java.util.stream.Collectors.toList());
            List<ChecklistItem> complete = t.checklist.stream()
                .filter(st -> st.checked)
                .collect(java.util.stream.Collectors.toList());

            int shown = 0;
            for (ChecklistItem st : incomplete) {
                if (shown >= maxSub) break;
                h += LINE_H * countedLines(fr, st.title, checklistCount(st), checklistW);
                shown++;
            }
            for (ChecklistItem st : complete) {
                if (shown >= maxSub) break;
                h += LINE_H * countedLines(fr, st.title, checklistCount(st), checklistW);
                shown++;
            }
            if (t.checklist.size() > shown) h += LINE_H; // "+N more"
            h += BLOCK_GAP;
        }
        return h;
    }

    static int maxBlockWidth(List<Task> pinned, FontRenderer fr, PinnedTasksConfig cfg) {
        int maxSub = cfg.getMaxChecklistShown();
        int max = 80;
        for (Task t : pinned) {
            max = Math.max(
                max,
                fr.getStringWidth(
                    "[" + t.status.displayName()
                        .toUpperCase() + "]"));
            int titlePrefix = t.iconItem != null ? ICON_SIZE + ICON_GAP : 0;
            max = Math.max(max, titlePrefix + fr.getStringWidth(t.title) + countWidth(fr, titleCount(t)));
            // Measure the items that get drawn: unchecked ones first, the same as drawTaskBlock. The sort is stable.
            List<ChecklistItem> shownItems = t.checklist.stream()
                .sorted(java.util.Comparator.comparing((ChecklistItem st) -> st.checked))
                // A hand-edited config can hold a negative limit, which limit() rejects.
                .limit(Math.max(0, maxSub))
                .collect(java.util.stream.Collectors.toList());
            for (ChecklistItem st : shownItems) {
                max = Math.max(max, PADDING + fr.getStringWidth("- " + st.title) + countWidth(fr, checklistCount(st)));
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
            case IN_PROGRESS -> ColorUtils.accentGold.getColor();
            case DONE -> ColorUtils.accentGreen.getColor();
            default -> ColorUtils.textGray.getColor();
        };
    }
}
