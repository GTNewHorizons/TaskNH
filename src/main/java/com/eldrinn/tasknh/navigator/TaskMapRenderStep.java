package com.eldrinn.tasknh.navigator;

import java.util.List;

import net.minecraft.client.Minecraft;

import com.eldrinn.tasknh.data.TaskStatus;
import com.eldrinn.tasknh.gui.ColorUtils;
import com.gtnewhorizons.navigator.api.model.steps.UniversalInteractableStep;
import com.gtnewhorizons.navigator.api.util.DrawUtils;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class TaskMapRenderStep extends UniversalInteractableStep<TaskMapLocation> {

    private static final int ICON_SIZE = 16;
    private static final int FONT_HEIGHT = Minecraft.getMinecraft().fontRenderer.FONT_HEIGHT;

    public TaskMapRenderStep(TaskMapLocation location) {
        super(location);
        setFontScale(1.2F);
        setMinScale(1);
    }

    @Override
    public void preRender(double topX, double topY, float drawScale, double zoom) {
        double iconSize = isXaero ? ICON_SIZE * zoom : ICON_SIZE * drawScale * getZoomScale(1, 2, 3, 5);
        setSize(iconSize);
        setOffset(-iconSize / 2);
    }

    @Override
    public void draw(double topX, double topY, float drawScale, double zoom) {
        double labelScale = isXaero ? getFontScale() : getFontScale() * getZoomScale(1, 2.5, 2, 5);
        if (!isMinimap() && getZoomStep() >= 2) {
            DrawUtils.drawLabel(
                location.getTitle(),
                topX + width / 2,
                topY - FONT_HEIGHT * labelScale - 5,
                location.getColor(),
                ColorUtils.BG_HUD.getColor(),
                true,
                labelScale);
        }

        int bgColor = bgColor(location.getStatus());
        DrawUtils.drawRect(topX, topY, width, height, bgColor, 200);
        DrawUtils.drawHollowRect(topX, topY, width, height, ColorUtils.MAP_BORDER.getColor(), 180);
        String letter = statusLetter(location.getStatus());
        double glyphScale = isXaero ? getFontScale() : getFontScale() * getZoomScale(1, 2, 3, 5);
        DrawUtils.drawLabel(
            letter,
            topX + width / 2,
            topY + (height - FONT_HEIGHT * glyphScale) / 2,
            ColorUtils.MAP_TEXT.getColor(),
            ColorUtils.BG_HUD.getColor(),
            true,
            glyphScale);
    }

    @Override
    public boolean isMouseOver(int mouseX, int mouseY) {
        return mouseX >= getX() && mouseX <= getX() + width && mouseY >= getY() && mouseY <= getY() + height;
    }

    @Override
    public void getTooltip(List<String> list) {
        list.add(location.getTitle());
        list.add(
            location.getStatus()
                .displayName());
    }

    private static int bgColor(TaskStatus status) {
        return switch (status) {
            case OPEN -> ColorUtils.MAP_FILL_OPEN.getColor();
            case IN_PROGRESS -> ColorUtils.MAP_FILL_IN_PROGRESS.getColor();
            case DONE -> ColorUtils.MAP_FILL_DONE.getColor();
        };
    }

    private static String statusLetter(TaskStatus status) {
        return switch (status) {
            case OPEN -> "O";
            case IN_PROGRESS -> "~";
            case DONE -> "V";
        };
    }

}
