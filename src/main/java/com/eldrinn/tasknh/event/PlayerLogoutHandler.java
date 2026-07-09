package com.eldrinn.tasknh.event;

import net.minecraft.entity.player.EntityPlayerMP;

import com.eldrinn.tasknh.storage.TaskNHWorldData;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;

public class PlayerLogoutHandler {

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.player instanceof EntityPlayerMP player)) return;
        TaskNHWorldData.get()
            .setPlayerLastSeen(player.getUniqueID(), System.currentTimeMillis());
    }
}
