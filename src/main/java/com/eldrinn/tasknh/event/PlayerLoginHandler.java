package com.eldrinn.tasknh.event;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.event.ClickEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.common.UsernameCache;

import com.eldrinn.tasknh.cache.PlayerEntry;
import com.eldrinn.tasknh.data.AssignedPlayer;
import com.eldrinn.tasknh.data.Task;
import com.eldrinn.tasknh.network.SyncAllTasksPacket;
import com.eldrinn.tasknh.network.SyncTeamMembersPacket;
import com.eldrinn.tasknh.network.TaskNHNetwork;
import com.eldrinn.tasknh.storage.TaskNHWorldData;
import com.gtnewhorizon.gtnhlib.teams.Team;
import com.gtnewhorizon.gtnhlib.teams.TeamManager;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;

public class PlayerLoginHandler {

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.player instanceof EntityPlayerMP player)) return;

        Team team = TeamManager.getTeamByPlayer(player.getUniqueID());
        if (team == null) return;

        TaskNHWorldData data = TaskNHWorldData.get();
        TaskNHNetwork.CHANNEL.sendTo(new SyncAllTasksPacket(data.getTeamTasks(team.getTeamId())), player);
        TaskNHNetwork.CHANNEL.sendTo(buildTeamMembersPacket(team), player);
        sendLoginNotifications(player, team, data);
    }

    private static SyncTeamMembersPacket buildTeamMembersPacket(Team team) {
        List<PlayerEntry> entries = new ArrayList<>();

        Map<UUID, String> onlineNames = new LinkedHashMap<>();
        for (EntityPlayerMP p : MinecraftServer.getServer()
            .getConfigurationManager().playerEntityList) {
            onlineNames.put(p.getUniqueID(), p.getCommandSenderName());
        }

        for (UUID memberId : team.getMembers()) {
            String name;
            if (onlineNames.containsKey(memberId)) {
                name = onlineNames.get(memberId);
            } else {
                String cached = UsernameCache.getLastKnownUsername(memberId);
                name = cached != null ? cached
                    : memberId.toString()
                        .substring(0, 8);
            }
            entries.add(new PlayerEntry(memberId, name));
        }
        return new SyncTeamMembersPacket(entries);
    }

    private static void sendLoginNotifications(EntityPlayerMP player, Team team, TaskNHWorldData data) {
        long lastSeen = data.getPlayerLastSeen(player.getUniqueID());
        UUID playerId = player.getUniqueID();

        List<Task> newTasks = new ArrayList<>();
        for (Task task : data.getTeamTasks(team.getTeamId())) {
            for (AssignedPlayer ap : task.assignees) {
                if (ap.playerId()
                    .equals(playerId) && ap.assignedAt() > lastSeen) {
                    newTasks.add(task);
                    break;
                }
            }
        }

        if (newTasks.isEmpty()) return;

        if (newTasks.size() > 1) {
            player.addChatMessage(new ChatComponentTranslation("tasknh.chat.login.new_tasks", newTasks.size()));
        }
        for (Task task : newTasks) {
            player.addChatMessage(buildTaskLink("tasknh.chat.login.task", task.title, task.id));
        }
    }

    public static IChatComponent buildTaskLink(String translationKey, String taskTitle, UUID taskId) {
        IChatComponent base = new ChatComponentTranslation(translationKey, taskTitle);
        IChatComponent open = new net.minecraft.util.ChatComponentText(" [Open]");
        open.getChatStyle()
            .setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tasknh open " + taskId));
        open.getChatStyle()
            .setColor(EnumChatFormatting.AQUA);
        base.appendSibling(open);
        return base;
    }
}
