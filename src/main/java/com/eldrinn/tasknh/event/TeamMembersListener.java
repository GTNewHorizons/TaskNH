package com.eldrinn.tasknh.event;

import com.eldrinn.tasknh.network.SyncAllTasksPacket;
import com.eldrinn.tasknh.network.TaskNHNetwork;
import com.eldrinn.tasknh.storage.TaskNHWorldData;
import com.gtnewhorizon.gtnhlib.teams.Team;
import com.gtnewhorizon.gtnhlib.teams.TeamEvents.TeamDisbandEvent;
import com.gtnewhorizon.gtnhlib.teams.TeamEvents.TeamJoinEvent;
import com.gtnewhorizon.gtnhlib.teams.TeamEvents.TeamKickEvent;
import com.gtnewhorizon.gtnhlib.teams.TeamEvents.TeamLeaveEvent;
import com.gtnewhorizon.gtnhlib.teams.TeamManager;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/** Sends members and tasks to the online members of every team whose membership just changed. */
public class TeamMembersListener {

    @SubscribeEvent
    public void onJoin(TeamJoinEvent event) {
        syncTeam(event.team);
        // A solo team merged into the new one is already gone from the manager, and its player got the sync above.
        if (event.oldTeam != null && TeamManager.getTeamById(event.oldTeam.getTeamId()) != null) {
            syncTeam(event.oldTeam);
        }
    }

    @SubscribeEvent
    public void onLeave(TeamLeaveEvent event) {
        if (!event.teamDisbanded) syncTeam(event.team);
        syncTeam(event.newTeam);
    }

    @SubscribeEvent
    public void onKick(TeamKickEvent event) {
        syncTeam(event.team);
        syncTeam(event.newTeam);
    }

    @SubscribeEvent
    public void onDisband(TeamDisbandEvent event) {
        for (Team newTeam : event.newTeamsByMember.values()) syncTeam(newTeam);
    }

    private static void syncTeam(Team team) {
        // Members go first: the task packet rebuilds an open GUI, which reads the member list.
        TaskNHNetwork.sendToTeamMembers(team.getMembers(), PlayerLoginHandler.buildTeamMembersPacket(team));
        TaskNHNetwork.sendToTeamMembers(
            team.getMembers(),
            new SyncAllTasksPacket(TaskNHWorldData.get().getTeamTasks(team.getTeamId())));
    }
}
