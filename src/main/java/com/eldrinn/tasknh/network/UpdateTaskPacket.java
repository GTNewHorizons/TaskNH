package com.eldrinn.tasknh.network;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentTranslation;

import com.eldrinn.tasknh.data.AssignedPlayer;
import com.eldrinn.tasknh.data.Task;
import com.eldrinn.tasknh.data.TaskStatus;
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

        // Decided here rather than on the client, so item tracking checking the last box closes the task
        // through the same path. Runs before the reorder below, which reads the status the task ends up with.
        if (task.status != TaskStatus.DONE && task.shouldCompleteOnChecklist()) {
            task.status = TaskStatus.DONE;
        }

        boolean reparented = !Objects.equals(oldTask.parentId, task.parentId);
        if (reparented && !canNest(data, team.getTeamId(), task)) {
            task.parentId = oldTask.parentId;
            reparented = false;
        }

        if (oldTask.status != task.status || reparented) data.moveToEnd(team.getTeamId(), task);
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

    /** Whether the task may sit under its new parent. A task with no parent always may. */
    private static boolean canNest(TaskNHWorldData data, UUID teamId, Task task) {
        if (task.parentId == null) return true;
        // Nesting is one level deep: the parent must be a root, and the task must not have subtasks of its own.
        Task parent = data.getTask(teamId, task.parentId);
        if (parent == null || parent.id.equals(task.id) || parent.parentId != null) return false;
        for (Task other : data.getTeamTasks(teamId)) {
            if (task.id.equals(other.parentId)) return false;
        }
        return true;
    }
}
