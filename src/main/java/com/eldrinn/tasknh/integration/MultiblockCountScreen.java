package com.eldrinn.tasknh.integration;

import java.util.function.IntConsumer;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.StatCollector;

import org.lwjgl.input.Keyboard;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Asks how many of a multiblock the task is for, then goes back to the screen it was opened from.
 * A plain screen of its own, so NEI and BlockRenderer6343 never see its keys or mouse wheel.
 */
@SideOnly(Side.CLIENT)
public class MultiblockCountScreen extends GuiScreen {

    private static final int MAX_COUNT = 999;

    private final GuiScreen parent;
    private final IntConsumer onConfirm;
    private GuiTextField countField;
    private boolean confirmed = false;

    public MultiblockCountScreen(GuiScreen parent, IntConsumer onConfirm) {
        this.parent = parent;
        this.onConfirm = onConfirm;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        int x = this.width / 2;
        int y = this.height / 2;
        // Resizing the window runs this again, so keep what was typed.
        String text = this.countField == null ? "1" : this.countField.getText();
        this.countField = new GuiTextField(this.fontRendererObj, x - 40, y - 10, 80, 20);
        this.countField.setMaxStringLength(
            String.valueOf(MAX_COUNT)
                .length());
        this.countField.setText(text);
        // Select the text so the first digit typed replaces it.
        this.countField.setCursorPositionEnd();
        this.countField.setSelectionPos(0);
        this.countField.setFocused(true);
        this.buttonList.add(new GuiButton(0, x - 82, y + 20, 80, 20, StatCollector.translateToLocal("gui.done")));
        this.buttonList.add(new GuiButton(1, x + 2, y + 20, 80, 20, StatCollector.translateToLocal("gui.cancel")));
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public void updateScreen() {
        this.countField.updateCursorCounter();
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            this.mc.displayGuiScreen(this.parent);
        } else if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
            confirm();
        } else if (Character.isDigit(typedChar) || typedChar < ' ') {
            // Digits only, plus control keys such as backspace and the arrows.
            this.countField.textboxKeyTyped(typedChar, keyCode);
            // Ctrl+V is a control key too and pastes the clipboard as is.
            String digits = this.countField.getText()
                .replaceAll("\\D", "");
            if (!digits.equals(this.countField.getText())) this.countField.setText(digits);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) {
        super.mouseClicked(mouseX, mouseY, button);
        GuiTextField f = this.countField;
        if (button == 1 && mouseX >= f.xPosition
            && mouseX < f.xPosition + f.width
            && mouseY >= f.yPosition
            && mouseY < f.yPosition + f.height) {
            // Right-click empties the field, like the search fields in NEI.
            f.setText("");
            f.setFocused(true);
            return;
        }
        f.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 0) confirm();
        else this.mc.displayGuiScreen(this.parent);
    }

    private void confirm() {
        int count;
        try {
            count = Integer.parseInt(this.countField.getText());
        } catch (NumberFormatException e) {
            // Empty field: nothing to create yet.
            return;
        }
        // Another mod can cancel the screen change and leave this screen open, so a second Enter must not
        // create the task twice.
        if (count < 1 || this.confirmed) return;
        this.confirmed = true;
        this.onConfirm.accept(count);
        this.mc.displayGuiScreen(this.parent);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawCenteredString(
            this.fontRendererObj,
            StatCollector.translateToLocal("tasknh.gui.multiblock.count"),
            this.width / 2,
            this.height / 2 - 24,
            0xFFFFFF);
        this.countField.drawTextBox();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}
