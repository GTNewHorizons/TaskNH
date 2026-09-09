package com.eldrinn.tasknh.gui.widget;

import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.NotNull;

import com.cleanroommc.modularui.api.ITheme;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.Interactable;
import com.cleanroommc.modularui.drawable.GuiDraw;
import com.cleanroommc.modularui.integration.recipeviewer.RecipeViewerGhostIngredientSlot;
import com.cleanroommc.modularui.integration.recipeviewer.RecipeViewerIngredientProvider;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.widget.Widget;
import com.eldrinn.tasknh.gui.ColorUtils;
import com.eldrinn.tasknh.integration.NEIRecipeIntegration;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * A ghost slot for setting a task icon via NEI drag-and-drop.
 * Right-click clears the icon, middle-click reaches the slot's own handler where there is one.
 * With NEI installed, left-click opens the item's recipes and the R and U hotkeys work over the slot.
 */
@SideOnly(Side.CLIENT)
public class IconSlotWidget extends Widget<IconSlotWidget>
    implements RecipeViewerGhostIngredientSlot<ItemStack>, RecipeViewerIngredientProvider, Interactable {

    private final ItemHolder iconHolder;
    private final Runnable onChanged;
    private Runnable onMiddleClick;

    public interface ItemHolder {

        ItemStack get();

        void set(ItemStack iconItem);

        /** Stack size the slot stands for. A slot that only holds an icon keeps the default. */
        default int getCount() {
            return 1;
        }

        default void setCount(int count) {}
    }

    public IconSlotWidget(ItemHolder iconHolder, Runnable onChanged) {
        this(iconHolder, onChanged, "tasknh.gui.detail.icon_hint");
    }

    public IconSlotWidget(ItemHolder iconHolder, Runnable onChanged, String tooltipKey) {
        this.iconHolder = iconHolder;
        this.onChanged = onChanged;
        addTooltipLine(IKey.lang(tooltipKey));
    }

    /** Runs on middle click, for slots that let the player edit the count by hand. */
    public IconSlotWidget onMiddleClick(Runnable action) {
        this.onMiddleClick = action;
        return this;
    }

    @Override
    public boolean handleDragAndDrop(@NotNull ItemStack draggedStack, int button) {
        // A copy of one item, NBT included: the dragged stack belongs to NEI and its size is the amount
        // the player asked for, which the task keeps in a field of its own.
        ItemStack stack = draggedStack.copy();
        stack.stackSize = 1;
        iconHolder.set(stack);
        // NEI hands over the stack size from its own quantity field, see PanelWidget#getDraggedStackWithQuantity.
        iconHolder.setCount(Math.max(1, draggedStack.stackSize));
        onChanged.run();
        return true;
    }

    @Override
    public @NotNull Interactable.Result onMousePressed(int button) {
        if (button == 1) {
            // Both branches rebuild or replace the screen, and a text field only commits its value
            // when it loses focus, so drop the focus first or the edit in progress is lost.
            getContext().removeFocus();
            iconHolder.set(null);
            iconHolder.setCount(1);
            onChanged.run();
            return Interactable.Result.SUCCESS;
        }
        if (button == 2 && this.onMiddleClick != null) {
            // Opening the count field rebuilds the GUI, which drops a text field before it commits.
            getContext().removeFocus();
            this.onMiddleClick.run();
            return Interactable.Result.SUCCESS;
        }
        if (button == 0 && NEIRecipeIntegration.isAvailable()) {
            ItemStack stack = iconHolder.get();
            if (stack != null) {
                getContext().removeFocus();
                // An item with no recipes leaves the screen as it is, so the click stays unhandled.
                if (NEIRecipeIntegration.showRecipes(stack)) return Interactable.Result.SUCCESS;
            }
        }
        return Interactable.Result.IGNORE;
    }

    /** Lets NEI resolve the hovered item, so its R and U hotkeys work over this slot. */
    @Override
    public ItemStack getStackForRecipeViewer() {
        return iconHolder.get();
    }

    @Override
    protected WidgetThemeEntry<?> getWidgetThemeInternal(ITheme theme) {
        return theme.getItemSlotTheme();
    }

    @Override
    public void draw(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
        ItemStack stack = iconHolder.get();
        if (stack == null) {
            // Hint that an NEI item can be dropped here to set a task icon.
            net.minecraft.client.gui.FontRenderer font = net.minecraft.client.Minecraft.getMinecraft().fontRenderer;
            String hint = "+";
            int x = (getArea().width - font.getStringWidth(hint)) / 2;
            int y = (getArea().height - font.FONT_HEIGHT) / 2;
            font.drawString(hint, x, y, ColorUtils.textGray.getColor());
            return;
        }
        int pad = 1;
        GuiDraw.drawItem(
            stack,
            pad,
            pad,
            getArea().width - 2 * pad,
            getArea().height - 2 * pad,
            context.getCurrentDrawingZ());

        // GuiDraw.drawItem skips the vanilla item overlay, so the count is drawn separately. The helper
        // scales the text down when it would not fit, which a four digit count does not.
        GuiDraw.drawStandardSlotAmountText(iconHolder.getCount(), null, getArea());
    }

}
