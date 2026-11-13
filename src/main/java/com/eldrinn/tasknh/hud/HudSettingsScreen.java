package com.eldrinn.tasknh.hud;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.StatCollector;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import com.cleanroommc.modularui.drawable.GuiTextures;
import com.eldrinn.tasknh.cache.TaskNHClientCache;
import com.eldrinn.tasknh.config.PinnedTasksConfig;
import com.eldrinn.tasknh.data.Task;
import com.eldrinn.tasknh.gui.ColorUtils;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class HudSettingsScreen extends GuiScreen {

    private static final int HANDLE_SIZE = 10;
    private static final int PANEL_H = 24;
    private static final int PANEL_PADDING = 6;
    private static final int SNAP_DISTANCE = 5;

    private boolean dragging = false;
    private int dragOffsetX; // mouse offset within handle at drag start
    private int dragOffsetY;
    private int dragAnchorX; // anchorX at drag start (constant during drag)
    private int dragAnchorY;
    private double guideX = Double.NaN; // screen x of the line the list snapped to, NaN when none
    private double guideY = Double.NaN;

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawRect(0, 0, width, height, ColorUtils.backgroundOverlay.getColor());

        PinnedTasksConfig cfg = TaskNHClientCache.getPinConfig();
        ScaledResolution res = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        int sw = res.getScaledWidth();
        int sh = res.getScaledHeight();

        boolean movedInDashboard = isMovedInDashboard();
        String hint = StatCollector
            .translateToLocal(movedInDashboard ? "tasknh.hud.settings.hint_dashboard" : "tasknh.hud.settings.hint");
        fontRendererObj.drawStringWithShadow(
            hint,
            (sw - fontRendererObj.getStringWidth(hint)) / 2,
            6,
            ColorUtils.textGray.getColor());

        if (!movedInDashboard) {
            int[] pos = hudPos(cfg, sw, sh);
            int hx = pos[0];
            int hy = pos[1];
            drawRect(hx, hy, hx + HANDLE_SIZE, hy + HANDLE_SIZE, ColorUtils.backgroundHandle.getColor());
            GL11.glColor4f(1f, 1f, 1f, 1f);
            GuiTextures.ALL_DIRECTIONS.draw(hx, hy, HANDLE_SIZE, HANDLE_SIZE);
            GL11.glColor4f(1f, 1f, 1f, 1f);
        }

        if (!Double.isNaN(guideX)) {
            int gx = Math.min((int) guideX, sw - 1);
            drawRect(gx, 0, gx + 1, sh, ColorUtils.accentGreen.getColor());
        }
        if (!Double.isNaN(guideY)) {
            int gy = Math.min((int) guideY, sh - 1);
            drawRect(0, gy, sw, gy + 1, ColorUtils.accentGreen.getColor());
        }

        drawControlPanel(cfg, sw, sh);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    /** The handle would move a position the list no longer uses. */
    private static boolean isMovedInDashboard() {
        return HudRenderer.DASHBOARD && DashboardHud.isMoved();
    }

    private int[] hudPos(PinnedTasksConfig cfg, int sw, int sh) {
        List<Task> pinned = TaskNHClientCache.getPinnedTasks();
        return HudRenderer.computeHudPosition(cfg, sw, sh, fontRendererObj, pinned);
    }

    private void drawControlPanel(PinnedTasksConfig cfg, int sw, int sh) {
        int panelW = 500;
        int px = (sw - panelW) / 2;
        int py = sh - PANEL_H - PANEL_PADDING;

        drawRect(px - 4, py - 4, px + panelW + 4, py + PANEL_H + 4, ColorUtils.backgroundPanel.getColor());

        int cx = px;

        // Scale
        String scaleKey = StatCollector.translateToLocal("tasknh.hud.settings.scale");
        fontRendererObj.drawStringWithShadow(scaleKey, cx, py + 7, ColorUtils.textGray.getColor());
        cx += fontRendererObj.getStringWidth(scaleKey) + 4;

        drawRect(cx, py + 2, cx + 14, py + 22, ColorUtils.backgroundButton.getColor());
        fontRendererObj.drawStringWithShadow("-", cx + 4, py + 7, ColorUtils.textWhite.getColor());
        cx += 16;

        String scaleLabel = String.format("%.2fx", cfg.getScale());
        fontRendererObj.drawStringWithShadow(scaleLabel, cx, py + 7, ColorUtils.textWhite.getColor());
        cx += fontRendererObj.getStringWidth(scaleLabel) + 4;

        drawRect(cx, py + 2, cx + 14, py + 22, ColorUtils.backgroundButton.getColor());
        fontRendererObj.drawStringWithShadow("+", cx + 3, py + 7, ColorUtils.textWhite.getColor());
        cx += 20;

        // Checklist
        String checklistKey = StatCollector.translateToLocal("tasknh.hud.settings.checklist");
        fontRendererObj.drawStringWithShadow(checklistKey, cx, py + 7, ColorUtils.textGray.getColor());
        cx += fontRendererObj.getStringWidth(checklistKey) + 4;

        drawRect(cx, py + 2, cx + 14, py + 22, ColorUtils.backgroundButton.getColor());
        fontRendererObj.drawStringWithShadow("-", cx + 4, py + 7, ColorUtils.textWhite.getColor());
        cx += 16;

        String checklistLabel = String.valueOf(cfg.getMaxChecklistShown());
        fontRendererObj.drawStringWithShadow(checklistLabel, cx, py + 7, ColorUtils.textWhite.getColor());
        cx += fontRendererObj.getStringWidth(checklistLabel) + 4;

        drawRect(cx, py + 2, cx + 14, py + 22, ColorUtils.backgroundButton.getColor());
        fontRendererObj.drawStringWithShadow("+", cx + 3, py + 7, ColorUtils.textWhite.getColor());
        cx += 20;

        // Subtasks
        String subtasksKey = StatCollector.translateToLocal("tasknh.hud.settings.subtasks");
        fontRendererObj.drawStringWithShadow(subtasksKey, cx, py + 7, ColorUtils.textGray.getColor());
        cx += fontRendererObj.getStringWidth(subtasksKey) + 4;

        drawRect(cx, py + 2, cx + 14, py + 22, ColorUtils.backgroundButton.getColor());
        fontRendererObj.drawStringWithShadow("-", cx + 4, py + 7, ColorUtils.textWhite.getColor());
        cx += 16;

        String subtasksLabel = String.valueOf(cfg.getMaxSubtasksShown());
        fontRendererObj.drawStringWithShadow(subtasksLabel, cx, py + 7, ColorUtils.textWhite.getColor());
        cx += fontRendererObj.getStringWidth(subtasksLabel) + 4;

        drawRect(cx, py + 2, cx + 14, py + 22, ColorUtils.backgroundButton.getColor());
        fontRendererObj.drawStringWithShadow("+", cx + 3, py + 7, ColorUtils.textWhite.getColor());
        cx += 20;

        // Pins
        String pinsKey = StatCollector.translateToLocal("tasknh.hud.settings.tasks");
        fontRendererObj.drawStringWithShadow(pinsKey, cx, py + 7, ColorUtils.textGray.getColor());
        cx += fontRendererObj.getStringWidth(pinsKey) + 4;

        drawRect(cx, py + 2, cx + 14, py + 22, ColorUtils.backgroundButton.getColor());
        fontRendererObj.drawStringWithShadow("-", cx + 4, py + 7, ColorUtils.textWhite.getColor());
        cx += 16;

        String pinsLabel = String.valueOf(cfg.getMaxPinnedTasks());
        fontRendererObj.drawStringWithShadow(pinsLabel, cx, py + 7, ColorUtils.textWhite.getColor());
        cx += fontRendererObj.getStringWidth(pinsLabel) + 4;

        drawRect(cx, py + 2, cx + 14, py + 22, ColorUtils.backgroundButton.getColor());
        fontRendererObj.drawStringWithShadow("+", cx + 3, py + 7, ColorUtils.textWhite.getColor());
        cx += 20;

        // BG toggle
        String bgLabel = StatCollector.translateToLocalFormatted(
            "tasknh.hud.settings.bg",
            cfg.isShowBackground() ? StatCollector.translateToLocal("tasknh.hud.on")
                : StatCollector.translateToLocal("tasknh.hud.off"));
        int bgColor = cfg.isShowBackground() ? ColorUtils.accentGreen.getColor() : ColorUtils.textGray.getColor();
        drawRect(
            cx,
            py + 2,
            cx + fontRendererObj.getStringWidth(bgLabel) + 8,
            py + 22,
            ColorUtils.backgroundButton.getColor());
        fontRendererObj.drawStringWithShadow(bgLabel, cx + 4, py + 7, bgColor);
        cx += fontRendererObj.getStringWidth(bgLabel) + 12;

        // HUD toggle
        String hudLabel = StatCollector.translateToLocalFormatted(
            "tasknh.hud.settings.hud",
            cfg.isHudVisible() ? StatCollector.translateToLocal("tasknh.hud.on")
                : StatCollector.translateToLocal("tasknh.hud.off"));
        int hudColor = cfg.isHudVisible() ? ColorUtils.accentGreen.getColor() : ColorUtils.textGray.getColor();
        drawRect(
            cx,
            py + 2,
            cx + fontRendererObj.getStringWidth(hudLabel) + 8,
            py + 22,
            ColorUtils.backgroundButton.getColor());
        fontRendererObj.drawStringWithShadow(hudLabel, cx + 4, py + 7, hudColor);
        cx += fontRendererObj.getStringWidth(hudLabel) + 12;

        // Reset
        String resetLabel = StatCollector.translateToLocal("tasknh.hud.settings.reset");
        drawRect(
            cx,
            py + 2,
            cx + fontRendererObj.getStringWidth(resetLabel) + 8,
            py + 22,
            ColorUtils.backgroundDanger.getColor());
        fontRendererObj.drawStringWithShadow(resetLabel, cx + 4, py + 7, ColorUtils.textWhite.getColor());
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) {
        if (button != 0) return;
        PinnedTasksConfig cfg = TaskNHClientCache.getPinConfig();
        ScaledResolution res = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        int sw = res.getScaledWidth();
        int sh = res.getScaledHeight();

        int[] pos = hudPos(cfg, sw, sh);
        int hx = pos[0];
        int hy = pos[1];

        if (!isMovedInDashboard() && mouseX >= hx
            && mouseX <= hx + HANDLE_SIZE
            && mouseY >= hy
            && mouseY <= hy + HANDLE_SIZE) {
            dragging = true;
            dragOffsetX = mouseX - hx;
            dragOffsetY = mouseY - hy;
            // anchorX = hx - offsetX; store so drag updates offset correctly
            dragAnchorX = hx - cfg.getOffsetX();
            dragAnchorY = hy - cfg.getOffsetY();
            return;
        }

        handlePanelClick(cfg, mouseX, mouseY, sw, sh);
    }

    private void handlePanelClick(PinnedTasksConfig cfg, int mouseX, int mouseY, int sw, int sh) {
        int panelW = 500;
        int px = (sw - panelW) / 2;
        int py = sh - PANEL_H - PANEL_PADDING;

        if (mouseY < py - 4 || mouseY > py + PANEL_H + 4) return;

        int cx = px;

        // Scale
        cx += fontRendererObj.getStringWidth(StatCollector.translateToLocal("tasknh.hud.settings.scale")) + 4;
        if (mouseX >= cx && mouseX <= cx + 14) {
            cfg.setScale(cfg.getScale() - 0.25);
            return;
        }
        cx += 16;
        String scaleLabel = String.format("%.2fx", cfg.getScale());
        cx += fontRendererObj.getStringWidth(scaleLabel) + 4;
        if (mouseX >= cx && mouseX <= cx + 14) {
            cfg.setScale(cfg.getScale() + 0.25);
            return;
        }
        cx += 20;

        // Checklist
        cx += fontRendererObj.getStringWidth(StatCollector.translateToLocal("tasknh.hud.settings.checklist")) + 4;
        if (mouseX >= cx && mouseX <= cx + 14) {
            cfg.setMaxChecklistShown(cfg.getMaxChecklistShown() - 1);
            return;
        }
        cx += 16;
        String checklistLabel = String.valueOf(cfg.getMaxChecklistShown());
        cx += fontRendererObj.getStringWidth(checklistLabel) + 4;
        if (mouseX >= cx && mouseX <= cx + 14) {
            cfg.setMaxChecklistShown(cfg.getMaxChecklistShown() + 1);
            return;
        }
        cx += 20;

        // Subtasks
        cx += fontRendererObj.getStringWidth(StatCollector.translateToLocal("tasknh.hud.settings.subtasks")) + 4;
        if (mouseX >= cx && mouseX <= cx + 14) {
            cfg.setMaxSubtasksShown(cfg.getMaxSubtasksShown() - 1);
            return;
        }
        cx += 16;
        String subtasksLabel = String.valueOf(cfg.getMaxSubtasksShown());
        cx += fontRendererObj.getStringWidth(subtasksLabel) + 4;
        if (mouseX >= cx && mouseX <= cx + 14) {
            cfg.setMaxSubtasksShown(cfg.getMaxSubtasksShown() + 1);
            return;
        }
        cx += 20;

        // Pins
        cx += fontRendererObj.getStringWidth(StatCollector.translateToLocal("tasknh.hud.settings.tasks")) + 4;
        if (mouseX >= cx && mouseX <= cx + 14) {
            cfg.setMaxPinnedTasks(cfg.getMaxPinnedTasks() - 1);
            return;
        }
        cx += 16;
        String pinsLabel = String.valueOf(cfg.getMaxPinnedTasks());
        cx += fontRendererObj.getStringWidth(pinsLabel) + 4;
        if (mouseX >= cx && mouseX <= cx + 14) {
            cfg.setMaxPinnedTasks(cfg.getMaxPinnedTasks() + 1);
            return;
        }
        cx += 20;

        // BG toggle
        String bgLabel = StatCollector.translateToLocalFormatted(
            "tasknh.hud.settings.bg",
            cfg.isShowBackground() ? StatCollector.translateToLocal("tasknh.hud.on")
                : StatCollector.translateToLocal("tasknh.hud.off"));
        if (mouseX >= cx && mouseX <= cx + fontRendererObj.getStringWidth(bgLabel) + 8) {
            cfg.setShowBackground(!cfg.isShowBackground());
            return;
        }
        cx += fontRendererObj.getStringWidth(bgLabel) + 12;

        // HUD toggle
        String hudLabel = StatCollector.translateToLocalFormatted(
            "tasknh.hud.settings.hud",
            cfg.isHudVisible() ? StatCollector.translateToLocal("tasknh.hud.on")
                : StatCollector.translateToLocal("tasknh.hud.off"));
        if (mouseX >= cx && mouseX <= cx + fontRendererObj.getStringWidth(hudLabel) + 8) {
            cfg.setHudVisible(!cfg.isHudVisible());
            return;
        }
        cx += fontRendererObj.getStringWidth(hudLabel) + 12;

        // Reset
        String resetLabel = StatCollector.translateToLocal("tasknh.hud.settings.reset");
        if (mouseX >= cx && mouseX <= cx + fontRendererObj.getStringWidth(resetLabel) + 8) {
            cfg.resetToDefaults();
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int button, long timeSinceLastClick) {
        if (!dragging || button != 0) return;
        PinnedTasksConfig cfg = TaskNHClientCache.getPinConfig();
        ScaledResolution res = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        double[] box = visibleBox(cfg);
        boolean snap = !Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) && !Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
        double[] guide = new double[1];
        int x = fitAxis(mouseX - dragOffsetX, box[0], box[1], res.getScaledWidth(), snap, guide);
        guideX = guide[0];
        int y = fitAxis(mouseY - dragOffsetY, box[0], box[2], res.getScaledHeight(), snap, guide);
        guideY = guide[0];
        // offset = absolute position - anchor (keeps offsetX/Y as delta from anchor)
        cfg.setOffsetXRaw(x - dragAnchorX);
        cfg.setOffsetYRaw(y - dragAnchorY);
    }

    /** Returns [padding, width, height] of the box the player sees, in screen pixels. */
    private double[] visibleBox(PinnedTasksConfig cfg) {
        List<Task> pinned = TaskNHClientCache.getPinnedTasks();
        double scale = cfg.getScale();
        double w = (HudRenderer.maxBlockWidth(pinned, fontRendererObj, cfg) + HudRenderer.PADDING * 2) * scale;
        double h = (HudRenderer.totalHeight(pinned, cfg, fontRendererObj) + HudRenderer.PADDING * 2) * scale;
        return new double[] { HudRenderer.PADDING * scale, w, h };
    }

    /** Keeps the box on screen, snaps it if asked, returns the text start and sets guide[0] to the snap line or NaN. */
    private static int fitAxis(int start, double padBefore, double size, int screen, boolean snap, double[] guide) {
        double low = Math.max(0, Math.min(start - padBefore, screen - size));
        guide[0] = Double.NaN;
        if (snap) {
            if (low < SNAP_DISTANCE) {
                low = 0;
                guide[0] = 0;
            } else if (screen - size - low < SNAP_DISTANCE) {
                low = screen - size;
                guide[0] = screen;
            } else if (Math.abs(low + size / 2 - screen / 2.0) < SNAP_DISTANCE) {
                low = (screen - size) / 2;
                guide[0] = screen / 2.0;
            }
        }
        return (int) Math.round(low + padBefore);
    }

    /** Moves the list by whole pixels, kept on screen. */
    private void nudge(int dx, int dy) {
        PinnedTasksConfig cfg = TaskNHClientCache.getPinConfig();
        ScaledResolution res = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        int[] pos = HudRenderer.computeHudPosition(
            cfg,
            res.getScaledWidth(),
            res.getScaledHeight(),
            fontRendererObj,
            TaskNHClientCache.getPinnedTasks());
        double[] box = visibleBox(cfg);
        double[] unused = new double[1];
        int x = fitAxis(pos[0] + dx, box[0], box[1], res.getScaledWidth(), false, unused);
        int y = fitAxis(pos[1] + dy, box[0], box[2], res.getScaledHeight(), false, unused);
        cfg.setOffsetXRaw(x - (pos[0] - cfg.getOffsetX()));
        cfg.setOffsetYRaw(y - (pos[1] - cfg.getOffsetY()));
    }

    @Override
    protected void mouseMovedOrUp(int mouseX, int mouseY, int button) {
        if (button == 0 && dragging) {
            dragging = false;
            guideX = Double.NaN;
            guideY = Double.NaN;
            PinnedTasksConfig cfg = TaskNHClientCache.getPinConfig();
            ScaledResolution res = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
            int sw = res.getScaledWidth();
            int sh = res.getScaledHeight();
            List<Task> pinned = TaskNHClientCache.getPinnedTasks();
            PinnedTasksConfig cfg2 = TaskNHClientCache.getPinConfig();
            int blockW = HudRenderer.maxBlockWidth(pinned, fontRendererObj, cfg2);
            int totalH = HudRenderer.totalHeight(pinned, cfg2, fontRendererObj);
            int[] pos = HudRenderer.computeHudPosition(cfg, sw, sh, fontRendererObj, pinned);
            int hudX = pos[0];
            int hudY = pos[1];
            PinnedTasksConfig.Anchor newAnchor = bestAnchor(hudX + blockW / 2, hudY + totalH / 2, sw, sh);
            cfg.setAnchor(newAnchor);
            cfg.setOffsetXRaw(hudX - HudRenderer.anchorX(newAnchor, sw, blockW));
            cfg.setOffsetYRaw(hudY - HudRenderer.anchorY(newAnchor, sh, totalH));
            cfg.save();
        }
    }

    private static PinnedTasksConfig.Anchor bestAnchor(int cx, int cy, int sw, int sh) {
        boolean left = cx < sw / 2;
        boolean top = cy < sh / 2;
        if (top) return left ? PinnedTasksConfig.Anchor.TOP_LEFT : PinnedTasksConfig.Anchor.TOP_RIGHT;
        return left ? PinnedTasksConfig.Anchor.BOTTOM_LEFT : PinnedTasksConfig.Anchor.BOTTOM_RIGHT;
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            Minecraft.getMinecraft()
                .displayGuiScreen(null);
            return;
        }
        if (isMovedInDashboard()) return;
        switch (keyCode) {
            case Keyboard.KEY_LEFT -> nudge(-1, 0);
            case Keyboard.KEY_RIGHT -> nudge(1, 0);
            case Keyboard.KEY_UP -> nudge(0, -1);
            case Keyboard.KEY_DOWN -> nudge(0, 1);
            default -> {}
        }
    }

    @Override
    public void onGuiClosed() {
        TaskNHClientCache.getPinConfig()
            .save();
    }
}
