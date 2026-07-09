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

    private boolean dragging = false;
    private int dragOffsetX; // mouse offset within handle at drag start
    private int dragOffsetY;
    private int dragAnchorX; // anchorX at drag start (constant during drag)
    private int dragAnchorY;

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawRect(0, 0, width, height, ColorUtils.BG_OVERLAY.getColor());

        PinnedTasksConfig cfg = TaskNHClientCache.getPinConfig();
        ScaledResolution res = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        int sw = res.getScaledWidth();
        int sh = res.getScaledHeight();

        String hint = StatCollector.translateToLocal("tasknh.hud.settings.hint");
        fontRendererObj.drawStringWithShadow(
            hint,
            (sw - fontRendererObj.getStringWidth(hint)) / 2,
            6,
            ColorUtils.TEXT_GRAY.getColor());

        int[] pos = hudPos(cfg, sw, sh);
        int hx = pos[0];
        int hy = pos[1];
        drawRect(hx, hy, hx + HANDLE_SIZE, hy + HANDLE_SIZE, ColorUtils.HANDLE.getColor());
        GL11.glColor4f(1f, 1f, 1f, 1f);
        GuiTextures.ALL_DIRECTIONS.draw(hx, hy, HANDLE_SIZE, HANDLE_SIZE);
        GL11.glColor4f(1f, 1f, 1f, 1f);

        drawControlPanel(cfg, sw, sh);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private int[] hudPos(PinnedTasksConfig cfg, int sw, int sh) {
        List<Task> pinned = TaskNHClientCache.getPinnedTasks();
        return HudRenderer.computeHudPosition(cfg, sw, sh, fontRendererObj, pinned);
    }

    private void drawControlPanel(PinnedTasksConfig cfg, int sw, int sh) {
        int panelW = 420;
        int px = (sw - panelW) / 2;
        int py = sh - PANEL_H - PANEL_PADDING;

        drawRect(px - 4, py - 4, px + panelW + 4, py + PANEL_H + 4, ColorUtils.BG_PANEL.getColor());

        int cx = px;

        // Scale
        String scaleKey = StatCollector.translateToLocal("tasknh.hud.settings.scale");
        fontRendererObj.drawStringWithShadow(scaleKey, cx, py + 7, ColorUtils.TEXT_GRAY.getColor());
        cx += fontRendererObj.getStringWidth(scaleKey) + 4;

        drawRect(cx, py + 2, cx + 14, py + 22, ColorUtils.BG_BUTTON.getColor());
        fontRendererObj.drawStringWithShadow("-", cx + 4, py + 7, ColorUtils.TEXT_WHITE.getColor());
        cx += 16;

        String scaleLabel = String.format("%.2fx", cfg.getScale());
        fontRendererObj.drawStringWithShadow(scaleLabel, cx, py + 7, ColorUtils.TEXT_WHITE.getColor());
        cx += fontRendererObj.getStringWidth(scaleLabel) + 4;

        drawRect(cx, py + 2, cx + 14, py + 22, ColorUtils.BG_BUTTON.getColor());
        fontRendererObj.drawStringWithShadow("+", cx + 3, py + 7, ColorUtils.TEXT_WHITE.getColor());
        cx += 20;

        // Subtasks
        String subtasksKey = StatCollector.translateToLocal("tasknh.hud.settings.subtasks");
        fontRendererObj.drawStringWithShadow(subtasksKey, cx, py + 7, ColorUtils.TEXT_GRAY.getColor());
        cx += fontRendererObj.getStringWidth(subtasksKey) + 4;

        drawRect(cx, py + 2, cx + 14, py + 22, ColorUtils.BG_BUTTON.getColor());
        fontRendererObj.drawStringWithShadow("-", cx + 4, py + 7, ColorUtils.TEXT_WHITE.getColor());
        cx += 16;

        String subtasksLabel = String.valueOf(cfg.getMaxSubtasksShown());
        fontRendererObj.drawStringWithShadow(subtasksLabel, cx, py + 7, ColorUtils.TEXT_WHITE.getColor());
        cx += fontRendererObj.getStringWidth(subtasksLabel) + 4;

        drawRect(cx, py + 2, cx + 14, py + 22, ColorUtils.BG_BUTTON.getColor());
        fontRendererObj.drawStringWithShadow("+", cx + 3, py + 7, ColorUtils.TEXT_WHITE.getColor());
        cx += 20;

        // Pins
        String pinsKey = StatCollector.translateToLocal("tasknh.hud.settings.tasks");
        fontRendererObj.drawStringWithShadow(pinsKey, cx, py + 7, ColorUtils.TEXT_GRAY.getColor());
        cx += fontRendererObj.getStringWidth(pinsKey) + 4;

        drawRect(cx, py + 2, cx + 14, py + 22, ColorUtils.BG_BUTTON.getColor());
        fontRendererObj.drawStringWithShadow("-", cx + 4, py + 7, ColorUtils.TEXT_WHITE.getColor());
        cx += 16;

        String pinsLabel = String.valueOf(cfg.getMaxPinnedTasks());
        fontRendererObj.drawStringWithShadow(pinsLabel, cx, py + 7, ColorUtils.TEXT_WHITE.getColor());
        cx += fontRendererObj.getStringWidth(pinsLabel) + 4;

        drawRect(cx, py + 2, cx + 14, py + 22, ColorUtils.BG_BUTTON.getColor());
        fontRendererObj.drawStringWithShadow("+", cx + 3, py + 7, ColorUtils.TEXT_WHITE.getColor());
        cx += 20;

        // BG toggle
        String bgLabel = StatCollector.translateToLocalFormatted(
            "tasknh.hud.settings.bg",
            cfg.isShowBackground() ? StatCollector.translateToLocal("tasknh.hud.on")
                : StatCollector.translateToLocal("tasknh.hud.off"));
        int bgColor = cfg.isShowBackground() ? ColorUtils.GREEN.getColor() : ColorUtils.TEXT_GRAY.getColor();
        drawRect(
            cx,
            py + 2,
            cx + fontRendererObj.getStringWidth(bgLabel) + 8,
            py + 22,
            ColorUtils.BG_BUTTON.getColor());
        fontRendererObj.drawStringWithShadow(bgLabel, cx + 4, py + 7, bgColor);
        cx += fontRendererObj.getStringWidth(bgLabel) + 12;

        // HUD toggle
        String hudLabel = StatCollector.translateToLocalFormatted(
            "tasknh.hud.settings.hud",
            cfg.isHudVisible() ? StatCollector.translateToLocal("tasknh.hud.on")
                : StatCollector.translateToLocal("tasknh.hud.off"));
        int hudColor = cfg.isHudVisible() ? ColorUtils.GREEN.getColor() : ColorUtils.TEXT_GRAY.getColor();
        drawRect(
            cx,
            py + 2,
            cx + fontRendererObj.getStringWidth(hudLabel) + 8,
            py + 22,
            ColorUtils.BG_BUTTON.getColor());
        fontRendererObj.drawStringWithShadow(hudLabel, cx + 4, py + 7, hudColor);
        cx += fontRendererObj.getStringWidth(hudLabel) + 12;

        // Reset
        String resetLabel = StatCollector.translateToLocal("tasknh.hud.settings.reset");
        drawRect(
            cx,
            py + 2,
            cx + fontRendererObj.getStringWidth(resetLabel) + 8,
            py + 22,
            ColorUtils.BG_DANGER.getColor());
        fontRendererObj.drawStringWithShadow(resetLabel, cx + 4, py + 7, ColorUtils.TEXT_WHITE.getColor());
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

        if (mouseX >= hx && mouseX <= hx + HANDLE_SIZE && mouseY >= hy && mouseY <= hy + HANDLE_SIZE) {
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
        int panelW = 420;
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
        // offset = absolute position - anchor (keeps offsetX/Y as delta from anchor)
        cfg.setOffsetXRaw(mouseX - dragOffsetX - dragAnchorX);
        cfg.setOffsetYRaw(mouseY - dragOffsetY - dragAnchorY);
    }

    @Override
    protected void mouseMovedOrUp(int mouseX, int mouseY, int button) {
        if (button == 0 && dragging) {
            dragging = false;
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
        }
    }
}
