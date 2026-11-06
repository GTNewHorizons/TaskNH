package com.eldrinn.tasknh.event;

import com.eldrinn.tasknh.network.TaskNHNetwork;
import com.gtnewhorizon.gtnhlib.teams.Team;
import com.gtnewhorizon.gtnhlib.teams.TeamEvents.TeamDisbandEvent;
import com.gtnewhorizon.gtnhlib.teams.TeamEvents.TeamJoinEvent;
import com.gtnewhorizon.gtnhlib.teams.TeamEvents.TeamKickEvent;
import com.gtnewhorizon.gtnhlib.teams.TeamEvents.TeamLeaveEvent;
import com.gtnewhorizon.gtnhlib.teams.TeamManager;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/** Sends the new member list to the online members of every team whose membership just changed. */
public class TeamMembersListener {

    @SubscribeEvent
    public void onJoin(TeamJoinEvent event) {
        sendMembers(event.team);
        // A solo team merged into the new one is already gone from the manager, and its player got the list above.
        if (event.oldTeam != null && TeamManager.getTeamById(event.oldTeam.getTeamId()) != null) {
            sendMembers(event.oldTeam);
        }
    }

    @SubscribeEvent
    public void onLeave(TeamLeaveEvent event) {
        if (!event.teamDisbanded) sendMembers(event.team);
        sendMembers(event.newTeam);
    }

    @SubscribeEvent
    public void onKick(TeamKickEvent event) {
        sendMembers(event.team);
        sendMembers(event.newTeam);
    }

    @SubscribeEvent
    public void onDisband(TeamDisbandEvent event) {
        for (Team newTeam : event.newTeamsByMember.values()) sendMembers(newTeam);
    }

    private static void sendMembers(Team team) {
        TaskNHNetwork.sendToTeamMembers(team.getMembers(), PlayerLoginHandler.buildTeamMembersPacket(team));
    }
}
