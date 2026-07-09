package com.eldrinn.tasknh.network;

import java.io.IOException;
import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentTranslation;

import com.eldrinn.tasknh.data.AssignedPlayer;
import com.eldrinn.tasknh.data.Task;
import com.eldrinn.tasknh.storage.TaskNHWorldData;
import com.gtnewhorizon.gtnhlib.network.base.IPacket;
import com.gtnewhorizon.gtnhlib.teams.Team;
import com.gtnewhorizon.gtnhlib.teams.TeamManager;

public class UpdateTaskPacket implements IPacket {

    private Task task;

    public UpdateTaskPacket() {}

    public UpdateTaskPacket(Task task) {
        this.task = task;
    }

    @Override
    public void encode(PacketBuffer buf) throws IOException {
        task.writeToBuf(buf);
    }

    @Override
    public void decode(PacketBuffer buf) throws IOException {
        task = Task.readFromBuf(buf);
    }

    @Override
    public IPacket executeServer(NetHandlerPlayServer handler) {
        EntityPlayerMP sender = handler.playerEntity;
        Team team = TeamManager.getTeamByPlayer(sender.getUniqueID());
        if (team == null) return null;

        TaskNHWorldData data = TaskNHWorldData.get();
        Task oldTask = data.getTask(team.getTeamId(), task.id);
        if (oldTask == null) return null; // unknown task
        data.updateTask(team.getTeamId(), task);

        // Notify players who are newly assigned to this task
        List<EntityPlayerMP> online = MinecraftServer.getServer()
            .getConfigurationManager().playerEntityList;
        for (AssignedPlayer ap : task.assignees) {
            boolean wasAssigned = oldTask.assignees.stream()
                .anyMatch(
                    old -> old.playerId()
                        .equals(ap.playerId()));
            if (!wasAssigned) {
                for (EntityPlayerMP p : online) {
                    if (ap.playerId()
                        .equals(p.getUniqueID())) {
                        p.addChatMessage(new ChatComponentTranslation("tasknh.chat.assigned", task.title));
                        break;
                    }
                }
            }
        }

        TaskNHNetwork.sendToTeamMembers(team.getMembers(), new SyncAllTasksPacket(data.getTeamTasks(team.getTeamId())));
        return null;
    }
}
