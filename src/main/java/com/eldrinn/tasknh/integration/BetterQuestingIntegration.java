package com.eldrinn.tasknh.integration;

import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;

import com.eldrinn.tasknh.data.ChecklistItem;
import com.eldrinn.tasknh.data.Task;
import com.eldrinn.tasknh.data.TaskStatus;
import com.eldrinn.tasknh.network.CreateTaskPacket;
import com.eldrinn.tasknh.network.TaskNHNetwork;

import betterquesting.api.properties.NativeProps;
import betterquesting.api.questing.IQuest;
import betterquesting.api.questing.tasks.ITask;
import betterquesting.api.utils.BigItemStack;
import betterquesting.api2.client.gui.context.IQuestContextMenuEntry;
import betterquesting.api2.client.gui.context.QuestContextMenuRegistry;
import betterquesting.api2.storage.DBEntry;
import bq_standard.tasks.TaskRetrieval;

/**
 * Registers TaskNH's entry in BetterQuesting's quest context menu.
 * Only loaded when the BetterQuesting context menu API is present (guarded by BQIntegration.isAvailable).
 */
public final class BetterQuestingIntegration {

    private BetterQuestingIntegration() {}

    public static void register() {
        QuestContextMenuRegistry.register(new IQuestContextMenuEntry() {

            @Override
            public String getLabel(UUID questId, IQuest quest) {
                return "Add to TaskNH";
            }

            @Override
            public Runnable getAction(UUID questId, IQuest quest) {
                return () -> createTaskFromQuest(quest);
            }
        });
    }

    private static void createTaskFromQuest(IQuest quest) {
        String title = quest.getProperty(NativeProps.NAME);
        Task task = new Task(UUID.randomUUID(), title, "", TaskStatus.OPEN);
        task.iconItem = toIconStack(quest.getProperty(NativeProps.ICON));

        // Map TaskRetrieval required items to checklist items that check themselves once a member carries them
        for (DBEntry<ITask> entry : quest.getTasks()
            .getEntries()) {
            if (entry.getValue() instanceof TaskRetrieval retrieval) {
                for (BigItemStack required : retrieval.requiredItems) {
                    String itemName = required.getBaseStack()
                        .getDisplayName();
                    ChecklistItem item = new ChecklistItem(
                        UUID.randomUUID(),
                        required.stackSize + "x " + itemName,
                        false);
                    item.trackItem = toIconStack(required);
                    item.trackItemCount = Task.clampTrackItemCount(required.stackSize);
                    // A quest asking for a tag accepts any item under it, so the item does too.
                    if (required.hasOreDict()) item.trackOre = required.getOreDict();
                    task.checklist.add(item);
                }
            }
        }

        TaskNHNetwork.sendEditToServer(task, new CreateTaskPacket(task));

        Minecraft.getMinecraft().thePlayer
            .addChatMessage(new ChatComponentText("§aTaskNH: task \"" + title + "\" created."));
    }

    /** Returns a single item copy of a quest stack, NBT included, or null when there is none. */
    private static ItemStack toIconStack(BigItemStack iconStack) {
        if (iconStack == null) return null;
        ItemStack base = iconStack.getBaseStack();
        if (base == null || base.getItem() == null) return null;
        ItemStack copy = base.copy();
        copy.stackSize = 1;
        return copy;
    }
}
