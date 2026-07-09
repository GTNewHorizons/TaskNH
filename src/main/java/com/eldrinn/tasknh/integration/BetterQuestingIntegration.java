package com.eldrinn.tasknh.integration;

import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;

import com.eldrinn.tasknh.data.Subtask;
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
 * Only loaded when BetterQuesting is present (guarded by Loader.isModLoaded in ClientProxy).
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
        task.iconItem = toIconString(quest.getProperty(NativeProps.ICON));

        // Map TaskRetrieval required items to subtasks
        for (DBEntry<ITask> entry : quest.getTasks()
            .getEntries()) {
            if (entry.getValue() instanceof TaskRetrieval retrieval) {
                for (BigItemStack required : retrieval.requiredItems) {
                    String itemName = required.getBaseStack()
                        .getDisplayName();
                    task.subtasks.add(new Subtask(UUID.randomUUID(), required.stackSize + "x " + itemName, false));
                }
            }
        }

        TaskNHNetwork.CHANNEL.sendToServer(new CreateTaskPacket(task));

        Minecraft.getMinecraft().thePlayer
            .addChatMessage(new ChatComponentText("§aTaskNH: task \"" + title + "\" created."));
    }

    /** Returns "modid:itemname:meta" for the given BigItemStack, or null if unavailable. */
    private static String toIconString(BigItemStack iconStack) {
        if (iconStack == null) return null;
        ItemStack base = iconStack.getBaseStack();
        if (base.getItem() == null) return null;
        String itemName = Item.itemRegistry.getNameForObject(base.getItem());
        return itemName != null ? itemName + ":" + base.getItemDamage() : null;
    }
}
