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

    public interface ItemHolder {

        String get();

        void set(String iconItem);
    }

    public IconSlotWidget(ItemHolder iconHolder, Runnable onChanged) {
        this.iconHolder = iconHolder;
        this.onChanged = onChanged;
        tooltip().addLine(net.minecraft.util.StatCollector.translateToLocal("tasknh.gui.detail.icon_hint"));
    }

    @Override
    public boolean handleDragAndDrop(@NotNull ItemStack draggedStack, int button) {
        iconHolder.set(Item.itemRegistry.getNameForObject(draggedStack.getItem()) + ":" + draggedStack.getItemDamage());
        onChanged.run();
        return true;
    }

    @Override
    public @NotNull Interactable.Result onMousePressed(int button) {
        if (button == 1) {
            iconHolder.set(null);
            onChanged.run();
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
            font.drawString(hint, x, y, com.eldrinn.tasknh.gui.ColorUtils.TEXT_GRAY.getColor());
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
