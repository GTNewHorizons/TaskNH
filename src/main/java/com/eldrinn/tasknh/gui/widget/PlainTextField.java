package com.eldrinn.tasknh.gui.widget;

import org.lwjgl.input.Keyboard;

import com.cleanroommc.modularui.api.widget.Interactable;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * TextFieldWidget without right-click-to-clear unless {@link #rightClickClears()} turns it on. Search bar uses
 * stock TextFieldWidget instead.
 */
@SideOnly(Side.CLIENT)
public class PlainTextField extends TextFieldWidget {

    /** Matches the shortest string cap the task packets read back, so no field can build an unsendable packet. */
    public static final int DEFAULT_MAX_LENGTH = 256;

    private Runnable onEnter;
    private boolean rightClickClears = false;

    public PlainTextField() {
        setMaxLength(DEFAULT_MAX_LENGTH);
    }

    /** Lets right-click empty the field. Off by default, so a stray click can't wipe a title or description. */
    public PlainTextField rightClickClears() {
        this.rightClickClears = true;
        return this;
    }

    /** Fires when Enter is pressed while the field is focused. */
    public PlainTextField onEnter(Runnable onEnter) {
        this.onEnter = onEnter;
        return this;
    }

    /** Empties the field and moves the cursor with it, unlike setText, which leaves it out of bounds. */
    public void clearText() {
        this.handler.clear();
    }

    @Override
    public @org.jetbrains.annotations.NotNull Interactable.Result onMousePressed(int mouseButton) {
        if (mouseButton == 1 && !this.rightClickClears) {
            // skip clear — right-click only clears the search bar and fields that opt in
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
