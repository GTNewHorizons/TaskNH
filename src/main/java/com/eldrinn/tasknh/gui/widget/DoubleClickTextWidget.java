package com.eldrinn.tasknh.gui.widget;

import net.minecraft.client.Minecraft;

import org.jetbrains.annotations.NotNull;

import com.cleanroommc.modularui.api.widget.Interactable;
import com.cleanroommc.modularui.widgets.TextWidget;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** A text label that runs an action when double-clicked with the left button. */
@SideOnly(Side.CLIENT)
public class DoubleClickTextWidget extends TextWidget<DoubleClickTextWidget> implements Interactable {

    // Same window the text fields use for their own double-click.
    private static final int DOUBLE_CLICK_MS = 300;

    private final Runnable onDoubleClick;
    private long lastClickTime = 0;

    public DoubleClickTextWidget(String text, Runnable onDoubleClick) {
        super(text);
        this.onDoubleClick = onDoubleClick;
    }

    @Override
    public @NotNull Interactable.Result onMousePressed(int button) {
        if (button != 0) return Interactable.Result.IGNORE;
        long now = Minecraft.getSystemTime();
        if (now - this.lastClickTime < DOUBLE_CLICK_MS) {
            this.lastClickTime = 0;
            // The action rebuilds the GUI, which drops a text field before it commits on focus loss.
            getContext().removeFocus();
            this.onDoubleClick.run();
        } else {
            this.lastClickTime = now;
        }
        return Interactable.Result.SUCCESS;
    }
}
