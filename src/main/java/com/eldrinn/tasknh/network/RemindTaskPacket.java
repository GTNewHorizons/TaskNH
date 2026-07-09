package com.eldrinn.tasknh.network;

import java.util.UUID;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentTranslation;

import com.eldrinn.tasknh.data.Task;
import com.eldrinn.tasknh.event.PlayerLoginHandler;
import com.eldrinn.tasknh.proxy.CommonProxy;
import com.eldrinn.tasknh.storage.TaskNHWorldData;
import com.gtnewhorizon.gtnhlib.network.base.IPacket;
import com.gtnewhorizon.gtnhlib.teams.Team;
import com.gtnewhorizon.gtnhlib.teams.TeamManager;

public class RemindTaskPacket implements IPacket {

    private UUID taskId;
    private UUID targetPlayerId;

    public RemindTaskPacket() {}

    public RemindTaskPacket(UUID taskId, UUID targetPlayerId) {
        this.taskId = taskId;
        this.targetPlayerId = targetPlayerId;
    }

    @Override
    public void encode(PacketBuffer buf) {
        buf.writeLong(taskId.getMostSignificantBits());
        buf.writeLong(taskId.getLeastSignificantBits());
        buf.writeLong(targetPlayerId.getMostSignificantBits());
        buf.writeLong(targetPlayerId.getLeastSignificantBits());
    }

    @Override
    public void decode(PacketBuffer buf) {
        taskId = new UUID(buf.readLong(), buf.readLong());
        targetPlayerId = new UUID(buf.readLong(), buf.readLong());
    }

    @Override
    public IPacket executeServer(NetHandlerPlayServer handler) {
        EntityPlayerMP sender = handler.playerEntity;
        Team team = TeamManager.getTeamByPlayer(sender.getUniqueID());
        if (team == null) return null;

        TaskNHWorldData data = TaskNHWorldData.get();
        Task task = data.getTask(team.getTeamId(), taskId);
        if (task == null) return null;

        String cooldownKey = taskId + ":" + targetPlayerId;
        long now = System.currentTimeMillis();
        Long lastRemind = CommonProxy.remindCooldowns.get(cooldownKey);
        if (lastRemind != null && now - lastRemind < 60_000L) {
            sender.addChatMessage(new ChatComponentTranslation("tasknh.chat.remind.cooldown"));
            return null;
        }

        EntityPlayerMP target = null;
        for (EntityPlayerMP p : MinecraftServer.getServer()
            .getConfigurationManager().playerEntityList) {
            if (targetPlayerId.equals(p.getUniqueID())) {
                target = p;
                break;
            }
        }
        if (target == null) {
            sender.addChatMessage(new ChatComponentTranslation("tasknh.chat.remind.offline"));
            return null;
        }

        CommonProxy.remindCooldowns.put(cooldownKey, now);
        target.addChatMessage(new ChatComponentTranslation("tasknh.chat.remind.prefix", sender.getCommandSenderName()));
        target.addChatMessage(PlayerLoginHandler.buildTaskLink("tasknh.chat.remind.message", task.title, taskId));
        return null;
    }
}
