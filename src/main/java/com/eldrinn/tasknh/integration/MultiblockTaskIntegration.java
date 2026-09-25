package com.eldrinn.tasknh.integration;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.MinecraftForge;

import com.eldrinn.tasknh.data.ChecklistItem;
import com.eldrinn.tasknh.data.Task;
import com.eldrinn.tasknh.data.TaskStatus;
import com.eldrinn.tasknh.network.CreateTaskPacket;
import com.eldrinn.tasknh.network.TaskNHNetwork;

import blockrenderer6343.client.utils.BRUtil;
import blockrenderer6343.integration.nei.GuiMultiblockHandler;
import blockrenderer6343.integration.nei.MultiblockHandler;
import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.GuiRecipeButton;
import codechicken.nei.recipe.RecipeCatalysts;
import codechicken.nei.recipe.RecipeHandlerRef;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Adds a button to BlockRenderer6343's multiblock preview in NEI that turns the shown structure into a task:
 * the multiblock's name, its controller as the icon, and a tracked checklist item per part.
 * References NEI and BlockRenderer6343 classes directly, so the caller checks both mods are loaded first.
 */
@SideOnly(Side.CLIENT)
public final class MultiblockTaskIntegration {

    private static final Field LAYER_INDEX = findLayerIndex();

    private MultiblockTaskIntegration() {}

    public static void register() {
        MinecraftForge.EVENT_BUS.register(new MultiblockTaskIntegration());
    }

    // LOW: BlockRenderer6343 empties this list for its own handler at the default priority, which would
    // drop the button if it were added first.
    @SubscribeEvent(priority = EventPriority.LOW)
    public void onRecipeButtons(GuiRecipeButton.UpdateRecipeButtonsEvent.Post event) {
        RecipeHandlerRef ref = event.recipeWidget.getRecipeHandlerRef();
        if (!(ref.handler instanceof MultiblockHandler)) return;
        // Top right corner of the recipe, clear of the preview's own buttons along the bottom.
        int x = Math.min(166, event.recipeWidget.w) - GuiRecipeButton.BUTTON_WIDTH;
        event.buttonList.add(new AddTaskButton(ref, x, 0));
    }

    /**
     * BlockRenderer6343 publishes the preview's parts as the handler's catalysts, the column beside the
     * preview, with their counts for the tier and channels picked on screen. NEI sorts them so the
     * controller comes first.
     */
    private static void createTask(MultiblockHandler handler) {
        if (isSingleLayer()) {
            Minecraft.getMinecraft().thePlayer
                .addChatMessage(new ChatComponentTranslation("tasknh.chat.multiblock_single_layer"));
            return;
        }
        List<PositionedStack> parts = RecipeCatalysts.getRecipeCatalysts(handler);
        if (parts.isEmpty()) return;

        String title = handler.getFullRecipeName();
        Task task = new Task(UUID.randomUUID(), title, "", TaskStatus.OPEN);
        task.iconItem = singleItem(parts.get(0).item);
        for (PositionedStack part : parts) {
            ItemStack stack = part.item;
            if (stack == null) continue;
            // Any tier of hatch fits its slot, so tracking the one the preview shows would miss the others.
            if (BRUtil.hatchFilter.test(stack)) continue;
            ChecklistItem item = new ChecklistItem(
                UUID.randomUUID(),
                stack.stackSize + "x " + stack.getDisplayName(),
                false);
            item.trackItem = singleItem(stack);
            // Parts past the tracking cap keep their full count in the title.
            item.trackItemCount = Task.clampTrackItemCount(stack.stackSize);
            task.checklist.add(item);
        }

        TaskNHNetwork.sendEditToServer(task, new CreateTaskPacket(task));
        Minecraft.getMinecraft().thePlayer
            .addChatMessage(new ChatComponentTranslation("tasknh.chat.multiblock_created", title));
    }

    /**
     * The preview's Layer control narrows the part list to one layer, which would leave the task short and
     * could drop the controller. BlockRenderer6343 keeps the picked layer in a protected field, -1 for all.
     */
    private static boolean isSingleLayer() {
        // A newer BlockRenderer6343 without the field keeps the button working rather than dead.
        if (LAYER_INDEX == null) return false;
        try {
            return LAYER_INDEX.getInt(null) > -1;
        } catch (IllegalAccessException e) {
            return false;
        }
    }

    private static Field findLayerIndex() {
        try {
            Field field = GuiMultiblockHandler.class.getDeclaredField("layerIndex");
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException e) {
            return null;
        }
    }

    private static ItemStack singleItem(ItemStack stack) {
        if (stack == null) return null;
        ItemStack copy = stack.copy();
        copy.stackSize = 1;
        return copy;
    }

    private static final class AddTaskButton extends GuiRecipeButton {

        // Clear of the ids NEI gives its own recipe buttons.
        private static final int BUTTON_ID_START = 200;

        private AddTaskButton(RecipeHandlerRef ref, int x, int y) {
            super(ref, x, y, BUTTON_ID_START + ref.recipeIndex, "+");
        }

        @Override
        public List<String> handleTooltip(List<String> currenttip) {
            currenttip.add(StatCollector.translateToLocal("tasknh.gui.multiblock.add"));
            if (isSingleLayer()) {
                currenttip.add(StatCollector.translateToLocal("tasknh.gui.multiblock.single_layer"));
            }
            return currenttip;
        }

        @Override
        public Map<String, String> handleHotkeys(int mousex, int mousey, Map<String, String> hotkeys) {
            return hotkeys;
        }

        @Override
        public void lastKeyTyped(char keyChar, int keyID) {}

        @Override
        public void drawItemOverlay() {}

        @Override
        public void mouseReleased(int mouseX, int mouseY) {
            createTask((MultiblockHandler) handlerRef.handler);
        }
    }
}
