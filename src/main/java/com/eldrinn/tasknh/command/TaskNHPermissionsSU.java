package com.eldrinn.tasknh.command;

import net.minecraft.entity.player.EntityPlayerMP;

import serverutils.lib.util.permission.DefaultPermissionLevel;
import serverutils.lib.util.permission.PermissionAPI;

/**
 * ServerUtilities-only bridge. Never loaded unless serverutilities is present, so its serverutils.* imports are safe
 * behind a Loader.isModLoaded guard.
 */
final class TaskNHPermissionsSU {

    private TaskNHPermissionsSU() {}

    static void registerNode(String node, boolean opByDefault, String desc) {
        PermissionAPI.registerNode(node, opByDefault ? DefaultPermissionLevel.OP : DefaultPermissionLevel.ALL, desc);
    }

    static boolean hasPermission(EntityPlayerMP player, String node) {
        return PermissionAPI.hasPermission(player, node);
    }
}
