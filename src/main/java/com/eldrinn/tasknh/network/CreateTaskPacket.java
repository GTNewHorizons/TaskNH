package com.eldrinn.tasknh.network;

import java.io.IOException;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.PacketBuffer;

import com.eldrinn.tasknh.data.Task;
import com.eldrinn.tasknh.storage.TaskNHWorldData;
import com.gtnewhorizon.gtnhlib.network.base.IPacket;
import com.gtnewhorizon.gtnhlib.teams.Team;
import com.gtnewhorizon.gtnhlib.teams.TeamManager;

public class CreateTaskPacket implements IPacket {

    private Task task;

    public CreateTaskPacket() {}

    public CreateTaskPacket(Task task) {
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
        if (data.getTask(team.getTeamId(), task.id) != null) return null; // duplicate
        data.addTask(team.getTeamId(), task);
        TaskNHNetwork.sendToTeamMembers(team.getMembers(), new SyncAllTasksPacket(data.getTeamTasks(team.getTeamId())));
        return null;
    }
}
