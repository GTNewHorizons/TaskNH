package com.eldrinn.tasknh.network;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;

import com.gtnewhorizon.gtnhlib.network.base.IPacket;
import com.gtnewhorizon.gtnhlib.network.base.NetworkChannel;

public class TaskNHNetwork {

    public static final NetworkChannel CHANNEL = new NetworkChannel("tasknh");

    public static void init() {
        CHANNEL.toClient(new SyncAllTasksPacket());
        CHANNEL.toClient(new OpenGuiPacket());
        CHANNEL.toClient(new SyncTeamMembersPacket());
        CHANNEL.toServer(new CreateTaskPacket());
        CHANNEL.toServer(new UpdateTaskPacket());
        CHANNEL.toServer(new DeleteTaskPacket());
        CHANNEL.toServer(new RemindTaskPacket());
    }

    /**
     * Sends a task edit the server answers with a {@link SyncAllTasksPacket}. The GUI skips its
     * rebuild for that echo, so an open form keeps its focus and layout.
     */
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public static void sendEditToServer(com.eldrinn.tasknh.data.Task task, IPacket packet) {
        com.eldrinn.tasknh.gui.TaskNHGui.expectSelfSync();
        com.eldrinn.tasknh.cache.TaskNHClientCache.putLocal(task);
        CHANNEL.sendToServer(packet);
    }

    /**
     * Sends a task deletion, which the server also answers with a {@link SyncAllTasksPacket}. The
     * pending edit is dropped, otherwise it would put the deleted task back into the cache.
     */
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public static void sendDeleteToServer(java.util.UUID taskId, IPacket packet) {
        com.eldrinn.tasknh.gui.TaskNHGui.expectSelfSync();
        com.eldrinn.tasknh.cache.TaskNHClientCache.removeLocal(taskId);
        CHANNEL.sendToServer(packet);
    }

    /**
     * Sends a packet to all online members of the given team.
     * Members who are offline are skipped silently.
     */
    public static void sendToTeamMembers(Set<UUID> memberUuids, IPacket packet) {
        List<EntityPlayerMP> online = MinecraftServer.getServer()
            .getConfigurationManager().playerEntityList;
        for (EntityPlayerMP player : online) {
            if (memberUuids.contains(player.getUniqueID())) {
                CHANNEL.sendTo(packet, player);
            }
        }
    }
}
