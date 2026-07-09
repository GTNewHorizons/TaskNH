package com.eldrinn.tasknh.event;

import com.eldrinn.tasknh.network.SyncAllTasksPacket;
import com.eldrinn.tasknh.network.TaskNHNetwork;
import com.eldrinn.tasknh.storage.TaskNHWorldData;
import com.gtnewhorizon.gtnhlib.teams.TeamEvents.TeamMergeEvent;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public class TeamMergeListener {

    @SubscribeEvent
    public void onTeamMerge(TeamMergeEvent event) {
        TaskNHWorldData data = TaskNHWorldData.get();
        data.mergeTasks(event.consumed.getTeamId(), event.surviving.getTeamId());
        TaskNHNetwork.sendToTeamMembers(
            event.surviving.getMembers(),
            new SyncAllTasksPacket(data.getTeamTasks(event.surviving.getTeamId())));
    }
}
