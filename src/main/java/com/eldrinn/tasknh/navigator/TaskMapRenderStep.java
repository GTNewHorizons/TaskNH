package com.eldrinn.tasknh.navigator;

import java.util.List;

import com.eldrinn.tasknh.data.TaskStatus;
import com.eldrinn.tasknh.gui.ColorUtils;
import com.gtnewhorizons.navigator.api.model.steps.UniversalInteractableStep;
import com.gtnewhorizons.navigator.api.util.DrawUtils;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class TaskMapRenderStep extends UniversalInteractableStep<TaskMapLocation> {

    public TaskMapRenderStep(TaskMapLocation location) {
        super(location);
        width = 16;
        height = 16;
    }

    @Override
    public void draw(double topX, double topY, float drawScale, double zoom) {
        int bgColor = bgColor(location.getStatus());
        DrawUtils.drawRect(topX, topY, getAdjustedWidth(), getAdjustedHeight(), bgColor, 200);
        DrawUtils
            .drawHollowRect(topX, topY, getAdjustedWidth(), getAdjustedHeight(), ColorUtils.MAP_BORDER.getColor(), 180);
        String letter = statusLetter(location.getStatus());
        DrawUtils.drawLabel(
            letter,
            topX + getAdjustedWidth() / 2,
            topY + getAdjustedHeight() / 2,
            ColorUtils.MAP_TEXT.getColor(),
            ColorUtils.BG_HUD.getColor(),
            false,
            fontScale);
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
