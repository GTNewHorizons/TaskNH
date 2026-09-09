package com.eldrinn.tasknh.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ICrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;

import com.eldrinn.tasknh.config.TaskNHConfig;
import com.eldrinn.tasknh.data.Task;
import com.eldrinn.tasknh.data.TaskStatus;
import com.eldrinn.tasknh.network.SyncAllTasksPacket;
import com.eldrinn.tasknh.network.TaskNHNetwork;
import com.eldrinn.tasknh.storage.TaskNHWorldData;
import com.gtnewhorizon.gtnhlib.teams.Team;
import com.gtnewhorizon.gtnhlib.teams.TeamManager;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * Completes tasks whose tracked item shows up in a team member's inventory.
 * Inventory changes are reported by a container listener and checked once on the next server tick,
 * so nothing is scanned while inventories stay untouched.
 * Completion is one-way: spending the item afterwards does not reopen the task.
 */
public class ItemTrackHandler {

    private static final Map<UUID, InventoryListener> LISTENERS = new HashMap<>();
    private static final Set<EntityPlayerMP> pending = new LinkedHashSet<>();

    /** Attaches the inventory listener on login, respawn and dimension change. */
    @SubscribeEvent
    public void onEntityJoinWorld(EntityJoinWorldEvent event) {
        if (event.world.isRemote || !(event.entity instanceof EntityPlayerMP player)) return;
        InventoryListener listener = LISTENERS.get(player.getUniqueID());
        if (listener == null) {
            listener = new InventoryListener(player);
            LISTENERS.put(player.getUniqueID(), listener);
        } else {
            listener.player = player;
        }
        // A dimension change reuses the same player and container, so the listener may already be attached.
        // Container.removeCraftingFromCrafters is client only, hence the container is remembered instead.
        if (listener.attachedTo != player.inventoryContainer) {
            player.inventoryContainer.addCraftingToCrafters(listener);
            listener.attachedTo = player.inventoryContainer;
        }
        // Check once on join: the item may have been obtained while offline or in another dimension.
        schedule(player);
    }

    private static void schedule(EntityPlayerMP player) {
        synchronized (pending) {
            pending.add(player);
        }
    }

    /** Drops the listener and any pending check for a player who left. */
    public static void forget(EntityPlayerMP player) {
        LISTENERS.remove(player.getUniqueID());
        synchronized (pending) {
            pending.remove(player);
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        List<EntityPlayerMP> toCheck;
        synchronized (pending) {
            if (pending.isEmpty()) return;
            toCheck = new ArrayList<>(pending);
            pending.clear();
        }
        if (!TaskNHConfig.itemTrackingEnabled) return;

        TaskNHWorldData data = TaskNHWorldData.get();
        for (EntityPlayerMP player : toCheck) {
            checkPlayer(player, data);
        }
    }

    private static void checkPlayer(EntityPlayerMP player, TaskNHWorldData data) {
        Team team = TeamManager.getTeamByPlayer(player.getUniqueID());
        if (team == null) return;

        boolean anyCompleted = false;
        for (Task task : new ArrayList<>(data.getTeamTasks(team.getTeamId()))) {
            if (task.status == TaskStatus.DONE) continue;
            if (task.trackItem == null) continue;
            if (countItem(player, task.trackItem) < Math.max(1, task.trackItemCount)) continue;

            task.status = TaskStatus.DONE;
            data.updateTask(team.getTeamId(), task);
            anyCompleted = true;

            if (TaskNHConfig.announceAutoComplete) {
                for (EntityPlayerMP p : MinecraftServer.getServer()
                    .getConfigurationManager().playerEntityList) {
                    if (team.getMembers()
                        .contains(p.getUniqueID())) {
                        p.addChatMessage(
                            new ChatComponentTranslation(
                                "tasknh.chat.auto_complete",
                                task.title,
                                player.getCommandSenderName()));
                    }
                }
            }
        }

        if (anyCompleted) {
            TaskNHNetwork
                .sendToTeamMembers(team.getMembers(), new SyncAllTasksPacket(data.getTeamTasks(team.getTeamId())));
        }
    }

    /** Counts the tracked item across one player's main inventory. No OreDictionary. */
    private static int countItem(EntityPlayerMP player, ItemStack trackItem) {
        // Reading the name costs a registry lookup for a seed, so the tracked one is read once.
        String trackedName = trackItem.getUnlocalizedName();
        int found = 0;
        for (ItemStack stack : player.inventory.mainInventory) {
            if (stack != null && matches(stack, trackItem, trackedName)) found += stack.stackSize;
        }
        return found;
    }

    /**
     * Item, meta and the stack's own unlocalized name must match. A mod overrides that name when one item
     * carries several different things, so CropsNH seeds of different plants count apart while the growth
     * values they each carry are ignored. A seed nobody analyzed yet is named after no plant, so it stays
     * out of a task asking for a specific one until someone scans it.
     */
    private static boolean matches(ItemStack candidate, ItemStack tracked, String trackedName) {
        return candidate.getItem() == tracked.getItem() && candidate.getItemDamage() == tracked.getItemDamage()
            && candidate.getUnlocalizedName()
                .equals(trackedName);
    }

    /** Marks a player for checking whenever their main inventory changes. */
    private static class InventoryListener implements ICrafting {

        private EntityPlayerMP player;
        private Container attachedTo;

        private InventoryListener(EntityPlayerMP player) {
            this.player = player;
        }

        @Override
        public void sendContainerAndContentsToPlayer(Container container, @SuppressWarnings("rawtypes") List contents) {
            schedule(player);
        }

        @Override
        public void sendSlotContents(Container container, int slot, ItemStack stack) {
            // Main inventory only — skip the crafting grid and armor slots.
            if (slot >= 9 && slot <= 44) schedule(player);
        }

        @Override
        public void sendProgressBarUpdate(Container container, int id, int value) {}
    }
}
