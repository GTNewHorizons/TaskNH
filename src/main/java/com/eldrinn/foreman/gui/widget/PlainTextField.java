package com.eldrinn.foreman.gui.widget;

import org.lwjgl.input.Keyboard;

import com.cleanroommc.modularui.api.widget.Interactable;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** TextFieldWidget without right-click-to-clear. Search bar uses stock TextFieldWidget instead. */
@SideOnly(Side.CLIENT)
public class PlainTextField extends TextFieldWidget {

    private Runnable onEnter;

    /** Fires when Enter is pressed while the field is focused. */
    public PlainTextField onEnter(Runnable onEnter) {
        this.onEnter = onEnter;
        return this;
    }

    @Override
    public @org.jetbrains.annotations.NotNull Interactable.Result onMousePressed(int mouseButton) {
        if (mouseButton == 1) {
            // skip clear — right-click is reserved for the search bar only
            return Interactable.Result.IGNORE;
        }
        return super.onMousePressed(mouseButton);
    }

    @Override
    public @org.jetbrains.annotations.NotNull Interactable.Result onKeyPressed(char typedChar, int keyCode) {
        if (onEnter != null && (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER)) {
            onEnter.run();
            return Interactable.Result.SUCCESS;
        }
        return super.onKeyPressed(typedChar, keyCode);
    }
}
