package com.eldrinn.tasknh.gui.widget;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.NotNull;

import com.cleanroommc.modularui.api.ITheme;
import com.cleanroommc.modularui.api.widget.Interactable;
import com.cleanroommc.modularui.drawable.GuiDraw;
import com.cleanroommc.modularui.integration.recipeviewer.RecipeViewerGhostIngredientSlot;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.widget.Widget;
import com.eldrinn.tasknh.gui.ColorUtils;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * A ghost slot for setting a task icon via NEI drag-and-drop.
 * Right-click clears the icon.
 */
@SideOnly(Side.CLIENT)
public class IconSlotWidget extends Widget<IconSlotWidget>
    implements RecipeViewerGhostIngredientSlot<ItemStack>, Interactable {

    private final ItemHolder iconHolder;
    private final Runnable onChanged;
    private Runnable onMiddleClick;

    public interface ItemHolder {

        String get();

        void set(String iconItem);

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
        tooltip().addLine(net.minecraft.util.StatCollector.translateToLocal(tooltipKey));
    }

    /** Runs on middle click, for slots that let the player edit the count by hand. */
    public IconSlotWidget onMiddleClick(Runnable action) {
        this.onMiddleClick = action;
        return this;
    }

    @Override
    public boolean handleDragAndDrop(@NotNull ItemStack draggedStack, int button) {
        iconHolder.set(Item.itemRegistry.getNameForObject(draggedStack.getItem()) + ":" + draggedStack.getItemDamage());
        // NEI hands over the stack size from its own quantity field, see PanelWidget#getDraggedStackWithQuantity.
        iconHolder.setCount(Math.max(1, draggedStack.stackSize));
        onChanged.run();
        return true;
    }

    @Override
    public @NotNull Interactable.Result onMousePressed(int button) {
        if (button == 1) {
            iconHolder.set(null);
            iconHolder.setCount(1);
            onChanged.run();
            return Interactable.Result.SUCCESS;
        }
        if (button == 2 && this.onMiddleClick != null) {
            this.onMiddleClick.run();
            return Interactable.Result.SUCCESS;
        }
        return Interactable.Result.IGNORE;
    }

    @Override
    protected WidgetThemeEntry<?> getWidgetThemeInternal(ITheme theme) {
        return theme.getItemSlotTheme();
    }

    @Override
    public void draw(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
        String iconItem = iconHolder.get();
        if (iconItem == null || iconItem.isEmpty()) {
            // Hint that an NEI item can be dropped here to set a task icon.
            net.minecraft.client.gui.FontRenderer font = net.minecraft.client.Minecraft.getMinecraft().fontRenderer;
            String hint = "+";
            int x = (getArea().width - font.getStringWidth(hint)) / 2;
            int y = (getArea().height - font.FONT_HEIGHT) / 2;
            font.drawString(hint, x, y, ColorUtils.textGray.getColor());
            return;
        }
        ItemStack stack = parseIconItem(iconItem);
        if (stack == null) return;
        int pad = 1;
        GuiDraw.drawItem(
            stack,
            pad,
            pad,
            getArea().width - 2 * pad,
            getArea().height - 2 * pad,
            context.getCurrentDrawingZ());

        int count = iconHolder.getCount();
        if (count > 1) {
            // GuiDraw.drawItem skips the vanilla item overlay, so the count is drawn here.
            net.minecraft.client.gui.FontRenderer font = net.minecraft.client.Minecraft.getMinecraft().fontRenderer;
            String text = String.valueOf(count);
            font.drawStringWithShadow(
                text,
                getArea().width - pad - font.getStringWidth(text),
                getArea().height - pad - font.FONT_HEIGHT,
                ColorUtils.textWhite.getColor());
        }
    }

    public static ItemStack parseIconItem(String iconItem) {
        if (iconItem == null || iconItem.isEmpty()) return null;
        String[] parts = iconItem.split(":");
        if (parts.length < 3) return null;
        String id = parts[0] + ":" + parts[1];
        int meta;
        try {
            meta = Integer.parseInt(parts[2]);
        } catch (NumberFormatException e) {
            return null;
        }
        Item item = (Item) Item.itemRegistry.getObject(id);
        return item != null ? new ItemStack(item, 1, meta) : null;
    }
}
