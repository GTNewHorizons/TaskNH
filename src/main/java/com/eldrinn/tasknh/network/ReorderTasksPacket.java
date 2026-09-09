package com.eldrinn.tasknh.network;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.PacketBuffer;

import com.eldrinn.tasknh.data.Task;
import com.eldrinn.tasknh.storage.TaskNHWorldData;
import com.gtnewhorizon.gtnhlib.network.base.IPacket;
import com.gtnewhorizon.gtnhlib.teams.Team;
import com.gtnewhorizon.gtnhlib.teams.TeamManager;

/**
 * Carries one finished reorder: the tasks of a single group, in the order the player left them.
 * The whole gesture is one packet and one sync back, so dragging a task across a long list does not
 * flood the team with a task list per intermediate swap.
 */
public class ReorderTasksPacket implements IPacket {

    /** Sanity cap, so a malformed packet cannot make the server allocate an arbitrary list. */
    private static final int MAX_TASKS = 2000;

    private List<UUID> orderedIds;

    public ReorderTasksPacket() {}

    public ReorderTasksPacket(List<UUID> orderedIds) {
        this.orderedIds = orderedIds;
    }

    @Override
    public void encode(PacketBuffer buf) throws IOException {
        buf.writeInt(orderedIds.size());
        for (UUID id : orderedIds) {
            buf.writeLong(id.getMostSignificantBits());
            buf.writeLong(id.getLeastSignificantBits());
        }
    }

    @Override
    public void decode(PacketBuffer buf) throws IOException {
        int count = buf.readInt();
        if (count < 0 || count > MAX_TASKS) throw new IOException("Invalid task count: " + count);
        orderedIds = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            orderedIds.add(new UUID(buf.readLong(), buf.readLong()));
        }
    }

    @Override
    public IPacket executeServer(NetHandlerPlayServer handler) {
        EntityPlayerMP sender = handler.playerEntity;
        Team team = TeamManager.getTeamByPlayer(sender.getUniqueID());
        if (team == null) return null;

        TaskNHWorldData data = TaskNHWorldData.get();
        boolean changed = false;
        for (int i = 0; i < orderedIds.size(); i++) {
            Task task = data.getTask(team.getTeamId(), orderedIds.get(i));
            // A task another member deleted meanwhile just leaves a gap in the numbering.
            if (task == null || task.order == i) continue;
            task.order = i;
            data.updateTask(team.getTeamId(), task);
            changed = true;
        }

        if (changed) {
            TaskNHNetwork
                .sendToTeamMembers(team.getMembers(), new SyncAllTasksPacket(data.getTeamTasks(team.getTeamId())));
        }
        return null;
    }
}
